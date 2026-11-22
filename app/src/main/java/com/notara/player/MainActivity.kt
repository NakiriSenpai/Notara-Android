package com.notara.player

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.ui.MainViewModel
import com.notara.player.ui.components.AudioEffectsSheet
import com.notara.player.ui.components.LanguagePickerSheet
import com.notara.player.ui.components.ThemePickerSheet
import com.notara.player.ui.i18n.LocalNotaraStrings
import com.notara.player.ui.i18n.stringsFor
import com.notara.player.ui.screens.LibraryScreen
import com.notara.player.ui.screens.PlayerScreen
import com.notara.player.ui.theme.NotaraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

private sealed interface PhotoPickTarget {
    data class TrackCover(val trackHash: String) : PhotoPickTarget
    data class PlaylistCover(val playlist: PlaylistEntity) : PhotoPickTarget
    data class NewPlaylistWithCover(val name: String) : PhotoPickTarget
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
        if (hasAudioPermission) viewModel.loadLibraryIfNeeded()
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.loadLibraryIfNeeded()
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

    val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(language) { stringsFor(language) }

    val allTracks by viewModel.tracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val customCovers by viewModel.customCovers.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistTrackMap by viewModel.playlistTrackMap.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val waveformBars by viewModel.waveformBars.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val selectedMarker by viewModel.selectedMarker.collectAsStateWithLifecycle()
    val transcriptLines by viewModel.transcriptLines.collectAsStateWithLifecycle()
    val effectsState by viewModel.effectsState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val duplicateAlert by viewModel.duplicatePlaylistAlert.collectAsStateWithLifecycle()

    var showPlayerScreen by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var pendingPhotoTarget by remember { mutableStateOf<PhotoPickTarget?>(null) }
    var dynamicSeed by remember { mutableStateOf(Color(0xFFE8A864)) }

    // Tombol Back HP: tutup sheet dulu -> lalu tutup layar pemutar ke Home!
    BackHandler(enabled = showEffectsSheet || showThemePicker || showLanguagePicker) {
        showEffectsSheet = false
        showThemePicker = false
        showLanguagePicker = false
    }
    BackHandler(enabled = showPlayerScreen && !showEffectsSheet && !showThemePicker && !showLanguagePicker) {
        showPlayerScreen = false
    }

    val currentCustomCoverPath = currentTrack?.let { customCovers[it.trackHash] }

    LaunchedEffect(currentTrack?.albumArtUri, currentCustomCoverPath) {
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val bmp = when {
                    !currentCustomCoverPath.isNullOrBlank() && File(currentCustomCoverPath).exists() ->
                        BitmapFactory.decodeFile(currentCustomCoverPath)
                    currentTrack?.albumArtUri != null ->
                        context.contentResolver.openInputStream(currentTrack!!.albumArtUri!!)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    else -> null
                } ?: return@runCatching null
                val palette = Palette.from(bmp).generate()
                val rgb = palette.getVibrantColor(palette.getMutedColor(0xFFE8A864.toInt()))
                Color(rgb)
            }.getOrNull()
        }
        dynamicSeed = extracted ?: Color(0xFFE8A864)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeStatusMessage()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val target = pendingPhotoTarget
        pendingPhotoTarget = null
        if (target == null) return@rememberLauncherForActivityResult
        when (target) {
            is PhotoPickTarget.TrackCover -> if (uri != null) viewModel.updateTrackCustomCover(target.trackHash, uri)
            is PhotoPickTarget.PlaylistCover -> if (uri != null) viewModel.updatePlaylist(target.playlist, target.playlist.name, uri)
            is PhotoPickTarget.NewPlaylistWithCover -> viewModel.createPlaylist(target.name, uri)
        }
    }

    val importTranscriptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!text.isNullOrBlank()) viewModel.saveTranscriptText(text)
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
                if (!json.isNullOrBlank()) viewModel.importMarkersJson(json)
            }
        }
    }

    CompositionLocalProvider(LocalNotaraStrings provides strings) {
        NotaraTheme(preset = themePreset, dynamicSeed = dynamicSeed) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (!hasAudioPermission) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(strings.permissionBody, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { permissionLauncher.launch(arrayOf(audioPermission)) }) {
                            Text(strings.allowAudioBtn)
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = showPlayerScreen && currentTrack != null,
                        transitionSpec = {
                            if (targetState) {
                                (slideInVertically(tween(220)) { it / 8 } + fadeIn(tween(180))) togetherWith fadeOut(tween(120))
                            } else {
                                fadeIn(tween(180)) togetherWith (slideOutVertically(tween(200)) { it / 8 } + fadeOut(tween(150)))
                            }
                        },
                        label = "screen_transition"
                    ) { inPlayer ->
                        if (inPlayer && currentTrack != null) {
                            val activeTrack = currentTrack!!
                            PlayerScreen(
                                track = activeTrack,
                                customCoverPath = customCovers[activeTrack.trackHash],
                                playlists = playlists,
                                isPlaying = isPlaying,
                                positionMsFlow = viewModel.currentPositionMs,
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
                                onClearSelectedMarker = viewModel::clearSelectedMarker,
                                onAddMarker = viewModel::addMarker,
                                onUpdateMarker = viewModel::updateMarker,
                                onDeleteMarker = viewModel::deleteMarker,
                                onOpenEffects = { showEffectsSheet = true },
                                onPickTrackCover = {
                                    pendingPhotoTarget = PhotoPickTarget.TrackCover(activeTrack.trackHash)
                                    photoPickerLauncher.launch("image/*")
                                },
                                onAddTrackToPlaylist = { pl -> viewModel.addTrackToPlaylist(pl, activeTrack) },
                                onSaveTranscriptRaw = viewModel::saveTranscriptText,
                                onSyncTranscriptLine = viewModel::syncTranscriptLineToCurrentTime,
                                onImportTranscriptFile = { importTranscriptLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                                onExportTranscriptLrc = {
                                    val safeName = activeTrack.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    exportTranscriptLauncher.launch("${safeName}.lrc")
                                },
                                onExportMarkersJson = {
                                    val safeName = activeTrack.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    exportMarkersLauncher.launch("${safeName}_markers.json")
                                },
                                onImportMarkersJson = { importMarkersLauncher.launch(arrayOf("application/json", "text/*")) }
                            )
                        } else {
                            LibraryScreen(
                                allTracks = allTracks,
                                filteredTracks = filteredTracks,
                                folders = folders,
                                selectedFolder = selectedFolder,
                                searchQuery = searchQuery,
                                isScanning = isScanning,
                                customCovers = customCovers,
                                playlists = playlists,
                                playlistTrackMap = playlistTrackMap,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                positionMsFlow = viewModel.currentPositionMs,
                                durationMs = durationMs,
                                onSearchChange = viewModel::updateSearchQuery,
                                onFolderSelect = viewModel::selectFolder,
                                onRescan = {
                                    Toast.makeText(context, strings.rescanStarted, Toast.LENGTH_SHORT).show()
                                    viewModel.rescanLibrary()
                                },
                                onOpenThemePicker = { showThemePicker = true },
                                onOpenLanguagePicker = { showLanguagePicker = true },
                                onTrackClick = { track, queue ->
                                    viewModel.playTrack(track, queue)
                                    showPlayerScreen = true
                                },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSkipNext = viewModel::skipNext,
                                onOpenPlayer = { showPlayerScreen = true },
                                onPickTrackCover = { track ->
                                    pendingPhotoTarget = PhotoPickTarget.TrackCover(track.trackHash)
                                    photoPickerLauncher.launch("image/*")
                                },
                                onResetTrackCover = { track -> viewModel.resetTrackCustomCover(track.trackHash) },
                                onCreatePlaylist = { name -> viewModel.createPlaylist(name, null) },
                                onCreatePlaylistWithPhoto = { name ->
                                    pendingPhotoTarget = PhotoPickTarget.NewPlaylistWithCover(name)
                                    photoPickerLauncher.launch("image/*")
                                },
                                onRenamePlaylist = { pl, newName -> viewModel.updatePlaylist(pl, newName, null) },
                                onPickPlaylistCover = { pl ->
                                    pendingPhotoTarget = PhotoPickTarget.PlaylistCover(pl)
                                    photoPickerLauncher.launch("image/*")
                                },
                                onDeletePlaylist = viewModel::deletePlaylist,
                                onAddTrackToPlaylist = { pl, track -> viewModel.addTrackToPlaylist(pl, track) },
                                onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist
                            )
                        }
                    }
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

                if (showThemePicker) {
                    ThemePickerSheet(
                        currentPreset = themePreset,
                        onSelect = viewModel::selectTheme,
                        onDismiss = { showThemePicker = false }
                    )
                }

                if (showLanguagePicker) {
                    LanguagePickerSheet(
                        currentLanguage = language,
                        onSelect = viewModel::selectLanguage,
                        onDismiss = { showLanguagePicker = false }
                    )
                }

                // Popup Peringatan jika lagu yang sama sudah ada di playlist tersebut
                val dup = duplicateAlert
                if (dup != null) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissDuplicateAlert,
                        title = { Text(strings.duplicateTitle) },
                        text = { Text(strings.duplicateBody(dup.trackTitle, dup.playlistName)) },
                        confirmButton = {
                            Button(onClick = viewModel::dismissDuplicateAlert) {
                                Text(strings.okBtn)
                            }
                        }
                    )
                }
            }
        }
    }
}
