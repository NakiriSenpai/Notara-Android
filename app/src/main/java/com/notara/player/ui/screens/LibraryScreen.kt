package com.notara.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.MiniEqualizerBars
import com.notara.player.ui.formatTimeMs
import com.notara.player.ui.i18n.LocalNotaraStrings
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    allTracks: List<LocalTrack>,
    filteredTracks: List<LocalTrack>,
    folders: List<String>,
    selectedFolder: String,
    searchQuery: String,
    isScanning: Boolean,
    customCovers: Map<String, String>,
    playlists: List<PlaylistEntity>,
    playlistTrackMap: Map<Long, List<String>>,
    currentTrack: LocalTrack?,
    isPlaying: Boolean,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    onSearchChange: (String) -> Unit,
    onFolderSelect: (String) -> Unit,
    onRescan: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onTrackClick: (LocalTrack, List<LocalTrack>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onPickTrackCover: (LocalTrack) -> Unit,
    onResetTrackCover: (LocalTrack) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistWithPhoto: (String) -> Unit,
    onRenamePlaylist: (PlaylistEntity, String) -> Unit,
    onPickPlaylistCover: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddTrackToPlaylist: (PlaylistEntity, LocalTrack) -> Unit,
    onRemoveTrackFromPlaylist: (Long, String) -> Unit
) {
    val strings = LocalNotaraStrings.current
    var activeSection by remember { mutableIntStateOf(0) } // 0 = All Songs, 1 = Playlists
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var trackForPlaylistPicker by remember { mutableStateOf<LocalTrack?>(null) }

    val openedPlaylist = playlists.firstOrNull { it.id == openedPlaylistId }

    // Sistem tombol Back HP yang natural (tidak langsung keluar aplikasi!)
    BackHandler(enabled = openedPlaylist != null || activeSection != 0 || searchQuery.isNotEmpty()) {
        when {
            openedPlaylist != null -> openedPlaylistId = null
            activeSection != 0 -> activeSection = 0
            searchQuery.isNotEmpty() -> onSearchChange("")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    if (openedPlaylist != null) {
                        IconButton(onClick = { openedPlaylistId = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                    }
                },
                title = {
                    if (openedPlaylist != null) {
                        Column {
                            Text(openedPlaylist.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val count = playlistTrackMap[openedPlaylist.id]?.size ?: 0
                            Text(
                                text = strings.songsCount(count),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "Notara",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = strings.localAudioCount(allTracks.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLanguagePicker) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = strings.chooseLanguageTitle,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenThemePicker) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = strings.chooseThemeTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            if (currentTrack != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(onClick = onOpenPlayer)
                    ) {
                        Column {
                            // Garis progres tipis bercahaya di tepi atas mini-player (gaya Tokyo Midnight)
                            MiniPlayerTopProgress(positionMsFlow = positionMsFlow, durationMs = durationMs)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CozyArtwork(
                                    customCoverPath = customCovers[currentTrack.trackHash],
                                    albumArtUri = currentTrack.albumArtUri,
                                    seedKey = currentTrack.trackHash,
                                    sizeDp = 44,
                                    cornerDp = 12
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = onTogglePlayPause) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onSkipNext) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (openedPlaylist != null) {
                val trackHashes = playlistTrackMap[openedPlaylist.id].orEmpty().toSet()
                // Deduplikasi ketat per trackHash agar tidak pernah muncul ganda!
                val plTracks = remember(allTracks, trackHashes) {
                    allTracks.filter { it.trackHash in trackHashes }.distinctBy { it.trackHash }
                }
                PlaylistDetailContent(
                    playlist = openedPlaylist,
                    tracks = plTracks,
                    customCovers = customCovers,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onPlayAll = { plTracks.firstOrNull()?.let { onTrackClick(it, plTracks) } },
                    onChangePhoto = { onPickPlaylistCover(openedPlaylist) },
                    onTrackClick = { onTrackClick(it, plTracks) },
                    onRemoveTrack = { onRemoveTrackFromPlaylist(openedPlaylist.id, it.trackHash) }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CozySegmentPill(
                        label = strings.allSongs,
                        selected = activeSection == 0,
                        onClick = { activeSection = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    CozySegmentPill(
                        label = "${strings.playlists} (${playlists.size})",
                        selected = activeSection == 1,
                        onClick = { activeSection = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedContent(
                    targetState = activeSection,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
                    label = "library_tab"
                ) { section ->
                    if (section == 0) {
                        Column(modifier = Modifier.fillMaxSize()) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(
                                        items = filteredTracks,
                                        key = { it.mediaStoreId },
                                        contentType = { "track_row" }
                                    ) { track ->
                                        TrackListRow(
                                            track = track,
                                            customCoverPath = customCovers[track.trackHash],
                                            isCurrent = track.trackHash == currentTrack?.trackHash,
                                            isPlaying = isPlaying,
                                            onClick = { onTrackClick(track, filteredTracks) },
                                            onAddToPlaylist = { trackForPlaylistPicker = track },
                                            onPickCover = { onPickTrackCover(track) },
                                            onResetCover = { onResetTrackCover(track) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { showCreatePlaylistDialog = true },
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
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(playlists, key = { it.id }) { pl ->
                                        val count = playlistTrackMap[pl.id]?.size ?: 0
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { openedPlaylistId = pl.id }
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
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
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
                        showCreatePlaylistDialog = false
                        onCreatePlaylistWithPhoto(finalName)
                    }) { Text(strings.addPhotoBtn) }
                    Button(onClick = {
                        val finalName = name.trim().ifBlank { "Playlist" }
                        showCreatePlaylistDialog = false
                        onCreatePlaylist(finalName)
                    }) { Text(strings.createBtn) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text(strings.cancelBtn) }
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
private fun MiniPlayerTopProgress(positionMsFlow: StateFlow<Long>, durationMs: Long) {
    val pos by positionMsFlow.collectAsStateWithLifecycle()
    val prog = (pos.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { prog },
        modifier = Modifier.fillMaxWidth().height(2.5.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.Transparent
    )
}

@Composable
private fun TrackListRow(
    track: LocalTrack,
    customCoverPath: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPickCover: () -> Unit,
    onResetCover: () -> Unit
) {
    val strings = LocalNotaraStrings.current
    var menuExpanded by remember { mutableStateOf(false) }

    // Desain bersih ala Tokyo Midnight mockup: hanya lagu aktif yang terangkat jadi kartu hangat
    val rowBg = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f) else Color.Transparent
    val rowModifier = if (isCurrent) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    }

    Row(
        modifier = rowModifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CozyArtwork(
            customCoverPath = customCoverPath,
            albumArtUri = track.albumArtUri,
            seedKey = track.trackHash,
            sizeDp = 50,
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${track.artist} · ${track.folderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            MiniEqualizerBars(isPlaying = isPlaying, modifier = Modifier.padding(horizontal = 6.dp))
        } else {
            Text(
                text = formatTimeMs(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
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
private fun PlaylistDetailContent(
    playlist: PlaylistEntity,
    tracks: List<LocalTrack>,
    customCovers: Map<String, String>,
    currentTrack: LocalTrack?,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onChangePhoto: () -> Unit,
    onTrackClick: (LocalTrack) -> Unit,
    onRemoveTrack: (LocalTrack) -> Unit
) {
    val strings = LocalNotaraStrings.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CozyArtwork(
                    customCoverPath = playlist.customCoverPath,
                    albumArtUri = null,
                    seedKey = "pl_${playlist.id}_${playlist.name}",
                    sizeDp = 88,
                    cornerDp = 20,
                    isPlaylist = true,
                    modifier = Modifier.clickable(onClick = onChangePhoto)
                )
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onPlayAll,
                        enabled = tracks.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.playPlaylist)
                    }
                    OutlinedButton(
                        onClick = onChangePhoto,
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

        if (tracks.isEmpty()) {
            item {
                Text(
                    text = strings.emptyPlaylistDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else {
            items(tracks, key = { it.trackHash }) { track ->
                val isCurrent = track.trackHash == currentTrack?.trackHash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .clickable { onTrackClick(track) }
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
                    IconButton(onClick = { onRemoveTrack(track) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
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
