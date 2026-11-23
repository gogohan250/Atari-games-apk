package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.components.AtariBottomBar
import com.example.ui.components.MainTab
import com.example.ui.components.RomImportOptionsDialog
import com.example.ui.components.RomImportProgressDialog
import com.example.ui.components.TelegramSubscribeStartupDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AppThemeStyle
import com.example.ui.theme.AtariBackground
import com.example.ui.theme.AtariClassicsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingIntentRomUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractIncomingRomUri(intent)?.let { pendingIntentRomUri.value = it }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val themeStyle = runCatching { AppThemeStyle.valueOf(settings.themeStyle) }
                .getOrDefault(AppThemeStyle.DARK)

            AtariClassicsTheme(
                themeStyle = themeStyle,
                isArabic = settings.isArabic
            ) {
                AtariClassicsRoot(
                    viewModel = viewModel,
                    pendingRomUri = pendingIntentRomUri.value,
                    onConsumedPendingUri = { pendingIntentRomUri.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingRomUri(intent)?.let { pendingIntentRomUri.value = it }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewModel.gamepadHandler.handleKeyEvent(
            ev = event,
            mapping = viewModel.settings.value.gamepadMapping,
            emulator = viewModel.emulator,
            isPlayerActive = viewModel.isPlayerActive.value,
            onQuickAction = viewModel::handleGamepadQuickAction
        )
        return if (handled) true else super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val handled = viewModel.gamepadHandler.handleMotionEvent(
            ev = ev,
            mapping = viewModel.settings.value.gamepadMapping,
            emulator = viewModel.emulator,
            isPlayerActive = viewModel.isPlayerActive.value,
            onQuickAction = viewModel::handleGamepadQuickAction
        )
        return if (handled) true else super.dispatchGenericMotionEvent(ev)
    }

    private fun extractIncomingRomUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
    }
}

@Composable
private fun AtariClassicsRoot(
    viewModel: MainViewModel,
    pendingRomUri: Uri?,
    onConsumedPendingUri: () -> Unit
) {
    val initProgress by viewModel.initProgress.collectAsStateWithLifecycle()
    val splashFinished by viewModel.splashFinished.collectAsStateWithLifecycle()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalGamesCount.collectAsStateWithLifecycle()
    val lastPlayed by viewModel.lastPlayedGame.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAddedGames.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayedGames.collectAsStateWithLifecycle()
    val filteredGames by viewModel.filteredGames.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteGames.collectAsStateWithLifecycle()
    val genres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val years by viewModel.availableYears.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSystem by viewModel.selectedSystem.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val activeSaveSlots by viewModel.activeSaveSlots.collectAsStateWithLifecycle()
    val activeCheats by viewModel.activeCheats.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val connectedGamepadName by viewModel.gamepadHandler.connectedGamepadName.collectAsStateWithLifecycle()
    val romImportProgress by viewModel.romImportProgress.collectAsStateWithLifecycle()
    var showTelegramPromptThisSession by remember { mutableStateOf(true) }
    var showImportOptionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        viewModel.isPlayerActive.value = currentRoute == "player"
    }

    val multiRomPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleRoms(uris) {
                navController.navigate("player")
            }
        }
    }

    val folderRomPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.importRomFolder(uri)
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Handle external "Open with" / "Share" ROM file intents automatically
    LaunchedEffect(pendingRomUri, splashFinished) {
        if (pendingRomUri != null && splashFinished) {
            viewModel.importRomUri(pendingRomUri, autoLaunch = true) {
                navController.navigate("player")
            }
            onConsumedPendingUri()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!splashFinished) {
            SplashScreen(progress = initProgress)
        } else {
            val isPlayerRoute = currentRoute == "player"
            val activeDownloadCount = downloads.count {
                it.status == "DOWNLOADING" || it.status == "QUEUED"
            }

            Scaffold(
                containerColor = AtariBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isPlayerRoute,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        AtariBottomBar(
                            currentRoute = currentRoute,
                            activeDownloadCount = activeDownloadCount,
                            onNavigate = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(MainTab.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = MainTab.HOME.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(MainTab.HOME.route) {
                        HomeScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            lastPlayedGame = lastPlayed,
                            recentlyAdded = recentlyAdded,
                            mostPlayed = mostPlayed,
                            searchResults = filteredGames,
                            onPlayGame = { game ->
                                viewModel.launchGame(game) {
                                    navController.navigate("player")
                                }
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onImportRomClick = {
                                showImportOptionsDialog = true
                            },
                            onOpenLibrary = {
                                navController.navigate(MainTab.LIBRARY.route) {
                                    popUpTo(MainTab.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(MainTab.LIBRARY.route) {
                        LibraryScreen(
                            games = filteredGames,
                            totalCount = totalCount,
                            searchQuery = searchQuery,
                            selectedSystem = selectedSystem,
                            selectedGenre = selectedGenre,
                            selectedYear = selectedYear,
                            sortMode = sortMode,
                            genres = genres,
                            years = years,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onSystemSelect = viewModel::setSelectedSystem,
                            onGenreSelect = viewModel::setSelectedGenre,
                            onYearSelect = viewModel::setSelectedYear,
                            onSortChange = viewModel::setSortMode,
                            onPlayGame = { game ->
                                viewModel.launchGame(game) {
                                    navController.navigate("player")
                                }
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onImportRomClick = {
                                showImportOptionsDialog = true
                            }
                        )
                    }

                    composable(MainTab.FAVORITES.route) {
                        FavoritesScreen(
                            favorites = favorites,
                            onPlayGame = { game ->
                                viewModel.launchGame(game) {
                                    navController.navigate("player")
                                }
                            },
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }

                    composable(MainTab.BROWSER.route) {
                        BrowserScreen(
                            downloads = downloads,
                            onStartDownload = { url, disp ->
                                viewModel.downloadManager.enqueueDownload(url, disp)
                            },
                            onPauseDownload = viewModel.downloadManager::pauseDownload,
                            onResumeDownload = viewModel.downloadManager::resumeDownload,
                            onDeleteDownload = viewModel.downloadManager::deleteDownload,
                            onPlayDownloadedGame = { gameId ->
                                scope.launch {
                                    val g = viewModel.repository.gameDao.getById(gameId)
                                    if (g != null) {
                                        viewModel.launchGame(g) {
                                            navController.navigate("player")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable(MainTab.SETTINGS.route) {
                        SettingsScreen(
                            settings = settings,
                            connectedGamepadName = connectedGamepadName,
                            gamepadHandler = viewModel.gamepadHandler,
                            onUpdateSettings = viewModel::updateSettings,
                            onScanWatchedNow = viewModel::scanWatchedFolderNow,
                            onBackupSaves = viewModel::backupSaves,
                            onRestoreSaves = viewModel::restoreSaves,
                            onClearCache = viewModel::clearCache
                        )
                    }

                    composable("player") {
                        val game = activeGame
                        if (game != null) {
                            PlayerScreen(
                                game = game,
                                emulator = viewModel.emulator,
                                settings = settings,
                                connectedGamepadName = connectedGamepadName,
                                gamepadHandler = viewModel.gamepadHandler,
                                saveSlots = activeSaveSlots,
                                cheats = activeCheats,
                                onUpdateSettings = viewModel::updateSettings,
                                onAddCheat = viewModel::addCheat,
                                onToggleCheat = viewModel::toggleCheat,
                                onDeleteCheat = viewModel::deleteCheat,
                                onToast = viewModel::postToast,
                                onExit = {
                                    viewModel.stopEmulation()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }

            if (showImportOptionsDialog) {
                RomImportOptionsDialog(
                    isArabic = settings.isArabic,
                    onDismiss = { showImportOptionsDialog = false },
                    onSelectMultipleFiles = { multiRomPickerLauncher.launch(arrayOf("*/*")) },
                    onSelectFolder = { folderRomPickerLauncher.launch(null) }
                )
            }

            romImportProgress?.let { progress ->
                RomImportProgressDialog(
                    progress = progress,
                    isArabic = settings.isArabic
                )
            }

            if (showTelegramPromptThisSession && !settings.telegramSubscribed && !isPlayerRoute) {
                TelegramSubscribeStartupDialog(
                    isArabic = settings.isArabic,
                    onDismissSession = { showTelegramPromptThisSession = false },
                    onMarkSubscribed = {
                        viewModel.updateSettings { it.copy(telegramSubscribed = true) }
                        showTelegramPromptThisSession = false
                    }
                )
            }
        }
    }
}
