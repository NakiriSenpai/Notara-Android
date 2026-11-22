package com.notara.player

import android.app.Application
import com.notara.player.data.LocalAudioScanner
import com.notara.player.data.NotaraRepository
import com.notara.player.data.db.NotaraDatabase
import com.notara.player.playback.WaveformExtractor

class NotaraApp : Application() {
    val database: NotaraDatabase by lazy { NotaraDatabase.getInstance(this) }
    val repository: NotaraRepository by lazy {
        NotaraRepository(
            scanner = LocalAudioScanner(this),
            dao = database.notaraDao()
        )
    }
    val waveformExtractor: WaveformExtractor by lazy { WaveformExtractor(this) }
}
