package com.notara.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.ReverbOption

class AudioDspController {

    private var boundSessionId: Int = 0
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null

    @OptIn(UnstableApi::class)
    fun apply(player: ExoPlayer, state: AudioEffectsState) {
        // 1. Speed, Pitch & Skip Silence langsung lewat Media3 ExoPlayer
        player.playbackParameters = PlaybackParameters(
            state.speed.coerceIn(0.5f, 2.0f),
            state.pitch.coerceIn(0.5f, 2.0f)
        )
        player.skipSilenceEnabled = state.skipSilence

        // 2. Volume dasar (0..100%)
        val clampedVol = state.volumePercent.coerceIn(0, 200)
        player.volume = if (clampedVol <= 100) clampedVol / 100f else 1.0f

        val sessionId = player.audioSessionId
        if (sessionId <= 0) return

        if (sessionId != boundSessionId) {
            releaseEffects()
            initEffects(sessionId)
            boundSessionId = sessionId
        }

        // 3. Volume Boost 101..200% via LoudnessEnhancer (0..1000 mB = 0..+10 dB)
        runCatching {
            val enhancer = loudnessEnhancer
            if (enhancer != null) {
                if (clampedVol > 100) {
                    val gainMb = (clampedVol - 100) * 10 // 200% -> +1000 mB
                    enhancer.setTargetGain(gainMb)
                    enhancer.enabled = true
                } else {
                    enhancer.setTargetGain(0)
                    enhancer.enabled = false
                }
            }
        }

        // 4. Proteksi clipping dengan DynamicsProcessing.Limiter (API 28+)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.enabled = state.limiterEnabled && clampedVol > 100
            }
        }

        // 5. Equalizer 5-band
        runCatching {
            val eq = equalizer
            if (eq != null) {
                eq.enabled = state.eqEnabled
                if (state.eqEnabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val minMb = eq.bandLevelRange[0].toInt()
                    val maxMb = eq.bandLevelRange[1].toInt()
                    for (i in 0 until minOf(numBands, state.eqBandLevelsDb.size)) {
                        val targetMb = (state.eqBandLevelsDb[i] * 100).coerceIn(minMb, maxMb)
                        eq.setBandLevel(i.toShort(), targetMb.toShort())
                    }
                }
            }
        }

        // 6. Bass Boost
        runCatching {
            val bb = bassBoost
            if (bb != null) {
                val strength = state.bassBoostStrength.coerceIn(0, 1000).toShort()
                bb.setStrength(strength)
                bb.enabled = strength > 0
            }
        }

        // 7. Preset Reverb
        runCatching {
            val rev = presetReverb
            if (rev != null) {
                if (state.reverb == ReverbOption.NONE) {
                    rev.enabled = false
                } else {
                    rev.preset = state.reverb.presetValue
                    rev.enabled = true
                }
            }
        }
    }

    private fun initEffects(sessionId: Int) {
        loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        presetReverb = runCatching { PresetReverb(0, sessionId) }.getOrNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing = runCatching {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    false, 0,
                    false, 0,
                    false, 0,
                    true // Aktifkan tahap Limiter
                ).build()
                DynamicsProcessing(0, sessionId, config).apply {
                    val limiter = DynamicsProcessing.Limiter(
                        true,  // inUse
                        true,  // enabled
                        0,     // linkGroup
                        1.0f,  // attackTime (ms)
                        60.0f, // releaseTime (ms)
                        10.0f, // ratio
                        -1.0f, // threshold (dB) -> cegah clipping keras
                        0.0f   // postGain (dB)
                    )
                    setLimiterAllChannelsTo(limiter)
                    enabled = false
                }
            }.getOrNull()
        }
    }

    fun releaseEffects() {
        runCatching { loudnessEnhancer?.release() }
        runCatching { dynamicsProcessing?.release() }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { presetReverb?.release() }
        loudnessEnhancer = null
        dynamicsProcessing = null
        equalizer = null
        bassBoost = null
        presetReverb = null
        boundSessionId = 0
    }
}
