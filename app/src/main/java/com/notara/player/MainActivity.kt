package com.notara.player

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import com.notara.player.ui.MainViewModel
import com.notara.player.ui.components.AudioEffectsSheet
import com.notara.player.ui.screens.LibraryScreen
import com.notara.player.ui.screens.PlayerScreen
import com.notara.player.ui.theme.NotaraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotaraRoot(viewModel = viewModel)
        }
    }
}

@Composable
private fun NotaraRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasAudioPermission = grants[audioPermission] == true
        if (hasAudioPermission) viewModel.scanLibrary()
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.scanLibrary()
        } else {
            val perms = buildList {
                add(audioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
    }

    val tracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val waveformBars by viewModel.waveformBars.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val selectedMarker by viewModel.selectedMarker.collectAsStateWithLifecycle()
    val transcriptLines by viewModel.transcriptLines.collectAsStateWithLifecycle()
    val effectsState by viewModel.effectsState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var showPlayerScreen by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var seedColor by remember { mutableStateOf(Color(0xFF3B82F6)) }

    // Ekstraksi warna dinamis dari sampul album lokal (gaya ArchiveTune)
    LaunchedEffect(currentTrack?.albumArtUri) {
        val artUri = currentTrack?.albumArtUri
        if (artUri == null) {
            seedColor = Color(0xFF3B82F6)
            return@LaunchedEffect
        }
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(artUri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream) ?: return@use null
                    val palette = Palette.from(bmp).generate()
                    val rgb = palette.getVibrantColor(
                        palette.getMutedColor(0xFF3B82F6.toInt())
                    )
                    Color(rgb)
                }
            }.getOrNull()
        }
        seedColor = extracted ?: Color(0xFF3B82F6)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeStatusMessage()
        }
    }

    // SAF Launchers: Impor & Ekspor Transkrip / Penanda
    val importTranscriptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!text.isNullOrBlank()) {
                    viewModel.saveTranscriptText(text)
                }
            }
        }
    }

    val exportTranscriptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val lrc = viewModel.buildTranscriptExportLrc() ?: return@launch
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(lrc) }
                }
            }
        }
    }

    val exportMarkersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val json = viewModel.buildMarkersExportJson() ?: return@launch
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                }
            }
        }
    }

    val importMarkersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val json = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!json.isNullOrBlank()) {
                    viewModel.importMarkersJson(json)
                }
            }
        }
    }

    NotaraTheme(seedColor = seedColor) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!hasAudioPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Notara memerlukan izin akses audio lokal untuk memindai dan memutar file musik di perangkat Anda.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { permissionLauncher.launch(arrayOf(audioPermission)) }) {
                        Text("Izinkan Akses Audio")
                    }
                }
            } else if (showPlayerScreen && currentTrack != null) {
                PlayerScreen(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    waveformBars = waveformBars,
                    markers = markers,
                    selectedMarker = selectedMarker,
                    transcriptLines = transcriptLines,
                    effectsState = effectsState,
                    onBack = { showPlayerScreen = false },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onMarkerTap = viewModel::jumpToMarker,
                    onAddMarker = viewModel::addMarker,
                    onUpdateMarker = viewModel::updateMarker,
                    onDeleteMarker = viewModel::deleteMarker,
                    onOpenEffects = { showEffectsSheet = true },
                    onSaveTranscriptRaw = viewModel::saveTranscriptText,
                    onSyncTranscriptLine = viewModel::syncTranscriptLineToCurrentTime,
                    onImportTranscriptFile = { importTranscriptLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                    onExportTranscriptLrc = {
                        val safeName = (currentTrack?.title ?: "transkrip").replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        exportTranscriptLauncher.launch("${safeName}.lrc")
                    },
                    onExportMarkersJson = {
                        val safeName = (currentTrack?.title ?: "penanda").replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        exportMarkersLauncher.launch("${safeName}_markers.json")
                    },
                    onImportMarkersJson = { importMarkersLauncher.launch(arrayOf("application/json", "text/*")) }
                )
            } else {
                LibraryScreen(
                    tracks = tracks,
                    folders = folders,
                    selectedFolder = selectedFolder,
                    searchQuery = searchQuery,
                    isScanning = isScanning,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onSearchChange = viewModel::updateSearchQuery,
                    onFolderSelect = viewModel::selectFolder,
                    onRescan = viewModel::scanLibrary,
                    onTrackClick = { track ->
                        viewModel.playTrack(track)
                        showPlayerScreen = true
                    },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onOpenPlayer = { showPlayerScreen = true }
                )
            }

            if (showEffectsSheet) {
                AudioEffectsSheet(
                    state = effectsState,
                    onDismiss = { showEffectsSheet = false },
                    onVolumeChange = viewModel::setVolumePercent,
                    onLimiterChange = viewModel::setLimiterEnabled,
                    onSpeedChange = viewModel::setSpeed,
                    onPitchChange = viewModel::setPitch,
                    onSkipSilenceChange = viewModel::setSkipSilence,
                    onEqEnabledChange = viewModel::setEqEnabled,
                    onPresetSelect = viewModel::selectEqPreset,
                    onEqBandChange = viewModel::setEqBand,
                    onBassBoostChange = viewModel::setBassBoost,
                    onReverbSelect = viewModel::setReverb,
                    onResetAll = viewModel::resetAudioEffects
                )
            }
        }
    }
}
