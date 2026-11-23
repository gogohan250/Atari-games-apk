package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AtariClassicsApp
import com.example.browser.RomDownloadManager
import com.example.data.model.CheatEntity
import com.example.data.model.DownloadTaskEntity
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import com.example.data.prefs.AtariSettings
import com.example.emulator.EmulatorController
import com.example.emulator.GamepadInputHandler
import com.example.emulator.GamepadQuickAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LibrarySortMode { TITLE, MOST_PLAYED, NEWEST }

data class RomImportProgress(
    val processed: Int,
    val total: Int,
    val importedCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AtariClassicsApp
    val repository = app.repository
    val settingsStore = app.settingsDataStore

    val emulator = EmulatorController(application, repository, viewModelScope)
    val downloadManager = RomDownloadManager(application, repository, viewModelScope)
    val gamepadHandler = GamepadInputHandler(application)

    val isPlayerActive = MutableStateFlow(false)

    private val _initProgress = MutableStateFlow(0f)
    val initProgress: StateFlow<Float> = _initProgress.asStateFlow()

    private val _splashFinished = MutableStateFlow(false)
    val splashFinished: StateFlow<Boolean> = _splashFinished.asStateFlow()

    val settings: StateFlow<AtariSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AtariSettings())

    val totalGamesCount: StateFlow<Int> = repository.gameDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastPlayedGame: StateFlow<GameEntity?> = repository.gameDao.observeLastPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentlyAddedGames: StateFlow<List<GameEntity>> = repository.gameDao.observeRecentlyAdded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedGames: StateFlow<List<GameEntity>> = repository.gameDao.observeMostPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteGames: StateFlow<List<GameEntity>> = repository.gameDao.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableGenres: StateFlow<List<String>> = repository.gameDao.observeGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableYears: StateFlow<List<Int>> = repository.gameDao.observeYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadTaskEntity>> = repository.downloadDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Library Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSystem = MutableStateFlow("")
    val selectedSystem: StateFlow<String> = _selectedSystem.asStateFlow()

    private val _selectedGenre = MutableStateFlow("")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _selectedYear = MutableStateFlow(0)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _sortMode = MutableStateFlow(LibrarySortMode.MOST_PLAYED)
    val sortMode: StateFlow<LibrarySortMode> = _sortMode.asStateFlow()

    val filteredGames: StateFlow<List<GameEntity>> = combine(
        _selectedSystem,
        _selectedGenre,
        _selectedYear,
        _searchQuery,
        _sortMode
    ) { sys, genre, year, query, sort ->
        FilterParams(sys, genre, year, query.trim(), sort.name)
    }.flatMapLatest { p ->
        repository.gameDao.observeFilteredGames(
            system = p.system,
            genre = p.genre,
            year = p.year,
            query = p.query,
            sortMode = p.sortMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Player Game
    private val _activeGame = MutableStateFlow<GameEntity?>(null)
    val activeGame: StateFlow<GameEntity?> = _activeGame.asStateFlow()

    val activeSaveSlots: StateFlow<List<SaveStateEntity>> = _activeGame.flatMapLatest { g ->
        if (g == null) flowOf(emptyList()) else repository.saveStateDao.observeForGame(g.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCheats: StateFlow<List<CheatEntity>> = _activeGame.flatMapLatest { g ->
        if (g == null) flowOf(emptyList()) else repository.cheatDao.observeForGame(g.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _romImportProgress = MutableStateFlow<RomImportProgress?>(null)
    val romImportProgress: StateFlow<RomImportProgress?> = _romImportProgress.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureCatalogLoaded().collect { progress ->
                _initProgress.value = progress
            }
            delay(500L)
            _splashFinished.value = true
        }

        // Sync audio, performance, and 2-player settings to emulator engine
        viewModelScope.launch {
            settings.collect { s ->
                emulator.setAudioConfig(s.audioEnabled, s.audioVolume)
                emulator.setPerformanceMode(s.performanceMode)
                emulator.setTwoPlayerMode(s.twoPlayerMode)
            }
        }

        // Optional Downloads SAF folder watcher loop
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val s = settings.value
                if (s.watchDownloadsFolder && s.watchedTreeUri.isNotBlank()) {
                    val count = repository.scanWatchedSafTree(s.watchedTreeUri)
                    if (count > 0) {
                        postToast("تم استيراد $count لعبة تلقائياً من مجلد التحميلات")
                    }
                }
                delay(15_000L)
            }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSelectedSystem(s: String) { _selectedSystem.value = s }
    fun setSelectedGenre(g: String) { _selectedGenre.value = g }
    fun setSelectedYear(y: Int) { _selectedYear.value = y }
    fun setSortMode(m: LibrarySortMode) { _sortMode.value = m }

    fun postToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleFavorite(gameId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.gameDao.toggleFavorite(gameId)
        }
    }

    fun launchGame(game: GameEntity, onReady: () -> Unit) {
        viewModelScope.launch {
            val bytes = repository.getRomBytes(game)
            if (bytes != null) {
                _activeGame.value = game
                emulator.startGame(game, bytes, autoLoadSave = settings.value.autoSaveEnabled)
                onReady()
            } else {
                postToast("ملف ROM غير موجود لهذه اللعبة — قم باستيراده أو تحميله من المتصفح")
            }
        }
    }

    fun stopEmulation() {
        emulator.stop(autoSave = settings.value.autoSaveEnabled)
    }

    fun importRomUri(uri: Uri, autoLaunch: Boolean = false, onLaunched: () -> Unit = {}) {
        viewModelScope.launch {
            val imported = repository.importRomFromUri(uri)
            if (imported != null) {
                postToast("تمت إضافة ${imported.title} (${imported.system})")
                if (autoLaunch) {
                    launchGame(imported, onLaunched)
                }
            } else {
                postToast("تعذّر قراءة ملف ROM أو صيغته غير مدعومة")
            }
        }
    }

    fun importMultipleRoms(uris: List<Uri>, onSingleAutoLaunch: () -> Unit = {}) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _romImportProgress.value = RomImportProgress(processed = 0, total = uris.size, importedCount = 0)
            try {
                val imported = repository.importMultipleRoms(uris) { imp, proc, total ->
                    _romImportProgress.value = RomImportProgress(processed = proc, total = total, importedCount = imp)
                }
                if (imported.size == 1 && uris.size == 1) {
                    val one = imported.first()
                    postToast("تمت إضافة ${one.title} (${one.system})")
                    launchGame(one, onSingleAutoLaunch)
                } else if (imported.isNotEmpty()) {
                    _sortMode.value = LibrarySortMode.NEWEST
                    postToast("تمت إضافة ${imported.size} لعبة بنجاح")
                } else {
                    postToast("تعذّر قراءة ملفات ROM أو صيغتها غير مدعومة")
                }
            } finally {
                _romImportProgress.value = null
            }
        }
    }

    fun importRomFolder(treeUri: Uri) {
        viewModelScope.launch {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            _romImportProgress.value = RomImportProgress(processed = 0, total = 0, importedCount = 0)
            try {
                val count = repository.importRomFolder(treeUri) { imp, proc, total ->
                    _romImportProgress.value = RomImportProgress(processed = proc, total = total, importedCount = imp)
                }
                if (count > 0) {
                    _sortMode.value = LibrarySortMode.NEWEST
                    postToast("تم استيراد $count لعبة من المجلد بنجاح")
                } else {
                    postToast("لم يتم العثور على ألعاب جديدة صالحة في المجلد")
                }
            } finally {
                _romImportProgress.value = null
            }
        }
    }

    fun addCheat(name: String, code: String) {
        val g = _activeGame.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = code.trim().uppercase()
            repository.cheatDao.insert(
                CheatEntity(
                    id = "${g.id}_${System.currentTimeMillis()}",
                    gameId = g.id,
                    name = name.trim().ifBlank { "Cheat" },
                    code = cleanCode,
                    enabled = true
                )
            )
            emulator.refreshCheats()
        }
    }

    fun toggleCheat(id: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cheatDao.setEnabled(id, enabled)
            emulator.refreshCheats()
        }
    }

    fun deleteCheat(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cheatDao.deleteById(id)
            emulator.refreshCheats()
        }
    }

    fun updateSettings(transform: (AtariSettings) -> AtariSettings) {
        viewModelScope.launch {
            settingsStore.update(transform)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freedBytes = repository.clearImageCache()
            postToast("تم مسح الكاش (${freedBytes / 1024} KB)")
        }
    }

    fun backupSaves(uri: Uri) {
        viewModelScope.launch {
            val ok = repository.exportBackupToUri(uri)
            postToast(if (ok) "تم تصدير النسخة الاحتياطية بنجاح" else "فشل تصدير النسخة الاحتياطية")
        }
    }

    fun restoreSaves(uri: Uri) {
        viewModelScope.launch {
            val ok = repository.restoreBackupFromUri(uri)
            postToast(if (ok) "تمت استعادة الحفظ بنجاح" else "ملف النسخة الاحتياطية غير صالح")
        }
    }

    fun handleGamepadQuickAction(action: GamepadQuickAction) {
        when (action) {
            GamepadQuickAction.QUICK_SAVE -> viewModelScope.launch {
                val ok = emulator.saveToSlot(1)
                postToast(if (ok) "تم الحفظ السريع في الخانة 1" else "تعذر الحفظ")
            }
            GamepadQuickAction.QUICK_LOAD -> viewModelScope.launch {
                val ok = emulator.loadFromSlot(1)
                postToast(if (ok) "تم التحميل السريع من الخانة 1" else "الخانة 1 فارغة")
            }
            GamepadQuickAction.FAST_FORWARD_START -> emulator.setFastForwardHold(true)
            GamepadQuickAction.FAST_FORWARD_STOP -> emulator.setFastForwardHold(false)
            GamepadQuickAction.REWIND -> {
                if (!emulator.rewindStep()) postToast("لا توجد إطارات للرجوع")
            }
        }
    }

    fun scanWatchedFolderNow() {
        val tree = settings.value.watchedTreeUri
        if (tree.isBlank()) {
            postToast("اختر مجلد التحميلات أولاً")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.scanWatchedSafTree(tree)
            postToast(if (count > 0) "تم استيراد $count لعبة جديدة" else "لا توجد ألعاب جديدة في المجلد")
        }
    }

    override fun onCleared() {
        gamepadHandler.release()
        emulator.stop(autoSave = false)
        super.onCleared()
    }

    private data class FilterParams(
        val system: String,
        val genre: String,
        val year: Int,
        val query: String,
        val sortMode: String
    )
}
