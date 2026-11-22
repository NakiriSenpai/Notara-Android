package com.notara.player.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaraDao {

    // --- Cached Tracks ---
    @Query("SELECT * FROM cached_tracks ORDER BY sortOrder ASC")
    suspend fun getCachedTracks(): List<CachedTrackEntity>

    @Query("DELETE FROM cached_tracks")
    suspend fun clearCachedTracks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedTracks(tracks: List<CachedTrackEntity>)

    @Transaction
    suspend fun replaceCachedTracks(tracks: List<CachedTrackEntity>) {
        clearCachedTracks()
        if (tracks.isNotEmpty()) insertCachedTracks(tracks)
    }

    @Query("UPDATE cached_tracks SET title = :title, artist = :artist WHERE mediaStoreId = :mediaStoreId")
    suspend fun updateCachedTrackMetadata(mediaStoreId: Long, title: String, artist: String)

    // --- Markers ---
    @Query("SELECT * FROM track_markers WHERE trackHash = :trackHash ORDER BY timestampMs ASC")
    fun observeMarkers(trackHash: String): Flow<List<TrackMarkerEntity>>

    @Query("SELECT * FROM track_markers WHERE trackHash = :trackHash ORDER BY timestampMs ASC")
    suspend fun getMarkersOnce(trackHash: String): List<TrackMarkerEntity>

    @Query("SELECT * FROM track_markers")
    suspend fun getAllMarkersOnce(): List<TrackMarkerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: TrackMarkerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkers(markers: List<TrackMarkerEntity>)

    @Update
    suspend fun updateMarker(marker: TrackMarkerEntity)

    @Delete
    suspend fun deleteMarker(marker: TrackMarkerEntity)

    // --- Transcripts ---
    @Query("SELECT * FROM track_transcripts WHERE trackHash = :trackHash ORDER BY lineIndex ASC")
    fun observeTranscript(trackHash: String): Flow<List<TrackTranscriptEntity>>

    @Query("SELECT * FROM track_transcripts WHERE trackHash = :trackHash ORDER BY lineIndex ASC")
    suspend fun getTranscriptOnce(trackHash: String): List<TrackTranscriptEntity>

    @Query("SELECT * FROM track_transcripts")
    suspend fun getAllTranscriptsOnce(): List<TrackTranscriptEntity>

    @Query("DELETE FROM track_transcripts WHERE trackHash = :trackHash")
    suspend fun clearTranscript(trackHash: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptLines(lines: List<TrackTranscriptEntity>)

    @Query("UPDATE track_transcripts SET startMs = :startMs WHERE id = :id")
    suspend fun updateTranscriptTimestamp(id: Long, startMs: Long)

    @Transaction
    suspend fun replaceTranscript(trackHash: String, lines: List<TrackTranscriptEntity>) {
        clearTranscript(trackHash)
        if (lines.isNotEmpty()) insertTranscriptLines(lines)
    }

    // --- Waveforms ---
    @Query("SELECT * FROM track_waveforms WHERE trackHash = :trackHash LIMIT 1")
    suspend fun getWaveform(trackHash: String): TrackWaveformEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWaveform(entity: TrackWaveformEntity)

    // --- Playlists ---
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId AND trackHash = :trackHash")
    suspend fun countTrackInPlaylist(playlistId: Long, trackHash: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackHash = :trackHash")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackHash: String)

    @Query("SELECT * FROM playlist_tracks ORDER BY addedAt ASC")
    fun observeAllPlaylistRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Query("SELECT * FROM playlist_tracks")
    suspend fun getAllPlaylistRefsOnce(): List<PlaylistTrackCrossRef>

    // --- Custom Track Covers ---
    @Query("SELECT * FROM track_custom_covers")
    fun observeAllCustomCovers(): Flow<List<TrackCustomCoverEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrackCustomCover(entity: TrackCustomCoverEntity)

    @Query("DELETE FROM track_custom_covers WHERE trackHash = :trackHash")
    suspend fun deleteTrackCustomCover(trackHash: String)

    // --- Favorites ---
    @Query("SELECT trackHash FROM favorite_tracks ORDER BY addedAt DESC")
    fun observeFavoriteHashes(): Flow<List<String>>

    @Query("SELECT * FROM favorite_tracks")
    suspend fun getAllFavoritesOnce(): List<FavoriteTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE trackHash = :trackHash")
    suspend fun removeFavorite(trackHash: String)

    // --- Play History (Recently Played) ---
    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC LIMIT 30")
    fun observeRecentlyPlayed(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE trackHash = :trackHash LIMIT 1")
    suspend fun getHistoryForTrack(trackHash: String): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayHistory(entity: PlayHistoryEntity)
}
