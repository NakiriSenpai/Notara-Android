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
import androidx.compose.material.icons.rounded.ChatBubbleOutline
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.db.TrackMarkerEntity
import com.notara.player.data.db.TrackTranscriptEntity
import com.notara.player.data.model.AudioEffectsState
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.WaveformSeekBar
import com.notara.player.ui.components.parseHexColor
import com.notara.player.ui.formatTimeMs
import com.notara.player.ui.i18n.LocalNotaraStrings
import kotlinx.coroutines.flow.StateFlow

private val pinColors = listOf("#E8A864", "#D99AA4", "#6B9E78", "#60A5FA", "#C084FC")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    track: LocalTrack,
    customCoverPath: String?,
    playlists: List<PlaylistEntity>,
    isPlaying: Boolean,
    positionMsFlow: StateFlow<Long>,
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
    onAddTrackToPlaylist: (PlaylistEntity) -> Unit,
    onSaveTranscriptRaw: (String) -> Unit,
    onSyncTranscriptLine: (TrackTranscriptEntity) -> Unit,
    onImportTranscriptFile: () -> Unit,
    onExportTranscriptLrc: () -> Unit,
    onExportMarkersJson: () -> Unit,
    onImportMarkersJson: () -> Unit
) {
    val strings = LocalNotaraStrings.current

    var isLyricsMode by remember { mutableStateOf(false) }
    var forceShowCover by remember { mutableStateOf(false) }
    var tapToSyncMode by remember { mutableStateOf(false) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var editingMarker by remember { mutableStateOf<TrackMarkerEntity?>(null) }
    var showEditTranscriptDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMarker?.id) {
        if (selectedMarker != null) {
            forceShowCover = false
            isLyricsMode = false
        }
    }

    // Gradien cahaya lampu amber hangat Tokyo Midnight yang persis seperti mockup (GPU-ringan)
    val warmLampGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
            0.34f to MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            0.62f to MaterialTheme.colorScheme.background,
            1.00f to MaterialTheme.colorScheme.background
        )
    )

    val playButtonRadius by animateDpAsState(
        targetValue = if (isPlaying) 20.dp else 32.dp,
        animationSpec = tween(200),
        label = "play_morph"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = tween(240),
        label = "cover_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(warmLampGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = strings.nowPlaying,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${track.title} · ${track.artist}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.clickable(onClick = onOpenEffects)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                val badge = when {
                                    effectsState.volumePercent > 100 -> "${effectsState.volumePercent}%"
                                    effectsState.speed != 1.0f -> "%.1fx".format(effectsState.speed)
                                    else -> strings.effectsBtn
                                }
                                Text(badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.changeTrackPhoto) },
                                    leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                    onClick = { showMoreMenu = false; onPickTrackCover() }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.addToPlaylist) },
                                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                                    onClick = { showMoreMenu = false; showPlaylistPicker = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.importMarkersJson) },
                                    leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                                    onClick = { showMoreMenu = false; onImportMarkersJson() }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.exportMarkersJson) },
                                    leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                    onClick = { showMoreMenu = false; onExportMarkersJson() }
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))

                // =========================================================================
                // HERO STAGE ATAS (PERSIS MOCKUP TOKYO MIDNIGHT — TANPA KOTAK KOSONG MURAHAN)
                // - Saat tampil sampul: sampul 216dp berbingkai ganda lembut langsung di atas cahaya amber
                // - Saat tampil Deskripsi Pin atau Lirik: kartu espresso transparan elegan yang sama tingginya
                // =========================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isLyricsMode,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 14 }) togetherWith fadeOut(tween(150))
                        },
                        label = "hero_stage"
                    ) { lyricsActive ->
                        if (lyricsActive) {
                            InlineLyricsStage(
                                lines = transcriptLines,
                                positionMsFlow = positionMsFlow,
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
                                markers = markers,
                                selectedMarker = selectedMarker,
                                positionMsFlow = positionMsFlow,
                                coverScale = coverScale,
                                forceShowCover = forceShowCover,
                                onToggleCover = { forceShowCover = !forceShowCover },
                                onEditPin = { editingMarker = it }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Pill Switcher [ Catatan | Lirik ] — persis posisi di mockup
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CozySegmentPill(
                        label = strings.notesTab,
                        selected = !isLyricsMode,
                        onClick = { isLyricsMode = false },
                        modifier = Modifier.width(102.dp)
                    )
                    CozySegmentPill(
                        label = strings.lyricsTab,
                        selected = isLyricsMode,
                        onClick = { isLyricsMode = true },
                        modifier = Modifier.width(102.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Waveform Real PCM + Pill Komentar + Waktu (Leaf composable: hanya bagian ini yang update tiap 200ms!)
                PlayerWaveformSection(
                    amplitudes = waveformBars,
                    positionMsFlow = positionMsFlow,
                    durationMs = durationMs,
                    markers = markers,
                    selectedMarker = selectedMarker,
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

                Spacer(Modifier.height(8.dp))

                // Kontrol Pemutar Utama (Persis Mockup: + Pin | Prev | Squircle Play/Pause | Next | Count)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.clickable { showAddMarkerDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(strings.pinBtn, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(playButtonRadius),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(60.dp)
                                .clickable(onClick = onTogglePlayPause)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    }

                    Text(
                        text = strings.pinsCount(markers.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Daftar Judul Pin Minimalis ala Tokyo Midnight Mockup (tanpa kartu abu-abu menumpuk!)
                if (markers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noPinsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(markers, key = { it.id }) { marker ->
                            val isSelected = selectedMarker?.id == marker.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        forceShowCover = false
                                        isLyricsMode = false
                                        onMarkerTap(marker)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(marker.colorHex))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = formatTimeMs(marker.timestampMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(46.dp)
                                )
                                Text(
                                    text = marker.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { editingMarker = marker }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteMarker(marker) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMarkerDialog) {
        MarkerEditorDialog(
            dialogTitle = strings.addPinDialogTitle,
            initialTimestampMs = positionMsFlow.value,
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

    val toEdit = editingMarker
    if (toEdit != null) {
        MarkerEditorDialog(
            dialogTitle = strings.editPinDialogTitle,
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
            title = { Text(strings.editLyricsTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.editLyricsHint, style = MaterialTheme.typography.bodySmall)
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
                Button(onClick = { onSaveTranscriptRaw(rawText); showEditTranscriptDialog = false }) { Text(strings.saveBtn) }
            },
            dismissButton = {
                TextButton(onClick = { showEditTranscriptDialog = false }) { Text(strings.cancelBtn) }
            }
        )
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
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
                                        onAddTrackToPlaylist(pl)
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
                TextButton(onClick = { showPlaylistPicker = false }) { Text(strings.closeBtn) }
            }
        )
    }
}

@Composable
private fun PlayerWaveformSection(
    amplitudes: List<Float>,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    markers: List<TrackMarkerEntity>,
    selectedMarker: TrackMarkerEntity?,
    onSeek: (Long) -> Unit,
    onMarkerTap: (TrackMarkerEntity) -> Unit
) {
    val positionMs by positionMsFlow.collectAsStateWithLifecycle()
    val activePin = remember(selectedMarker, markers, positionMs) {
        selectedMarker ?: markers.lastOrNull { it.timestampMs <= positionMs }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Gelembung pill judul pin mungil di atas waveform (persis pill 'comment' di mockup Tokyo Midnight)
        if (activePin != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = activePin.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WaveformSeekBar(
            amplitudes = amplitudes,
            positionMs = positionMs,
            durationMs = durationMs,
            markers = markers,
            selectedMarkerId = activePin?.id,
            onSeek = onSeek,
            onMarkerTap = onMarkerTap
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimeMs(positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTimeMs(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PinDescriptionOrCoverStage(
    track: LocalTrack,
    customCoverPath: String?,
    markers: List<TrackMarkerEntity>,
    selectedMarker: TrackMarkerEntity?,
    positionMsFlow: StateFlow<Long>,
    coverScale: Float,
    forceShowCover: Boolean,
    onToggleCover: () -> Unit,
    onEditPin: (TrackMarkerEntity) -> Unit
) {
    val strings = LocalNotaraStrings.current
    val positionMs by positionMsFlow.collectAsStateWithLifecycle()
    val resolvedPin = remember(selectedMarker, markers, positionMs) {
        selectedMarker ?: markers.lastOrNull { it.timestampMs <= positionMs }
    }
    val displayPin = if (forceShowCover) null else resolvedPin

    if (displayPin != null) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(parseHexColor(displayPin.colorHex)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formatTimeMs(displayPin.timestampMs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = displayPin.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onEditPin(displayPin) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    val descText = displayPin.description.ifBlank { strings.noPinDescHint }
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (displayPin.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        // Persis Mockup Tokyo Midnight: Bingkai sampul ganda hangat langsung di atas cahaya amber!
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .scale(coverScale)
                    .size(204.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f), RoundedCornerShape(30.dp))
                    .padding(7.dp)
                    .clickable { if (resolvedPin != null && forceShowCover) onToggleCover() },
                contentAlignment = Alignment.Center
            ) {
                CozyArtwork(
                    customCoverPath = customCoverPath,
                    albumArtUri = track.albumArtUri,
                    seedKey = track.trackHash,
                    sizeDp = 190,
                    cornerDp = 24
                )
            }
            if (resolvedPin != null && forceShowCover) {
                Spacer(Modifier.height(6.dp))
                Text(strings.tapCoverReturnNotes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun InlineLyricsStage(
    lines: List<TrackTranscriptEntity>,
    positionMsFlow: StateFlow<Long>,
    tapToSyncMode: Boolean,
    onToggleTapToSync: () -> Unit,
    onSeek: (Long) -> Unit,
    onSyncLine: (TrackTranscriptEntity) -> Unit,
    onOpenEdit: () -> Unit,
    onImportFile: () -> Unit,
    onExportLrc: () -> Unit
) {
    val strings = LocalNotaraStrings.current
    val positionMs by positionMsFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val activeIndex = remember(positionMs, lines) {
        lines.indexOfLast { it.startMs != null && it.startMs <= positionMs }
    }

    LaunchedEffect(activeIndex, tapToSyncMode) {
        if (!tapToSyncMode && activeIndex >= 0 && activeIndex < lines.size) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onOpenEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text(strings.editBtn, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onImportFile, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text(strings.importLrcBtn, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tapToSyncMode && lines.isNotEmpty()) {
                        val nextLine = lines.firstOrNull { it.startMs == null } ?: lines.last()
                        TextButton(onClick = { onSyncLine(nextLine) }) {
                            Text(strings.stampTimeBtn, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onToggleTapToSync, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = if (tapToSyncMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onExportLrc, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (lines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text(strings.noLyricsHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
    val strings = LocalNotaraStrings.current
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
                    OutlinedButton(onClick = { timestampMs = (timestampMs - 1000L).coerceAtLeast(0L) }) { Text("-1s") }
                    Text(formatTimeMs(timestampMs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { timestampMs = (timestampMs + 1000L).coerceAtMost(durationMs) }) { Text("+1s") }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.pinTitleLabel) },
                    placeholder = { Text(strings.pinTitleHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.pinDescLabel) },
                    placeholder = { Text(strings.pinDescHint) },
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
            Button(onClick = { onSave(timestampMs, title, description, colorHex) }) { Text(strings.saveBtn) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelBtn) }
        }
    )
}
