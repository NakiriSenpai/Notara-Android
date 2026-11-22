package com.notara.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.notara.player.MainActivity
import com.notara.player.R
import com.notara.player.playback.NotaraPlaybackService

class NotaraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, lastTitle, lastArtist, lastIsPlaying)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> NotaraPlaybackService.handleWidgetPlayPause()
            ACTION_NEXT -> NotaraPlaybackService.handleWidgetNext()
            ACTION_PREV -> NotaraPlaybackService.handleWidgetPrev()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.notara.player.WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.notara.player.WIDGET_NEXT"
        const val ACTION_PREV = "com.notara.player.WIDGET_PREV"

        @Volatile private var lastTitle: String = "Notara"
        @Volatile private var lastArtist: String = "Ketuk untuk memutar"
        @Volatile private var lastIsPlaying: Boolean = false

        fun updateAllWidgets(context: Context, title: String, artist: String, isPlaying: Boolean) {
            lastTitle = title
            lastArtist = artist
            lastIsPlaying = isPlaying

            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NotaraWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            fun actionPending(action: String, reqCode: Int): PendingIntent {
                val i = Intent(context, NotaraWidgetProvider::class.java).apply { this.action = action }
                return PendingIntent.getBroadcast(
                    context, reqCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            ids.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_notara).apply {
                    setTextViewText(R.id.widget_title, title)
                    setTextViewText(R.id.widget_artist, artist)
                    setImageViewResource(
                        R.id.widget_btn_play,
                        if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    )
                    setOnClickPendingIntent(R.id.widget_info_container, openPending)
                    setOnClickPendingIntent(R.id.widget_btn_play, actionPending(ACTION_PLAY_PAUSE, 1))
                    setOnClickPendingIntent(R.id.widget_btn_next, actionPending(ACTION_NEXT, 2))
                    setOnClickPendingIntent(R.id.widget_btn_prev, actionPending(ACTION_PREV, 3))
                }
                manager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
