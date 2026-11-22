package com.notara.player.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.WaveformSeekBar
import com.notara.player.ui.components.parseHexColor
import com.notara.player.ui.formatTimeMs

private val pinColors = listOf("#E8A864", "#D99AA4", "#6B9E78", "#60A5FA", "#C084FC")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    track: LocalTrack,
    customCoverPath: String?,
    playlists: List<PlaylistEntity>,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    waveformBars: List<Float>,
    markers: List<TrackMarkerEntity>,
    selectedMarker: TrackMarkerEntity?,
    transcriptLines: List<TrackTranscriptEntity>,
    effectsState: AudioEffectsState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onMarkerTap: (TrackMarkerEntity) -> Unit,
    onClearSelectedMarker: () -> Unit,
    onAddMarker: (Long, String, String, String) -> Unit,
    onUpdateMarker: (TrackMarkerEntity, String, String, Long, String) -> Unit,
    onDeleteMarker: (TrackMarkerEntity) -> Unit,
    onOpenEffects: () -> Unit,
    onPickTrackCover: () -> Unit,
    onAddTrackToPlaylist: (Long) -> Unit,
    onSaveTranscriptRaw: (String) -> Unit,
    onSyncTranscriptLine: (TrackTranscriptEntity) -> Unit,
    onImportTranscriptFile: () -> Unit,
    onExportTranscriptLrc: () -> Unit,
    onExportMarkersJson: () -> Unit,
    onImportMarkersJson: () -> Unit
) {
    // false = Mode Catatan Pin / Sampul, true = Mode Lirik Berjalan (hanya mengganti kotak hero atas!)
    var isLyricsMode by remember { mutableStateOf(false) }
    var forceShowCover by remember { mutableStateOf(false) }
    var tapToSyncMode by remember { mutableStateOf(false) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var editingMarker by remember { mutableStateOf<TrackMarkerEntity?>(null) }
    var showEditTranscriptDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    // Pin aktif: jika pengguna mengetuk pin tertentu, tampilkan pin itu;
    // jika tidak, otomatis tampilkan pin terakhir yang sedang dilewati posisi lagu.
    val activePin = remember(selectedMarker, markers, positionMs) {
        selectedMarker ?: markers.lastOrNull { it.timestampMs <= positionMs }
    }

    LaunchedEffect(selectedMarker?.id) {
        if (selectedMarker != null) {
            forceShowCover = false
            isLyricsMode = false
        }
    }

    // Latar gradien hangat ala lampu amber Tokyo Midnight (GPU-ringan, 0 blur berat)
    val ambientGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    val playButtonRadius by animateDpAsState(
        targetValue = if (isPlaying) 20.dp else 34.dp,
        animationSpec = tween(220),
        label = "play_morph"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = tween(280),
        label = "cover_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(ambientGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Tutup", modifier = Modifier.size(30.dp))
                        }
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
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
                    },
                    actions = {
                        AssistChip(
                            onClick = onOpenEffects,
                            label = {
                                val badge = when {
                                    effectsState.volumePercent > 100 -> "${effectsState.volumePercent}%"
                                    effectsState.speed != 1.0f -> "%.1fx".format(effectsState.speed)
                                    else -> "Efek"
                                }
                                Text(badge, style = MaterialTheme.typography.labelSmall)
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Opsi lainnya")
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Ubah Foto Sampul Lagu") },
                                    leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onPickTrackCover()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tambah ke Playlist") },
                                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showPlaylistPicker = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Impor Penanda JSON") },
                                    leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onImportMarkersJson()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ekspor Penanda JSON") },
                                    leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onExportMarkersJson()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // =========================================================================
                // KOTAK HERO ATAS (AREA YANG DILINGKARI MERAH)
                // Mode Catatan -> Menampilkan Deskripsi Pin Aktif (atau Cover Art)
                // Mode Lirik   -> Berubah halus menjadi Lirik Berjalan dari atas ke bawah!
                // =========================================================================
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(238.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            RoundedCornerShape(26.dp)
                        )
                ) {
                    AnimatedContent(
                        targetState = isLyricsMode,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 10 }) togetherWith fadeOut(tween(160))
                        },
                        label = "hero_stage"
                    ) { lyricsActive ->
                        if (lyricsActive) {
                            InlineLyricsStage(
                                lines = transcriptLines,
                                positionMs = positionMs,
                                tapToSyncMode = tapToSyncMode,
                                onToggleTapToSync = { tapToSyncMode = !tapToSyncMode },
                                onSeek = onSeek,
                                onSyncLine = onSyncTranscriptLine,
                                onOpenEdit = { showEditTranscriptDialog = true },
                                onImportFile = onImportTranscriptFile,
                                onExportLrc = onExportTranscriptLrc
                            )
                        } else {
                            PinDescriptionOrCoverStage(
                                track = track,
                                customCoverPath = customCoverPath,
                                activePin = if (forceShowCover) null else activePin,
                                coverScale = coverScale,
                                hasAnyPin = activePin != null,
                                forceShowCover = forceShowCover,
                                onToggleCover = { forceShowCover = !forceShowCover },
                                onEditActivePin = { activePin?.let { editingMarker = it } }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Pill Switcher: [ Catatan | Lirik ] — tepat di bawah kotak hero
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CozySegmentPill(
                        label = "Catatan",
                        selected = !isLyricsMode,
                        onClick = { isLyricsMode = false },
                        modifier = Modifier.width(108.dp)
                    )
                    CozySegmentPill(
                        label = "Lirik",
                        selected = isLyricsMode,
                        onClick = { isLyricsMode = true },
                        modifier = Modifier.width(108.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Waveform SoundCloud + Pin (SELALU TERLIHAT bahkan saat Lirik aktif!)
                WaveformSeekBar(
                    amplitudes = waveformBars,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    markers = markers,
                    selectedMarkerId = activePin?.id,
                    onSeek = {
                        onClearSelectedMarker()
                        onSeek(it)
                    },
                    onMarkerTap = { marker ->
                        forceShowCover = false
                        isLyricsMode = false
                        onMarkerTap(marker)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTimeMs(positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTimeMs(durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(6.dp))

                // Kontrol Utama: Prev / Play-Pause Morphing / Next / + Tandai
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { showAddMarkerDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pin", style = MaterialTheme.typography.labelLarge)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(32.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(playButtonRadius),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(62.dp)
                                .clickable(onClick = onTogglePlayPause)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(32.dp))
                        }
                    }

                    // Spacer seimbang di kanan atau tombol cepat jumlah pin
                    Text(
                        text = "${markers.size} pin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Daftar Judul Pin di Bawah (Menampilkan Judul Pin + Detik)
                if (markers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada pin. Ketuk '+ Pin' untuk menandai detik penting — judul pin muncul di sini dan deskripsinya tampil di kotak atas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(markers, key = { it.id }) { marker ->
                            val isSelected = activePin?.id == marker.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        forceShowCover = false
                                        isLyricsMode = false
                                        onMarkerTap(marker)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bookmark,
                                        contentDescription = null,
                                        tint = parseHexColor(marker.colorHex),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = formatTimeMs(marker.timestampMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(48.dp)
                                    )
                                    Text(
                                        text = marker.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { editingMarker = marker },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit pin", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteMarker(marker) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus pin", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Tambah Pin (Judul Pin + Deskripsi Pin)
    if (showAddMarkerDialog) {
        MarkerEditorDialog(
            dialogTitle = "Tambah Pin Detik",
            initialTimestampMs = positionMs,
            initialTitle = "",
            initialDescription = "",
            initialColorHex = pinColors.first(),
            durationMs = durationMs,
            onDismiss = { showAddMarkerDialog = false },
            onSave = { ts, title, desc, color ->
                onAddMarker(ts, title, desc, color)
                showAddMarkerDialog = false
            }
        )
    }

    // Dialog Edit Pin
    val toEdit = editingMarker
    if (toEdit != null) {
        MarkerEditorDialog(
            dialogTitle = "Edit Pin",
            initialTimestampMs = toEdit.timestampMs,
            initialTitle = toEdit.title,
            initialDescription = toEdit.description,
            initialColorHex = toEdit.colorHex,
            durationMs = durationMs,
            onDismiss = { editingMarker = null },
            onSave = { ts, title, desc, color ->
                onUpdateMarker(toEdit, title, desc, ts, color)
                editingMarker = null
            }
        )
    }

    // Dialog Edit / Tempel Lirik & Transkrip
    if (showEditTranscriptDialog) {
        var rawText by remember(transcriptLines) {
            mutableStateOf(
                transcriptLines.joinToString("\n") { line ->
                    val ms = line.startMs
                    if (ms != null) {
                        val m = ms / 60_000L
                        val s = (ms % 60_000L) / 1_000L
                        val c = (ms % 1_000L) / 10L
                        "[%02d:%02d.%02d]%s".format(m, s, c, line.text)
                    } else {
                        line.text
                    }
                }
            )
        }
        AlertDialog(
            onDismissRequest = { showEditTranscriptDialog = false },
            title = { Text("Edit / Tempel Lirik & Transkrip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempel teks biasa atau format LRC/SRT. Aktifkan ikon Sync di panel lirik untuk menyinkronkan baris ke detik lagu.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        minLines = 6,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveTranscriptRaw(rawText)
                    showEditTranscriptDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditTranscriptDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog Tambah ke Playlist
    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Tambahkan ke Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Belum ada playlist. Buat playlist dulu di tab Playlist pada layar utama.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        playlists.forEach { pl ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddTrackToPlaylist(pl.id)
                                        showPlaylistPicker = false
                                    }
                            ) {
                                Text(pl.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun PinDescriptionOrCoverStage(
    track: LocalTrack,
    customCoverPath: String?,
    activePin: TrackMarkerEntity?,
    coverScale: Float,
    hasAnyPin: Boolean,
    forceShowCover: Boolean,
    onToggleCover: () -> Unit,
    onEditActivePin: () -> Unit
) {
    if (activePin != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CozyArtwork(
                    customCoverPath = customCoverPath,
                    albumArtUri = track.albumArtUri,
                    seedKey = track.trackHash,
                    sizeDp = 42,
                    cornerDp = 10,
                    modifier = Modifier.clickable(onClick = onToggleCover)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(activePin.colorHex))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatTimeMs(activePin.timestampMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = activePin.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onEditActivePin) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit catatan pin", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val descText = activePin.description.ifBlank {
                    "Belum ada deskripsi untuk pin ini. Ketuk ikon pensil di kanan atas untuk menambahkan catatan lengkap."
                }
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (activePin.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CozyArtwork(
                    customCoverPath = customCoverPath,
                    albumArtUri = track.albumArtUri,
                    seedKey = track.trackHash,
                    sizeDp = 166,
                    cornerDp = 22,
                    modifier = Modifier
                        .scale(coverScale)
                        .clickable { if (hasAnyPin && forceShowCover) onToggleCover() }
                )
                if (hasAnyPin && forceShowCover) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Ketuk sampul untuk kembali ke deskripsi pin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineLyricsStage(
    lines: List<TrackTranscriptEntity>,
    positionMs: Long,
    tapToSyncMode: Boolean,
    onToggleTapToSync: () -> Unit,
    onSeek: (Long) -> Unit,
    onSyncLine: (TrackTranscriptEntity) -> Unit,
    onOpenEdit: () -> Unit,
    onImportFile: () -> Unit,
    onExportLrc: () -> Unit
) {
    val listState = rememberLazyListState()
    val activeIndex = remember(positionMs, lines) {
        lines.indexOfLast { it.startMs != null && it.startMs <= positionMs }
    }

    LaunchedEffect(activeIndex, tapToSyncMode) {
        if (!tapToSyncMode && activeIndex >= 0 && activeIndex < lines.size) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mini bar kontrol lirik di pojok atas kotak hero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onOpenEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onImportFile, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Impor LRC", style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tapToSyncMode && lines.isNotEmpty()) {
                    val nextLine = lines.firstOrNull { it.startMs == null } ?: lines.last()
                    TextButton(onClick = { onSyncLine(nextLine) }) {
                        Text("Cap Detik", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onToggleTapToSync, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Mode Tap-to-Sync",
                        tint = if (tapToSyncMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onExportLrc, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "Ekspor LRC", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada lirik/transkrip. Ketuk 'Edit' untuk menempel teks atau 'Impor LRC' dari penyimpanan lokal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(lines, key = { _, item -> item.id }) { idx, line ->
                    val isActive = idx == activeIndex
                    Text(
                        text = line.text,
                        style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (tapToSyncMode) onSyncLine(line)
                                else if (line.startMs != null) onSeek(line.startMs)
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkerEditorDialog(
    dialogTitle: String,
    initialTimestampMs: Long,
    initialTitle: String,
    initialDescription: String,
    initialColorHex: String,
    durationMs: Long,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit
) {
    var timestampMs by remember { mutableLongStateOf(initialTimestampMs) }
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var colorHex by remember { mutableStateOf(initialColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { timestampMs = (timestampMs - 1000L).coerceAtLeast(0L) }) {
                        Text("-1 dtk")
                    }
                    Text(
                        text = formatTimeMs(timestampMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = { timestampMs = (timestampMs + 1000L).coerceAtMost(durationMs) }) {
                        Text("+1 dtk")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Pin (muncul di daftar bawah)") },
                    placeholder = { Text("Misal: Bagian Reff / Dialog 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Pin (muncul di kotak atas)") },
                    placeholder = { Text("Tulis catatan, arti kalimat, atau penjelasan bagian ini...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pinColors.forEach { hex ->
                        val selected = hex.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .then(
                                    if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(timestampMs, title, description, colorHex) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
