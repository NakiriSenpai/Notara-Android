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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.WaveformSeekBar
import com.notara.player.ui.components.parseHexColor
import com.notara.player.ui.formatTimeMs

private val pinColors = listOf("#F59E0B", "#3B82F6", "#10B981", "#EC4899", "#8B5CF6")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    track: LocalTrack,
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
    onAddMarker: (Long, String, String) -> Unit,
    onUpdateMarker: (TrackMarkerEntity, String, Long, String) -> Unit,
    onDeleteMarker: (TrackMarkerEntity) -> Unit,
    onOpenEffects: () -> Unit,
    onSaveTranscriptRaw: (String) -> Unit,
    onSyncTranscriptLine: (TrackTranscriptEntity) -> Unit,
    onImportTranscriptFile: () -> Unit,
    onExportTranscriptLrc: () -> Unit,
    onExportMarkersJson: () -> Unit,
    onImportMarkersJson: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var editingMarker by remember { mutableStateOf<TrackMarkerEntity?>(null) }
    var showEditTranscriptDialog by remember { mutableStateOf(false) }
    var tapToSyncMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Text(
                        text = track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    AssistChip(
                        onClick = onOpenEffects,
                        label = {
                            val badge = if (effectsState.volumePercent > 100) {
                                "${effectsState.volumePercent}%"
                            } else if (effectsState.speed != 1.0f) {
                                "%.1fx".format(effectsState.speed)
                            } else {
                                "Efek"
                            }
                            Text(badge)
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Pemutar & Penanda (${markers.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Transkrip (${transcriptLines.size})") }
                )
            }

            if (activeTab == 0) {
                PlayerAndMarkersTab(
                    track = track,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    waveformBars = waveformBars,
                    markers = markers,
                    selectedMarker = selectedMarker,
                    onSeek = onSeek,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onMarkerTap = onMarkerTap,
                    onOpenAddMarker = { showAddMarkerDialog = true },
                    onEditMarker = { editingMarker = it },
                    onDeleteMarker = onDeleteMarker,
                    onExportMarkersJson = onExportMarkersJson,
                    onImportMarkersJson = onImportMarkersJson
                )
            } else {
                TranscriptTab(
                    lines = transcriptLines,
                    positionMs = positionMs,
                    isPlaying = isPlaying,
                    tapToSyncMode = tapToSyncMode,
                    onToggleTapToSync = { tapToSyncMode = !tapToSyncMode },
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onSyncLine = onSyncTranscriptLine,
                    onOpenEditDialog = { showEditTranscriptDialog = true },
                    onImportFile = onImportTranscriptFile,
                    onExportLrc = onExportTranscriptLrc
                )
            }
        }
    }

    // Dialog Tambah Penanda di Detik Aktif
    if (showAddMarkerDialog) {
        MarkerEditorDialog(
            title = "Tambah Penanda Detik",
            initialTimestampMs = positionMs,
            initialComment = "",
            initialColorHex = pinColors.first(),
            durationMs = durationMs,
            onDismiss = { showAddMarkerDialog = false },
            onSave = { ts, comment, color ->
                onAddMarker(ts, comment, color)
                showAddMarkerDialog = false
            }
        )
    }

    // Dialog Edit Penanda
    val toEdit = editingMarker
    if (toEdit != null) {
        MarkerEditorDialog(
            title = "Edit Penanda",
            initialTimestampMs = toEdit.timestampMs,
            initialComment = toEdit.comment,
            initialColorHex = toEdit.colorHex,
            durationMs = durationMs,
            onDismiss = { editingMarker = null },
            onSave = { ts, comment, color ->
                onUpdateMarker(toEdit, comment, ts, color)
                editingMarker = null
            }
        )
    }

    // Dialog Tempel / Edit Transkrip Manual
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
            title = { Text("Edit / Tempel Transkrip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempel teks biasa (1 baris per kalimat) atau format LRC/SRT. Teks biasa bisa disinkronkan lewat mode Tap-to-Sync.",
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
}

@Composable
private fun PlayerAndMarkersTab(
    track: LocalTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    waveformBars: List<Float>,
    markers: List<TrackMarkerEntity>,
    selectedMarker: TrackMarkerEntity?,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onMarkerTap: (TrackMarkerEntity) -> Unit,
    onOpenAddMarker: () -> Unit,
    onEditMarker: (TrackMarkerEntity) -> Unit,
    onDeleteMarker: (TrackMarkerEntity) -> Unit,
    onExportMarkersJson: () -> Unit,
    onImportMarkersJson: () -> Unit
) {
    // Temukan komentar aktif otomatis jika pemutaran sedang melewati penanda (±1.5 detik)
    val nearbyMarker = remember(positionMs, markers, selectedMarker) {
        selectedMarker ?: markers.firstOrNull { kotlin.math.abs(it.timestampMs - positionMs) <= 1500L }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(uri = track.albumArtUri, sizeDp = 96, cornerDp = 18)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.folderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Gelembung komentar aktif ala SoundCloud
        if (nearbyMarker != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(nearbyMarker.colorHex))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatTimeMs(nearbyMarker.timestampMs),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = nearbyMarker.comment,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Waveform Interaktif + Pin Penanda
        item {
            Column {
                WaveformSeekBar(
                    amplitudes = waveformBars,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    markers = markers,
                    selectedMarkerId = selectedMarker?.id,
                    onSeek = onSeek,
                    onMarkerTap = onMarkerTap
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTimeMs(positionMs), style = MaterialTheme.typography.labelMedium)
                    Text(formatTimeMs(durationMs), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Kontrol Utama Pemutar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(34.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(34.dp))
                }
            }
        }

        // Tombol Cepat Tandai Detik + Backup JSON
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenAddMarker,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Tandai ${formatTimeMs(positionMs)}")
                }
                IconButton(onClick = onImportMarkersJson) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = "Impor Penanda JSON")
                }
                IconButton(onClick = onExportMarkersJson) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "Ekspor Penanda JSON")
                }
            }
        }

        // Daftar Pintasan Penanda & Komentar
        if (markers.isEmpty()) {
            item {
                Text(
                    text = "Belum ada penanda di audio ini. Ketuk tombol Tandai di atas untuk menyimpan detik penting beserta komentarnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(markers, key = { it.id }) { marker ->
                Surface(
                    tonalElevation = if (selectedMarker?.id == marker.id) 4.dp else 1.dp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMarkerTap(marker) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = parseHexColor(marker.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatTimeMs(marker.timestampMs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = marker.comment,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = { onEditMarker(marker) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit penanda", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteMarker(marker) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Hapus penanda", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptTab(
    lines: List<TrackTranscriptEntity>,
    positionMs: Long,
    isPlaying: Boolean,
    tapToSyncMode: Boolean,
    onToggleTapToSync: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSyncLine: (TrackTranscriptEntity) -> Unit,
    onOpenEditDialog: () -> Unit,
    onImportFile: () -> Unit,
    onExportLrc: () -> Unit
) {
    val listState = rememberLazyListState()

    val activeIndex = remember(positionMs, lines) {
        lines.indexOfLast { it.startMs != null && it.startMs <= positionMs }
    }

    LaunchedEffect(activeIndex, tapToSyncMode) {
        if (!tapToSyncMode && activeIndex >= 0 && activeIndex < lines.size) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Bar Aksi Transkrip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = onOpenEditDialog) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Teks")
            }
            OutlinedButton(onClick = onImportFile) {
                Text("Impor LRC/SRT")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleTapToSync) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Mode Tap-to-Sync",
                    tint = if (tapToSyncMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onExportLrc) {
                Icon(Icons.Rounded.FileDownload, contentDescription = "Ekspor LRC")
            }
        }

        if (tapToSyncMode && lines.isNotEmpty()) {
            val nextUnsynced = lines.firstOrNull { it.startMs == null } ?: lines.last()
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode Tap-to-Sync · ${formatTimeMs(positionMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = nextUnsynced.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                    }
                    Button(onClick = { onSyncLine(nextUnsynced) }) {
                        Text("Sinkronkan")
                    }
                }
            }
        }

        if (lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada transkrip. Ketuk 'Teks' untuk menempel transkrip/lirik manual atau 'Impor LRC/SRT' dari penyimpanan lokal.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(lines, key = { _, item -> item.id }) { idx, line ->
                    val isActive = idx == activeIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (tapToSyncMode) {
                                    onSyncLine(line)
                                } else if (line.startMs != null) {
                                    onSeek(line.startMs)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = line.startMs?.let { formatTimeMs(it) } ?: "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .width(48.dp)
                                .padding(top = 3.dp)
                        )
                        Text(
                            text = line.text,
                            style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerEditorDialog(
    title: String,
    initialTimestampMs: Long,
    initialComment: String,
    initialColorHex: String,
    durationMs: Long,
    onDismiss: () -> Unit,
    onSave: (Long, String, String) -> Unit
) {
    var timestampMs by remember { mutableLongStateOf(initialTimestampMs) }
    var comment by remember { mutableStateOf(initialComment) }
    var colorHex by remember { mutableStateOf(initialColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentar / catatan bagian ini") },
                    placeholder = { Text("Misal: Bagian reff / Kosakata penting") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pinColors.forEach { hex ->
                        val selected = hex.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(timestampMs, comment, colorHex) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
