package com.notara.player.data

import com.notara.player.data.db.TrackTranscriptEntity

object TranscriptParser {

    private val lrcLineRegex = Regex("^\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]\\s*(.*)$")
    private val srtTimeRegex = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

    fun parse(rawText: String, trackHash: String): List<TrackTranscriptEntity> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Deteksi format SRT
        if (srtTimeRegex.containsMatchIn(trimmed)) {
            return parseSrt(trimmed, trackHash)
        }

        val lines = trimmed.lines()
        val hasLrc = lines.any { lrcLineRegex.matches(it.trim()) }
        if (hasLrc) {
            return parseLrc(lines, trackHash)
        }

        // Plain text manual (unsynced)
        return lines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { idx, line ->
                TrackTranscriptEntity(
                    trackHash = trackHash,
                    lineIndex = idx,
                    startMs = null,
                    text = line
                )
            }
    }

    private fun parseLrc(lines: List<String>, trackHash: String): List<TrackTranscriptEntity> {
        val result = mutableListOf<Pair<Long, String>>()
        for (raw in lines) {
            val match = lrcLineRegex.matchEntire(raw.trim()) ?: continue
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val fracStr = match.groupValues[3]
            val millis = when (fracStr.length) {
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                3 -> fracStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                val totalMs = min * 60_000L + sec * 1_000L + millis
                result.add(totalMs to text)
            }
        }
        return result
            .sortedBy { it.first }
            .mapIndexed { idx, (ms, text) ->
                TrackTranscriptEntity(
                    trackHash = trackHash,
                    lineIndex = idx,
                    startMs = ms,
                    text = text
                )
            }
    }

    private fun parseSrt(content: String, trackHash: String): List<TrackTranscriptEntity> {
        val blocks = content.split(Regex("\\r?\\n\\s*\\r?\\n"))
        val parsed = mutableListOf<Pair<Long, String>>()
        for (block in blocks) {
            val blockLines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val timeIdx = blockLines.indexOfFirst { srtTimeRegex.containsMatchIn(it) }
            if (timeIdx < 0) continue
            val m = srtTimeRegex.find(blockLines[timeIdx]) ?: continue
            val h = m.groupValues[1].toLong()
            val min = m.groupValues[2].toLong()
            val sec = m.groupValues[3].toLong()
            val ms = m.groupValues[4].toLong()
            val startMs = h * 3_600_000L + min * 60_000L + sec * 1_000L + ms
            val text = blockLines.drop(timeIdx + 1).joinToString(" ").trim()
            if (text.isNotEmpty()) {
                parsed.add(startMs to text)
            }
        }
        return parsed
            .sortedBy { it.first }
            .mapIndexed { idx, (ms, text) ->
                TrackTranscriptEntity(
                    trackHash = trackHash,
                    lineIndex = idx,
                    startMs = ms,
                    text = text
                )
            }
    }

    fun exportToLrc(lines: List<TrackTranscriptEntity>): String {
        return lines.joinToString("\n") { line ->
            val ms = line.startMs
            if (ms != null) {
                val minutes = ms / 60_000L
                val seconds = (ms % 60_000L) / 1_000L
                val centis = (ms % 1_000L) / 10L
                "[%02d:%02d.%02d]%s".format(minutes, seconds, centis, line.text)
            } else {
                line.text
            }
        }
    }
}
