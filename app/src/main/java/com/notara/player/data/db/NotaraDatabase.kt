package com.notara.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackMarkerEntity::class,
        TrackTranscriptEntity::class,
        TrackWaveformEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NotaraDatabase : RoomDatabase() {
    abstract fun notaraDao(): NotaraDao

    companion object {
        @Volatile
        private var INSTANCE: NotaraDatabase? = null

        fun getInstance(context: Context): NotaraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotaraDatabase::class.java,
                    "notara_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
