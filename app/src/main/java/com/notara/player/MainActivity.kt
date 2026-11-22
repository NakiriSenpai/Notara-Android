package com.notara.player

import android.Manifest
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import com.notara.player.data.db.PlaylistEntity
import com.notara.player.data.model.LocalTrack
import com.notara.player.ui.MainViewModel
import com.notara.player.ui.components.AudioEffectsSheet
import com.notara.player.ui.components.CozyArtwork
import com.notara.player.ui.components.LanguagePickerSheet
import com.notara.player.ui.components.SettingsSheet
import com.notara.player.ui.components.ThemePickerSheet
import com.notara.player.ui.components.WavyCircularProgressRing
import com.notara.player.ui.i18n.LocalNotaraStrings
import com.notara.player.ui.i18n.stringsFor
import com.notara.player.ui.screens.HomeScreen
import com.notara.player.ui.screens.PlayerScreen
import com.notara.player.ui.screens.PlaylistsScreen
import com.notara.player.ui.screens.SearchLibraryScreen
import com.notara.player.ui.theme.NotaraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
    }

    val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(language) { stringsFor(language) }
    val filterConfig by viewModel.filterConfig.collectAsStateWithLifecycle()

    val allVisibleTracks by viewModel.visibleTracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayedTracks.collectAsStateWithLifecycle()
    val visibleFolders by viewModel.visibleFolders.collectAsStateWithLifecycle()
    val allAvailableFolders by viewModel.allAvailableFolders.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val favoriteHashes by viewModel.favoriteHashes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val customCovers by viewModel.customCovers.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistTrackMap by viewModel.playlistTrackMap.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val abLoop by viewModel.abLoop.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val waveformBars by viewModel.waveformBars.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val selectedMarker by viewModel.selectedMarker.collectAsStateWithLifecycle()
    val transcriptLines by viewModel.transcriptLines.collectAsStateWithLifecycle()
    val effectsState by viewModel.effectsState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val duplicateAlert by viewModel.duplicatePlaylistAlert.collectAsStateWithLifecycle()

    var bottomTab by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Search, 2 = Playlists
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showPlayerScreen by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var pendingPhotoTarget by remember { mutableStateOf<PhotoPickTarget?>(null) }
    var editingMetadataTrack by remember { mutableStateOf<LocalTrack?>(null) }
    var pendingMetaWrite by remember { mutableStateOf<Triple<LocalTrack, String, String>?>(null) }
    var dynamicSeed by remember { mutableStateOf(Color(0xFFE8A864)) }

    // Sistem Back HP berurutan: Sheet -> Player -> Playlist Detail -> Search/Playlist Tab -> Home -> Exit
    val anySheetOpen = showEffectsSheet || showSettingsSheet || showThemePicker || showLanguagePicker
    BackHandler(enabled = anySheetOpen) {
        showEffectsSheet = false
        showSettingsSheet = false
        showThemePicker = false
        showLanguagePicker = false
    }
    BackHandler(enabled = !anySheetOpen && showPlayerScreen) {
        showPlayerScreen = false
    }
    BackHandler(enabled = !anySheetOpen && !showPlayerScreen && (openedPlaylistId != null || bottomTab != 0)) {
        if (openedPlaylistId != null) {
            openedPlaylistId = null
        } else {
            bottomTab = 0
        }
    }

    val currentCustomCoverPath = currentTrack?.let { customCovers[it.trackHash] }

    LaunchedEffect(currentTrack?.albumArtUri, currentCustomCoverPath) {
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val bmp = when {
                    !currentCustomCoverPath.isNullOrBlank() && File(currentCustomCoverPath).exists() ->
                        BitmapFactory.decodeFile(currentCustomCoverPath)
                    currentTrack?.albumArtUri != null ->
                        context.contentResolver.openInputStream(currentTrack!!.albumArtUri!!)?.use { BitmapFactory.decodeStream(it) }
                    else -> null
                } ?: return@runCatching null
                val palette = Palette.from(bmp).generate()
                Color(palette.getVibrantColor(palette.getMutedColor(0xFFE8A864.toInt())))
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

    val writeRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingMetaWrite
        pendingMetaWrite = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            viewModel.saveEditedTrackMetadata(pending.first, pending.second, pending.third)
        } else if (pending != null) {
            Toast.makeText(context, "Izin ubah file asli dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestDirectFileTagEdit(track: LocalTrack, newTitle: String, newArtist: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingMetaWrite = Triple(track, newTitle, newArtist)
            val pi = MediaStore.createWriteRequest(context.contentResolver, listOf(track.contentUri))
            writeRequestLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        } else {
            viewModel.saveEditedTrackMetadata(track, newTitle, newArtist)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = pendingPhotoTarget
        pendingPhotoTarget = null
        if (target == null) return@rememberLauncherForActivityResult
        when (target) {
            is PhotoPickTarget.TrackCover -> if (uri != null) viewModel.updateTrackCustomCover(target.trackHash, uri)
            is PhotoPickTarget.PlaylistCover -> if (uri != null) viewModel.updatePlaylist(target.playlist, target.playlist.name, uri)
            is PhotoPickTarget.NewPlaylistWithCover -> viewModel.createPlaylist(target.name, uri)
        }
    }

    val importTranscriptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            if (!text.isNullOrBlank()) viewModel.saveTranscriptText(text)
        }
    }

    val exportTranscriptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val lrc = viewModel.buildTranscriptExportLrc() ?: return@launch
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(lrc) } }
        }
    }

    val exportMarkersLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val json = viewModel.buildMarkersExportJson() ?: return@launch
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) } }
        }
    }

    val importMarkersLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val json = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            if (!json.isNullOrBlank()) viewModel.importMarkersJson(json)
        }
    }

    // Full 1-File App Backup & Restore Launchers
    val exportFullBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val json = viewModel.buildFullBackupJson()
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) } }
        }
    }

    val restoreFullBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val json = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            if (!json.isNullOrBlank()) viewModel.restoreFullBackupJson(json)
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
                        Button(onClick = { permissionLauncher.launch(arrayOf(audioPermission)) }) { Text(strings.allowAudioBtn) }
                    }
                } else {
                    AnimatedContent(
                        targetState = showPlayerScreen && currentTrack != null,
                        transitionSpec = {
                            if (targetState) (slideInVertically(tween(220)) { it / 8 } + fadeIn(tween(180))) togetherWith fadeOut(tween(120))
                            else fadeIn(tween(180)) togetherWith (slideOutVertically(tween(200)) { it / 8 } + fadeOut(tween(150)))
                        },
                        label = "root_nav"
                    ) { inPlayer ->
                        if (inPlayer && currentTrack != null) {
                            val activeTrack = currentTrack!!
                            PlayerScreen(
                                track = activeTrack,
                                customCoverPath = customCovers[activeTrack.trackHash],
                                customCovers = customCovers,
                                isFavorite = activeTrack.trackHash in favoriteHashes,
                                queue = currentQueue,
                                playlists = playlists,
                                isPlaying = isPlaying,
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                abLoop = abLoop,
                                positionMsFlow = viewModel.currentPositionMs,
                                durationMs = durationMs,
                                waveformBars = waveformBars,
                                markers = markers,
                                selectedMarker = selectedMarker,
                                transcriptLines = transcriptLines,
                                effectsState = effectsState,
                                onBack = { showPlayerScreen = false },
                                onToggleFavorite = { viewModel.toggleFavoriteTrack(activeTrack.trackHash) },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onCycleRepeatMode = viewModel::cycleRepeatMode,
                                onSeekDelta = viewModel::seekByDelta,
                                onEditTrackMetadata = { editingMetadataTrack = activeTrack },
                                onCycleAbLoop = viewModel::cycleAbLoop,
                                onJumpQueueIndex = viewModel::jumpToQueueIndex,
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
                                    val safe = activeTrack.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    exportTranscriptLauncher.launch("${safe}.lrc")
                                },
                                onExportMarkersJson = {
                                    val safe = activeTrack.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    exportMarkersLauncher.launch("${safe}_markers.json")
                                },
                                onImportMarkersJson = { importMarkersLauncher.launch(arrayOf("application/json", "text/*")) }
                            )
                        } else {
                            // Main Shell: Home | Search | Playlists + Floating Wavy Pill Mini-Player
                            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (bottomTab) {
                                            0 -> {
                                                val heroTrack = currentTrack ?: recentlyPlayed.firstOrNull() ?: allVisibleTracks.firstOrNull()
                                                HomeScreen(
                                                    heroTrack = heroTrack,
                                                    waveformBars = waveformBars,
                                                    isPlaying = isPlaying,
                                                    isHeroCurrentlyPlaying = heroTrack != null && heroTrack.trackHash == currentTrack?.trackHash,
                                                    positionMsFlow = viewModel.currentPositionMs,
                                                    durationMs = durationMs,
                                                    favoriteCount = favoriteHashes.size,
                                                    totalTrackCount = allVisibleTracks.size,
                                                    recentlyPlayed = recentlyPlayed,
                                                    fallbackTracks = allVisibleTracks,
                                                    playlists = playlists,
                                                    playlistTrackMap = playlistTrackMap,
                                                    customCovers = customCovers,
                                                    onOpenSettings = { showSettingsSheet = true },
                                                    onHeroCardClick = { t ->
                                                        if (currentTrack?.trackHash != t.trackHash) viewModel.playTrack(t, allVisibleTracks)
                                                        showPlayerScreen = true
                                                    },
                                                    onHeroPlayPause = { t ->
                                                        if (currentTrack?.trackHash == t.trackHash) viewModel.togglePlayPause()
                                                        else viewModel.playTrack(t, allVisibleTracks)
                                                    },
                                                    onHeroSeek = viewModel::seekTo,
                                                    onQuickShuffleAll = {
                                                        if (allVisibleTracks.isNotEmpty()) {
                                                            val shuffled = allVisibleTracks.shuffled()
                                                            viewModel.playTrack(shuffled.first(), shuffled)
                                                            showPlayerScreen = true
                                                        }
                                                    },
                                                    onQuickOpenFavorites = {
                                                        if (!favoritesOnly) viewModel.toggleFavoritesOnly()
                                                        bottomTab = 1
                                                    },
                                                    onQuickOpenLibrary = {
                                                        if (favoritesOnly) viewModel.toggleFavoritesOnly()
                                                        bottomTab = 1
                                                    },
                                                    onTrackClick = { t, q ->
                                                        viewModel.playTrack(t, q)
                                                        showPlayerScreen = true
                                                    },
                                                    onPlaylistClick = { pl ->
                                                        openedPlaylistId = pl.id
                                                        bottomTab = 2
                                                    },
                                                    onCreatePlaylistClick = {
                                                        openedPlaylistId = null
                                                        bottomTab = 2
                                                    }
                                                )
                                            }
                                            1 -> SearchLibraryScreen(
                                                filteredTracks = filteredTracks,
                                                folders = visibleFolders,
                                                selectedFolder = selectedFolder,
                                                favoritesOnly = favoritesOnly,
                                                favoriteHashes = favoriteHashes,
                                                sortMode = filterConfig.sortMode,
                                                searchQuery = searchQuery,
                                                isScanning = isScanning,
                                                customCovers = customCovers,
                                                playlists = playlists,
                                                currentTrack = currentTrack,
                                                isPlaying = isPlaying,
                                                onSearchChange = viewModel::updateSearchQuery,
                                                onFolderSelect = viewModel::selectFolder,
                                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                                onSortModeSelect = viewModel::setSortMode,
                                                onToggleFavoriteTrack = viewModel::toggleFavoriteTrack,
                                                onTrackClick = { t, q ->
                                                    viewModel.playTrack(t, q)
                                                    showPlayerScreen = true
                                                },
                                                onEditTrackMetadata = { t -> editingMetadataTrack = t },
                                                onPickTrackCover = { t ->
                                                    pendingPhotoTarget = PhotoPickTarget.TrackCover(t.trackHash)
                                                    photoPickerLauncher.launch("image/*")
                                                },
                                                onResetTrackCover = { t -> viewModel.resetTrackCustomCover(t.trackHash) },
                                                onAddTrackToPlaylist = { pl, t -> viewModel.addTrackToPlaylist(pl, t) }
                                            )
                                            else -> PlaylistsScreen(
                                                allTracks = allVisibleTracks,
                                                playlists = playlists,
                                                playlistTrackMap = playlistTrackMap,
                                                openedPlaylistId = openedPlaylistId,
                                                customCovers = customCovers,
                                                currentTrack = currentTrack,
                                                isPlaying = isPlaying,
                                                onOpenPlaylist = { openedPlaylistId = it },
                                                onCreatePlaylist = { name -> viewModel.createPlaylist(name, null) },
                                                onCreatePlaylistWithPhoto = { name ->
                                                    pendingPhotoTarget = PhotoPickTarget.NewPlaylistWithCover(name)
                                                    photoPickerLauncher.launch("image/*")
                                                },
                                                onRenamePlaylist = { pl, name -> viewModel.updatePlaylist(pl, name, null) },
                                                onPickPlaylistCover = { pl ->
                                                    pendingPhotoTarget = PhotoPickTarget.PlaylistCover(pl)
                                                    photoPickerLauncher.launch("image/*")
                                                },
                                                onDeletePlaylist = viewModel::deletePlaylist,
                                                onTrackClick = { t, q ->
                                                    viewModel.playTrack(t, q)
                                                    showPlayerScreen = true
                                                },
                                                onShufflePlaylist = { list ->
                                                    val shuffled = list.shuffled()
                                                    shuffled.firstOrNull()?.let {
                                                        viewModel.playTrack(it, shuffled)
                                                        showPlayerScreen = true
                                                    }
                                                },
                                                onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist
                                            )
                                        }
                                    }

                                    // Bottom Navigation Bar: Home | Search | Playlists
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.navigationBarsPadding()
                                    ) {
                                        val itemColors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        NavigationBarItem(
                                            selected = bottomTab == 0,
                                            onClick = { bottomTab = 0 },
                                            icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                                            label = { Text("Home") },
                                            colors = itemColors
                                        )
                                        NavigationBarItem(
                                            selected = bottomTab == 1,
                                            onClick = { bottomTab = 1 },
                                            icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                                            label = { Text("Search") },
                                            colors = itemColors
                                        )
                                        NavigationBarItem(
                                            selected = bottomTab == 2,
                                            onClick = { bottomTab = 2 },
                                            icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Playlists") },
                                            label = { Text(strings.playlists) },
                                            colors = itemColors
                                        )
                                    }
                                }

                                // Floating Wavy Circular Pill Mini-Player tepat di atas Bottom Nav!
                                if (currentTrack != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding()
                                            .padding(bottom = 86.dp, start = 14.dp, end = 14.dp)
                                    ) {
                                        WavyFloatingMiniPlayer(
                                            track = currentTrack!!,
                                            customCoverPath = customCovers[currentTrack!!.trackHash],
                                            isPlaying = isPlaying,
                                            positionMsFlow = viewModel.currentPositionMs,
                                            durationMs = durationMs,
                                            onOpenPlayer = { showPlayerScreen = true },
                                            onPrev = viewModel::skipPrevious,
                                            onPlayPause = viewModel::togglePlayPause,
                                            onNext = viewModel::skipNext
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSettingsSheet) {
                    SettingsSheet(
                        themePreset = themePreset,
                        language = language,
                        filterConfig = filterConfig,
                        allFolders = allAvailableFolders,
                        onOpenThemePicker = { showThemePicker = true },
                        onOpenLanguagePicker = { showLanguagePicker = true },
                        onMinDurationSelect = viewModel::setMinDurationSec,
                        onToggleHideFolder = viewModel::toggleHiddenFolder,
                        onExportFullBackup = { exportFullBackupLauncher.launch("notara_full_backup.json") },
                        onRestoreFullBackup = { restoreFullBackupLauncher.launch(arrayOf("application/json", "text/*")) },
                        onRescanLibrary = {
                            Toast.makeText(context, strings.rescanStarted, Toast.LENGTH_SHORT).show()
                            viewModel.rescanLibrary()
                        },
                        onDismiss = { showSettingsSheet = false }
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

                // Dialog Edit Judul & Artis (tersimpan langsung ke file asli MP3 + MediaStore!)
                val metaTrack = editingMetadataTrack
                if (metaTrack != null) {
                    var titleInput by remember(metaTrack) { mutableStateOf(metaTrack.title) }
                    var artistInput by remember(metaTrack) { mutableStateOf(metaTrack.artist) }
                    AlertDialog(
                        onDismissRequest = { editingMetadataTrack = null },
                        title = { Text("Edit Judul & Artis (File Asli)") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Perubahan akan ditulis langsung ke tag file audio asli dan MediaStore Android.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = titleInput,
                                    onValueChange = { titleInput = it },
                                    label = { Text("Judul Lagu") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = artistInput,
                                    onValueChange = { artistInput = it },
                                    label = { Text("Nama Artis") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                editingMetadataTrack = null
                                requestDirectFileTagEdit(metaTrack, titleInput, artistInput)
                            }) { Text(strings.saveBtn) }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingMetadataTrack = null }) { Text(strings.cancelBtn) }
                        }
                    )
                }

                val dup = duplicateAlert
                if (dup != null) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissDuplicateAlert,
                        title = { Text(strings.duplicateTitle) },
                        text = { Text(strings.duplicateBody(dup.trackTitle, dup.playlistName)) },
                        confirmButton = { Button(onClick = viewModel::dismissDuplicateAlert) { Text(strings.okBtn) } }
                    )
                }
            }
        }
    }
}

/**
 * Floating Pill Mini-Player ala foto referensi:
 * Lingkaran sampul + WavyCircularProgressRing di kiri, judul & artis di tengah,
 * serta 3 tombol bulat (Prev, Play/Pause solid, Next) di kanan.
 */
@Composable
private fun WavyFloatingMiniPlayer(
    track: LocalTrack,
    customCoverPath: String?,
    isPlaying: Boolean,
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    onOpenPlayer: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(32.dp))
            .clickable(onClick = onOpenPlayer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WavyCircularProgressRing(
                positionMsFlow = positionMsFlow,
                durationMs = durationMs,
                isPlaying = isPlaying,
                ringSize = 52.dp
            ) {
                CozyArtwork(
                    customCoverPath = customCoverPath,
                    albumArtUri = track.albumArtUri,
                    seedKey = track.trackHash,
                    sizeDp = 40,
                    cornerDp = 20
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                    modifier = Modifier.size(34.dp).clickable(onClick = onPrev)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp).clickable(onClick = onPlayPause)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                    modifier = Modifier.size(34.dp).clickable(onClick = onNext)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
