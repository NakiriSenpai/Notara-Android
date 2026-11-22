package com.notara.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.NotaraBrandIcon
import com.notara.player.ui.components.WavyLiveProgressBar
import com.notara.player.ui.i18n.LocalNotaraStrings
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    heroTrack: LocalTrack?,
    isPlaying: Boolean,
    isHeroCurrentlyPlaying: Boolean,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    recentlyPlayed: List<LocalTrack>,
    fallbackTracks: List<LocalTrack>,
    playlists: List<PlaylistEntity>,
    playlistTrackMap: Map<Long, List<String>>,
    customCovers: Map<String, String>,
    onOpenSettings: () -> Unit,
    onHeroCardClick: (LocalTrack) -> Unit,
    onHeroPlayPause: (LocalTrack) -> Unit,
    onTrackClick: (LocalTrack, List<LocalTrack>) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onCreatePlaylistClick: () -> Unit
) {
    val strings = LocalNotaraStrings.current
    val recentList = if (recentlyPlayed.isNotEmpty()) recentlyPlayed else fallbackTracks.take(10)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Top Header: Icon 01 + Notara (kiri) | Tombol Settings (kanan)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NotaraBrandIcon(size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Notara",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                ) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Cozy Animated Now / Last Played Hero Card (dengan WavyLiveProgressBar!)
        if (heroTrack != null) {
            item {
                val cardGradient = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .background(cardGradient)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            RoundedCornerShape(26.dp)
                        )
                        .clickable { onHeroCardClick(heroTrack) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), CircleShape)
                                .padding(3.dp)
                        ) {
                            CozyArtwork(
                                customCoverPath = customCovers[heroTrack.trackHash],
                                albumArtUri = heroTrack.albumArtUri,
                                seedKey = heroTrack.trackHash,
                                sizeDp = 76,
                                cornerDp = 38
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = heroTrack.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = heroTrack.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WavyLiveProgressBar(
                                    positionMsFlow = positionMsFlow,
                                    durationMs = if (isHeroCurrentlyPlaying) durationMs else heroTrack.durationMs,
                                    isPlaying = isHeroCurrentlyPlaying && isPlaying,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable { onHeroPlayPause(heroTrack) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isHeroCurrentlyPlaying && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Recently Played Horizontal Carousel
        if (recentList.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentList, key = { it.trackHash }) { track ->
                            Column(
                                modifier = Modifier
                                    .width(106.dp)
                                    .clickable { onTrackClick(track, recentList) }
                            ) {
                                CozyArtwork(
                                    customCoverPath = customCovers[track.trackHash],
                                    albumArtUri = track.albumArtUri,
                                    seedKey = track.trackHash,
                                    sizeDp = 106,
                                    cornerDp = 18
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Playlists Horizontal Photo Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.playlists,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists, key = { it.id }) { pl ->
                        val count = playlistTrackMap[pl.id]?.size ?: 0
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .width(126.dp)
                                .clickable { onPlaylistClick(pl) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                CozyArtwork(
                                    customCoverPath = pl.customCoverPath,
                                    albumArtUri = null,
                                    seedKey = "pl_${pl.id}_${pl.name}",
                                    sizeDp = 106,
                                    cornerDp = 14,
                                    isPlaylist = true
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.songsCount(count),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .width(114.dp)
                                .height(166.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable(onClick = onCreatePlaylistClick)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(strings.createBtn, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
