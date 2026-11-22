package com.notara.player.playback

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.notara.player.data.model.AudioEffectsState

class NotaraPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val dspController = AudioDspController()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                dspController.apply(player, currentEffectsState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    dspController.apply(player, currentEffectsState)
                }
            }
        })

        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
        activeInstance = this
        dspController.apply(player, currentEffectsState)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        activeInstance = null
        dspController.releaseEffects()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    private fun applyEffectsInternal(state: AudioEffectsState) {
        val player = exoPlayer ?: return
        dspController.apply(player, state)
    }

    companion object {
        @Volatile
        private var activeInstance: NotaraPlaybackService? = null

        @Volatile
        private var currentEffectsState: AudioEffectsState = AudioEffectsState()

        private val mainHandler = Handler(Looper.getMainLooper())

        fun updateAudioEffects(state: AudioEffectsState) {
            currentEffectsState = state
            mainHandler.post {
                activeInstance?.applyEffectsInternal(state)
            }
        }
    }
}
