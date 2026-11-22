package com.notara.player.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

sealed interface TagWriteResult {
    data class Success(val wroteFileTag: Boolean) : TagWriteResult
    data class Error(val message: String) : TagWriteResult
}

object Id3TagWriter {

    /**
     * Menyimpan Judul & Artis langsung ke tag ID3v2.3 file MP3 asli (menggunakan UTF-16LE agar
     * mendukung huruf Jepang, Korea, dan Indonesia) serta memperbarui MediaStore Android.
     */
    suspend fun writeTitleAndArtist(
        context: Context,
        contentUri: Uri,
        newTitle: String,
        newArtist: String
    ): TagWriteResult = withContext(Dispatchers.IO) {
        val cleanTitle = newTitle.trim()
        val cleanArtist = newArtist.trim()
        if (cleanTitle.isEmpty()) return@withContext TagWriteResult.Error("Judul tidak boleh kosong")

        var wroteId3 = false
        runCatching {
            val mime = context.contentResolver.getType(contentUri)?.lowercase() ?: ""
            val isMp3 = mime.contains("mpeg") || mime.contains("mp3") || contentUri.toString().lowercase().endsWith(".mp3")

            if (isMp3) {
                val originalBytes = context.contentResolver.openInputStream(contentUri)?.use { it.readBytes() }
                if (originalBytes != null && originalBytes.size > 16) {
                    val patched = patchMp3Id3v2(originalBytes, cleanTitle, cleanArtist)
                    context.contentResolver.openFileDescriptor(contentUri, "rwt")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.write(patched)
                            fos.flush()
                        }
                        wroteId3 = true
                    }
                }
            }

            // Perbarui juga MediaStore agar langsung terbaca sistem & pemutar lain
            val cv = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, cleanTitle)
                put(MediaStore.Audio.Media.ARTIST, cleanArtist.ifBlank { "<unknown>" })
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pendingOn = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 1) }
                runCatching { context.contentResolver.update(contentUri, pendingOn, null, null) }
                context.contentResolver.update(contentUri, cv, null, null)
                val pendingOff = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                runCatching { context.contentResolver.update(contentUri, pendingOff, null, null) }
            } else {
                context.contentResolver.update(contentUri, cv, null, null)
            }
        }.onFailure { err ->
            return@withContext TagWriteResult.Error(err.message ?: "Gagal menulis ke file asli")
        }

        TagWriteResult.Success(wroteFileTag = wroteId3)
    }

    private fun patchMp3Id3v2(fileBytes: ByteArray, title: String, artist: String): ByteArray {
        val keptFrames = mutableListOf<ByteArray>()
        var audioStartOffset = 0

        // Jika sudah ada header ID3v2.3/v2.4, simpan frame lain (seperti APIC sampul album!) kecuali TIT2 & TPE1
        if (fileBytes.size >= 10 &&
            fileBytes[0] == 'I'.code.toByte() &&
            fileBytes[1] == 'D'.code.toByte() &&
            fileBytes[2] == '3'.code.toByte()
        ) {
            val majorVer = fileBytes[3].toInt() and 0xFF
            val tagSize = readSyncSafeInt(fileBytes, 6)
            audioStartOffset = (10 + tagSize).coerceAtMost(fileBytes.size)

            if (majorVer == 3 || majorVer == 4) {
                var pos = 10
                while (pos + 10 <= audioStartOffset) {
                    val frameId = String(fileBytes, pos, 4, StandardCharsets.ISO_8859_1)
                    if (frameId[0] == '\u0000') break
                    val frameSize = if (majorVer == 4) {
                        readSyncSafeInt(fileBytes, pos + 4)
                    } else {
                        readBigEndianInt(fileBytes, pos + 4)
                    }
                    if (frameSize <= 0 || pos + 10 + frameSize > audioStartOffset) break

                    if (frameId != "TIT2" && frameId != "TPE1") {
                        // Salin frame lama (termasuk APIC cover art) dengan header ID3v2.3
                        val out = ByteArray(10 + frameSize)
                        System.arraycopy(fileBytes, pos, out, 0, 4)
                        writeBigEndianInt(out, 4, frameSize)
                        out[8] = fileBytes[pos + 8]
                        out[9] = fileBytes[pos + 9]
                        System.arraycopy(fileBytes, pos + 10, out, 10, frameSize)
                        keptFrames.add(out)
                    }
                    pos += 10 + frameSize
                }
            }
        }

        val body = ByteArrayOutputStream()
        body.write(buildTextFrame("TIT2", title))
        if (artist.isNotBlank()) {
            body.write(buildTextFrame("TPE1", artist))
        }
        keptFrames.forEach { body.write(it) }
        val bodyBytes = body.toByteArray()

        // Header ID3v2.3 (10 bytes) + body + raw audio data
        val result = ByteArrayOutputStream(10 + bodyBytes.size + (fileBytes.size - audioStartOffset))
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 3 // ID3v2.3
        header[4] = 0
        header[5] = 0
        writeSyncSafeInt(header, 6, bodyBytes.size)
        result.write(header)
        result.write(bodyBytes)
        result.write(fileBytes, audioStartOffset, fileBytes.size - audioStartOffset)
        return result.toByteArray()
    }

    private fun buildTextFrame(frameId: String, text: String): ByteArray {
        val textBytes = text.toByteArray(StandardCharsets.UTF_16) // Termasuk BOM untuk ID3v2.3 encoding=1
        val payloadSize = 1 + textBytes.size
        val frame = ByteArray(10 + payloadSize)
        val idBytes = frameId.toByteArray(StandardCharsets.ISO_8859_1)
        System.arraycopy(idBytes, 0, frame, 0, 4)
        writeBigEndianInt(frame, 4, payloadSize)
        frame[8] = 0
        frame[9] = 0
        frame[10] = 1 // Encoding 0x01 = UTF-16 with BOM (mendukung Latin, Jepang, Korea!)
        System.arraycopy(textBytes, 0, frame, 11, textBytes.size)
        return frame
    }

    private fun readSyncSafeInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0x7F) shl 21) or
            ((data[offset + 1].toInt() and 0x7F) shl 14) or
            ((data[offset + 2].toInt() and 0x7F) shl 7) or
            (data[offset + 3].toInt() and 0x7F)
    }

    private fun writeSyncSafeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value ushr 21) and 0x7F).toByte()
        data[offset + 1] = ((value ushr 14) and 0x7F).toByte()
        data[offset + 2] = ((value ushr 7) and 0x7F).toByte()
        data[offset + 3] = (value and 0x7F).toByte()
    }

    private fun readBigEndianInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    private fun writeBigEndianInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value ushr 24) and 0xFF).toByte()
        data[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        data[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 3] = (value and 0xFF).toByte()
    }
}
