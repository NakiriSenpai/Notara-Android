package com.notara.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notara.player.data.TrackSortMode
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.MiniEqualizerBars
import com.notara.player.ui.formatTimeMs
import com.notara.player.ui.i18n.LocalNotaraStrings

@Composable
fun SearchLibraryScreen(
    filteredTracks: List<LocalTrack>,
    folders: List<String>,
    selectedFolder: String,
    favoritesOnly: Boolean,
    favoriteHashes: Set<String>,
    sortMode: TrackSortMode,
    searchQuery: String,
    isScanning: Boolean,
    customCovers: Map<String, String>,
    playlists: List<PlaylistEntity>,
    currentTrack: LocalTrack?,
    isPlaying: Boolean,
    onSearchChange: (String) -> Unit,
    onFolderSelect: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSortModeSelect: (TrackSortMode) -> Unit,
    onToggleFavoriteTrack: (String) -> Unit,
    onTrackClick: (LocalTrack, List<LocalTrack>) -> Unit,
    onPickTrackCover: (LocalTrack) -> Unit,
    onResetTrackCover: (LocalTrack) -> Unit,
    onAddTrackToPlaylist: (PlaylistEntity, LocalTrack) -> Unit
) {
    val strings = LocalNotaraStrings.current
    var showSortMenu by remember { mutableStateOf(false) }
    var trackForPlaylistPicker by remember { mutableStateOf<LocalTrack?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp)
    ) {
        // Search Bar + Sort Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            )
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                ) {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text("Terbaru") }, onClick = { showSortMenu = false; onSortModeSelect(TrackSortMode.DATE_ADDED) })
                    DropdownMenuItem(text = { Text("Judul A - Z") }, onClick = { showSortMenu = false; onSortModeSelect(TrackSortMode.TITLE_AZ) })
                    DropdownMenuItem(text = { Text("Durasi Terpanjang") }, onClick = { showSortMenu = false; onSortModeSelect(TrackSortMode.DURATION_DESC) })
                    DropdownMenuItem(text = { Text("Favorit Dulu") }, onClick = { showSortMenu = false; onSortModeSelect(TrackSortMode.FAVORITES_FIRST) })
                }
            }
        }

        // Filter Chips: Favorit ♥ + Folders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = favoritesOnly,
                onClick = onToggleFavoritesOnly,
                label = { Text("Favorit") },
                leadingIcon = {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp)
            )
            folders.forEach { folder ->
                val label = if (folder == "ALL_FOLDER") strings.allFolder else folder
                FilterChip(
                    selected = folder == selectedFolder,
                    onClick = { onFolderSelect(folder) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        if (isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noAudioFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = filteredTracks,
                    key = { it.mediaStoreId },
                    contentType = { "track_row" }
                ) { track ->
                    val isCurrent = track.trackHash == currentTrack?.trackHash
                    val isFav = track.trackHash in favoriteHashes
                    TrackListRow(
                        track = track,
                        customCoverPath = customCovers[track.trackHash],
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        isFavorite = isFav,
                        onToggleFavorite = { onToggleFavoriteTrack(track.trackHash) },
                        onClick = { onTrackClick(track, filteredTracks) },
                        onAddToPlaylist = { trackForPlaylistPicker = track },
                        onPickCover = { onPickTrackCover(track) },
                        onResetCover = { onResetTrackCover(track) }
                    )
                }
            }
        }
    }

    val targetTrack = trackForPlaylistPicker
    if (targetTrack != null) {
        AlertDialog(
            onDismissRequest = { trackForPlaylistPicker = null },
            title = { Text(strings.addToPlaylist) },
            text = {
                if (playlists.isEmpty()) {
                    Text(strings.noPlaylistYet)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        playlists.forEach { pl ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddTrackToPlaylist(pl, targetTrack)
                                        trackForPlaylistPicker = null
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CozyArtwork(
                                        customCoverPath = pl.customCoverPath,
                                        albumArtUri = null,
                                        seedKey = "pl_${pl.id}_${pl.name}",
                                        sizeDp = 36,
                                        cornerDp = 8,
                                        isPlaylist = true
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(pl.name, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackForPlaylistPicker = null }) { Text(strings.closeBtn) }
            }
        )
    }
}

@Composable
fun PlaylistsScreen(
    allTracks: List<LocalTrack>,
    playlists: List<PlaylistEntity>,
    playlistTrackMap: Map<Long, List<String>>,
    openedPlaylistId: Long?,
    customCovers: Map<String, String>,
    currentTrack: LocalTrack?,
    isPlaying: Boolean,
    onOpenPlaylist: (Long?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistWithPhoto: (String) -> Unit,
    onRenamePlaylist: (PlaylistEntity, String) -> Unit,
    onPickPlaylistCover: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onTrackClick: (LocalTrack, List<LocalTrack>) -> Unit,
    onRemoveTrackFromPlaylist: (Long, String) -> Unit
) {
    val strings = LocalNotaraStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    val openedPlaylist = playlists.firstOrNull { it.id == openedPlaylistId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp)
    ) {
        if (openedPlaylist != null) {
            val trackHashes = playlistTrackMap[openedPlaylist.id].orEmpty().toSet()
            val plTracks = remember(allTracks, trackHashes) {
                allTracks.filter { it.trackHash in trackHashes }.distinctBy { it.trackHash }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onOpenPlaylist(null) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                Column {
                    Text(openedPlaylist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(strings.songsCount(plTracks.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CozyArtwork(
                            customCoverPath = openedPlaylist.customCoverPath,
                            albumArtUri = null,
                            seedKey = "pl_${openedPlaylist.id}_${openedPlaylist.name}",
                            sizeDp = 86,
                            cornerDp = 20,
                            isPlaylist = true,
                            modifier = Modifier.clickable { onPickPlaylistCover(openedPlaylist) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { plTracks.firstOrNull()?.let { onTrackClick(it, plTracks) } },
                                enabled = plTracks.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.playPlaylist)
                            }
                            OutlinedButton(
                                onClick = { onPickPlaylistCover(openedPlaylist) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.changePlaylistPhoto)
                            }
                        }
                    }
                }

                if (plTracks.isEmpty()) {
                    item {
                        Text(strings.emptyPlaylistDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                    }
                } else {
                    items(plTracks, key = { it.trackHash }) { track ->
                        val isCurrent = track.trackHash == currentTrack?.trackHash
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable { onTrackClick(track, plTracks) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CozyArtwork(
                                customCoverPath = customCovers[track.trackHash],
                                albumArtUri = track.albumArtUri,
                                seedKey = track.trackHash,
                                sizeDp = 46,
                                cornerDp = 10
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (isCurrent) {
                                MiniEqualizerBars(isPlaying = isPlaying, modifier = Modifier.padding(end = 6.dp))
                            }
                            IconButton(onClick = { onRemoveTrackFromPlaylist(openedPlaylist.id, track.trackHash) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Text(
                    text = strings.playlists,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.createPlaylist)
                }
                Spacer(Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.emptyPlaylists, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 110.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            val count = playlistTrackMap[pl.id]?.size ?: 0
                            var menuExpanded by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPlaylist(pl.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CozyArtwork(
                                        customCoverPath = pl.customCoverPath,
                                        albumArtUri = null,
                                        seedKey = "pl_${pl.id}_${pl.name}",
                                        sizeDp = 56,
                                        cornerDp = 14,
                                        isPlaylist = true
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(strings.songsCount(count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(Icons.Rounded.MoreVert, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text(strings.changePlaylistPhoto) },
                                                leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                                onClick = { menuExpanded = false; onPickPlaylistCover(pl) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(strings.renamePlaylist) },
                                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                                onClick = { menuExpanded = false; renamingPlaylist = pl }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(strings.deletePlaylist) },
                                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                                onClick = { menuExpanded = false; onDeletePlaylist(pl) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.createPlaylistTitle) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.playlistNameLabel) },
                    placeholder = { Text(strings.playlistNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val finalName = name.trim().ifBlank { "Playlist" }
                        showCreateDialog = false
                        onCreatePlaylistWithPhoto(finalName)
                    }) { Text(strings.addPhotoBtn) }
                    Button(onClick = {
                        val finalName = name.trim().ifBlank { "Playlist" }
                        showCreateDialog = false
                        onCreatePlaylist(finalName)
                    }) { Text(strings.createBtn) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancelBtn) }
            }
        )
    }

    val toRename = renamingPlaylist
    if (toRename != null) {
        var newName by remember(toRename) { mutableStateOf(toRename.name) }
        AlertDialog(
            onDismissRequest = { renamingPlaylist = null },
            title = { Text(strings.renamePlaylist) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = { onRenamePlaylist(toRename, newName); renamingPlaylist = null }) { Text(strings.saveBtn) }
            },
            dismissButton = {
                TextButton(onClick = { renamingPlaylist = null }) { Text(strings.cancelBtn) }
            }
        )
    }
}

@Composable
private fun TrackListRow(
    track: LocalTrack,
    customCoverPath: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPickCover: () -> Unit,
    onResetCover: () -> Unit
) {
    val strings = LocalNotaraStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    val rowModifier = if (isCurrent) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    }

    Row(
        modifier = rowModifier.padding(start = 10.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CozyArtwork(
            customCoverPath = customCoverPath,
            albumArtUri = track.albumArtUri,
            seedKey = track.trackHash,
            sizeDp = 48,
            cornerDp = 12
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} · ${formatTimeMs(track.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            MiniEqualizerBars(isPlaying = isPlaying, modifier = Modifier.padding(horizontal = 4.dp))
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(strings.addToPlaylist) },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                    onClick = { menuExpanded = false; onAddToPlaylist() }
                )
                DropdownMenuItem(
                    text = { Text(strings.changeTrackPhoto) },
                    leadingIcon = { Icon(Icons.Rounded.Image, null) },
                    onClick = { menuExpanded = false; onPickCover() }
                )
                if (customCoverPath != null) {
                    DropdownMenuItem(
                        text = { Text(strings.resetTrackPhoto) },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                        onClick = { menuExpanded = false; onResetCover() }
                    )
                }
            }
        }
    }
}

@Composable
fun CozySegmentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
