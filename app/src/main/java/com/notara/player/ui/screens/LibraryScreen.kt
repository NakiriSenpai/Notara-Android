package com.notara.player.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
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
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.MiniEqualizerBars
import com.notara.player.ui.formatTimeMs

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
    positionMs: Long,
    durationMs: Long,
    onSearchChange: (String) -> Unit,
    onFolderSelect: (String) -> Unit,
    onRescan: () -> Unit,
    onOpenThemePicker: () -> Unit,
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
    onAddTrackToPlaylist: (Long, LocalTrack) -> Unit,
    onRemoveTrackFromPlaylist: (Long, String) -> Unit
) {
    var activeSection by remember { mutableIntStateOf(0) } // 0 = Semua Lagu, 1 = Playlist
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var trackForPlaylistPicker by remember { mutableStateOf<LocalTrack?>(null) }

    val openedPlaylist = playlists.firstOrNull { it.id == openedPlaylistId }

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
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                title = {
                    if (openedPlaylist != null) {
                        Column {
                            Text(openedPlaylist.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val count = playlistTrackMap[openedPlaylist.id]?.size ?: 0
                            Text(
                                text = "$count lagu di playlist",
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
                                text = "${allTracks.size} audio lokal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenThemePicker) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Pilih Tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Pindai ulang audio")
                    }
                }
            )
        },
        bottomBar = {
            if (currentTrack != null) {
                val prog = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(onClick = onOpenPlayer)
                    ) {
                        Column {
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
                                    sizeDp = 46,
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
                                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onSkipNext) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya")
                                }
                            }
                            LinearProgressIndicator(
                                progress = { prog },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
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
                val plTracks = allTracks.filter { it.trackHash in trackHashes }
                PlaylistDetailContent(
                    playlist = openedPlaylist,
                    tracks = plTracks,
                    customCovers = customCovers,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onPlayAll = {
                        plTracks.firstOrNull()?.let { onTrackClick(it, plTracks) }
                    },
                    onChangePhoto = { onPickPlaylistCover(openedPlaylist) },
                    onTrackClick = { onTrackClick(it, plTracks) },
                    onRemoveTrack = { onRemoveTrackFromPlaylist(openedPlaylist.id, it.trackHash) }
                )
            } else {
                // Segmented Pill Switcher: Semua Lagu | Playlist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CozySegmentPill(
                        label = "Semua Lagu",
                        selected = activeSection == 0,
                        onClick = { activeSection = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    CozySegmentPill(
                        label = "Playlist (${playlists.size})",
                        selected = activeSection == 1,
                        onClick = { activeSection = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedContent(
                    targetState = activeSection,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "library_tab"
                ) { section ->
                    if (section == 0) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Cari lagu, artis, atau folder...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                                    FilterChip(
                                        selected = folder == selectedFolder,
                                        onClick = { onFolderSelect(folder) },
                                        label = { Text(folder) },
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
                                    Text(
                                        text = "Belum ada audio lokal ditemukan.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredTracks, key = { it.mediaStoreId }) { track ->
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
                        // Tab Playlist
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
                                Text("Buat Playlist Baru")
                            }
                            Spacer(Modifier.height(12.dp))

                            if (playlists.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Belum ada playlist. Buat playlist pertamamu dan pasang foto ikon favorit!",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                                    RoundedCornerShape(18.dp)
                                                )
                                                .clickable { openedPlaylistId = pl.id }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CozyArtwork(
                                                    customCoverPath = pl.customCoverPath,
                                                    albumArtUri = null,
                                                    seedKey = "pl_${pl.id}_${pl.name}",
                                                    sizeDp = 58,
                                                    cornerDp = 14,
                                                    isPlaylist = true
                                                )
                                                Spacer(Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = pl.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "$count lagu",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Box {
                                                    IconButton(onClick = { menuExpanded = true }) {
                                                        Icon(Icons.Rounded.MoreVert, contentDescription = "Opsi playlist")
                                                    }
                                                    DropdownMenu(
                                                        expanded = menuExpanded,
                                                        onDismissRequest = { menuExpanded = false }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Ubah Foto Icon") },
                                                            leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                onPickPlaylistCover(pl)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Ganti Nama") },
                                                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                renamingPlaylist = pl
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Hapus Playlist") },
                                                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                onDeletePlaylist(pl)
                                                            }
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

    // Dialog Buat Playlist Baru
    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Buat Playlist Baru") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama playlist") },
                    placeholder = { Text("Misal: Belajar Malam / Lo-Fi Santai") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val finalName = name.trim().ifBlank { "Playlist Baru" }
                        showCreatePlaylistDialog = false
                        onCreatePlaylistWithPhoto(finalName)
                    }) {
                        Text("+ Foto")
                    }
                    Button(onClick = {
                        val finalName = name.trim().ifBlank { "Playlist Baru" }
                        showCreatePlaylistDialog = false
                        onCreatePlaylist(finalName)
                    }) {
                        Text("Buat")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog Rename Playlist
    val toRename = renamingPlaylist
    if (toRename != null) {
        var newName by remember(toRename) { mutableStateOf(toRename.name) }
        AlertDialog(
            onDismissRequest = { renamingPlaylist = null },
            title = { Text("Ganti Nama Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenamePlaylist(toRename, newName)
                    renamingPlaylist = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { renamingPlaylist = null }) { Text("Batal") }
            }
        )
    }

    // Dialog Pilih Playlist untuk Menambahkan Lagu
    val targetTrack = trackForPlaylistPicker
    if (targetTrack != null) {
        AlertDialog(
            onDismissRequest = { trackForPlaylistPicker = null },
            title = { Text("Tambahkan ke Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Belum ada playlist. Buat playlist dulu di tab Playlist.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        playlists.forEach { pl ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddTrackToPlaylist(pl.id, targetTrack)
                                        trackForPlaylistPicker = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                TextButton(onClick = { trackForPlaylistPicker = null }) { Text("Tutup") }
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
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPickCover: () -> Unit,
    onResetCover: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
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
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Menu lagu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Tambah ke Playlist") },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ubah Foto Icon Musik") },
                        leadingIcon = { Icon(Icons.Rounded.Image, null) },
                        onClick = {
                            menuExpanded = false
                            onPickCover()
                        }
                    )
                    if (customCoverPath != null) {
                        DropdownMenuItem(
                            text = { Text("Reset Foto Bawaan") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            onClick = {
                                menuExpanded = false
                                onResetCover()
                            }
                        )
                    }
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
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Text("Putar Playlist")
                    }
                    OutlinedButton(
                        onClick = onChangePhoto,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ubah Foto Playlist")
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Text(
                    text = "Playlist masih kosong. Buka tab Semua Lagu lalu ketuk ikon titik tiga di lagu mana pun untuk menambahkannya ke sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else {
            items(tracks, key = { it.mediaStoreId }) { track ->
                val isCurrent = track.trackHash == currentTrack?.trackHash
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(track) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                            Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        if (isCurrent) {
                            MiniEqualizerBars(isPlaying = isPlaying, modifier = Modifier.padding(end = 6.dp))
                        }
                        IconButton(onClick = { onRemoveTrack(track) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Keluarkan dari playlist", modifier = Modifier.size(18.dp))
                        }
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
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
            .height(40.dp)
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
