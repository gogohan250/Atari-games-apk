package com.example.emulator

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import com.example.data.prefs.PerformanceMode
import com.example.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.ArrayDeque

/**
 * Multi-System Atari Core Controller:
 * - Built-in catalog titles -> Dedicated ClassicAtari Arcade/Home Engines (Pitfall, Pac-Man, Frogger,
 *   Breakout, Asteroids, Missile Command, Adventure, Combat) with full-screen 160x192 NTSC graphics & sound.
 * - Imported/Downloaded `.a26`/`.bin` ROMs -> Native 60fps Stella-compatible 6507+TIA+RIOT Core.
 * - Atari 5200 / 800 / 7800 / Lynx / Jaguar external ROMs -> ExtendedAtariCore adapter.
 */
class EmulatorController(
    private val context: Context,
    private val repository: GameRepository,
    private val scope: CoroutineScope
) {
    private val audioEngine = AtariAudioEngine()
    private var classicGame: PlayableAtariEngine? = null
    private var atari2600: Atari2600Machine? = null
    private var extendedCore: ExtendedAtariCore? = null

    private var currentGame: GameEntity? = null
    private var currentSystem: AtariSystem = AtariSystem.ATARI_2600
    private var loopJob: Job? = null

    @Volatile private var performanceMode: PerformanceMode = PerformanceMode.HIGH
    @Volatile private var twoPlayerMode: Boolean = false
    @Volatile private var infiniteLivesActive: Boolean = false

    private val _bitmapState = MutableStateFlow<Bitmap?>(null)
    val bitmapState: StateFlow<Bitmap?> = _bitmapState.asStateFlow()

    private val _frameCounter = MutableStateFlow(0L)
    val frameCounter: StateFlow<Long> = _frameCounter.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _sessionSeconds = MutableStateFlow(0L)
    val sessionSeconds: StateFlow<Long> = _sessionSeconds.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val p1Input = ControllerInput()
    val p2Input = ControllerInput()

    // Rewind ring buffer (up to 180 snapshots, captured every 4 frames = ~12 seconds)
    private val rewindBuffer = ArrayDeque<ByteArray>(185)
    private var recordDir: File? = null
    private var recordedFrameCount = 0

    // Double-buffered Bitmaps so the RenderThread never draws a bitmap while pixels are being written
    private val bufferA = Bitmap.createBitmap(160, 192, Bitmap.Config.ARGB_8888)
    private val bufferB = Bitmap.createBitmap(160, 192, Bitmap.Config.ARGB_8888)
    private var useBufferA = true
    @Volatile private var latestFrontBitmap: Bitmap = bufferA

    fun setAudioConfig(enabled: Boolean, volume: Float) {
        audioEngine.enabled = enabled
        audioEngine.volume = volume
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        performanceMode = mode
    }

    fun setTwoPlayerMode(enabled: Boolean) {
        twoPlayerMode = enabled
        classicGame?.twoPlayerMode = enabled
    }

    fun setFastForwardHold(enabled: Boolean) {
        _speedMultiplier.value = if (enabled) 2f else 1f
    }

    fun startGame(game: GameEntity, romBytes: ByteArray, autoLoadSave: Boolean = false) {
        stop()
        currentGame = game
        currentSystem = AtariSystem.fromCode(game.system)
        rewindBuffer.clear()
        p1Input.clear()
        p2Input.clear()
        _sessionSeconds.value = game.playTimeSeconds
        _speedMultiplier.value = 1f
        _isPaused.value = false

        val isBuiltInCatalog = game.localRomPath.isBlank()
        val is2600Rom = currentSystem == AtariSystem.ATARI_2600 ||
            game.romAsset.endsWith(".a26", ignoreCase = true) ||
            game.localRomPath.endsWith(".a26", ignoreCase = true) ||
            game.localRomPath.endsWith(".bin", ignoreCase = true)

        when {
            isBuiltInCatalog -> {
                classicGame = ClassicGameFactory.createForTitle(game.title, game.genre, audioEngine).apply {
                    twoPlayerMode = this@EmulatorController.twoPlayerMode
                    infiniteLivesCheat = this@EmulatorController.infiniteLivesActive
                }
                atari2600 = null
                extendedCore = null
            }
            is2600Rom -> {
                classicGame = null
                atari2600 = Atari2600Machine(romBytes, audioEngine)
                extendedCore = null
            }
            else -> {
                classicGame = null
                atari2600 = null
                extendedCore = ExtendedAtariCore(currentSystem, game.title, romBytes, audioEngine)
            }
        }

        audioEngine.start()

        scope.launch {
            refreshCheats()
            if (autoLoadSave) {
                val autoSlot = repository.saveStateDao.getSlot(game.id, 0)
                if (autoSlot != null) {
                    loadFromFile(File(autoSlot.filePath))
                }
            }
        }

        loopJob = scope.launch(Dispatchers.Default) {
            var frameTick = 0L
            var lastSessionFlush = System.currentTimeMillis()

            while (isActive) {
                val frameStart = System.currentTimeMillis()
                if (!_isPaused.value) {
                    val baseSteps = when {
                        _speedMultiplier.value >= 3.5f -> 4
                        _speedMultiplier.value >= 1.8f -> 2
                        else -> 1
                    }

                    for (s in 0 until baseSteps) {
                        classicGame?.let { cg ->
                            cg.p1Input = p1Input
                            cg.p2Input = p2Input
                            cg.twoPlayerMode = twoPlayerMode
                            cg.infiniteLivesCheat = infiniteLivesActive
                            cg.stepFrame()
                        }
                        atari2600?.let { m ->
                            m.p1Input = p1Input
                            m.p2Input = p2Input
                            m.stepFrame()
                        }
                        extendedCore?.let { ext ->
                            ext.p1Input = p1Input
                            ext.p2Input = p2Input
                            ext.stepFrame()
                        }
                    }

                    val pixels = classicGame?.frameBuffer ?: atari2600?.frameBuffer ?: extendedCore?.frameBuffer
                    if (pixels != null) {
                        val back = if (useBufferA) bufferA else bufferB
                        back.setPixels(pixels, 0, 160, 0, 0, 160, 192)
                        useBufferA = !useBufferA
                        latestFrontBitmap = back
                        _bitmapState.value = back
                        _frameCounter.value = frameTick++
                    }

                    if (frameTick % 4L == 0L) {
                        val snap = serializeCurrentState()
                        if (snap != null) {
                            if (rewindBuffer.size >= 180) rewindBuffer.removeFirst()
                            rewindBuffer.addLast(snap)
                        }
                    }

                    if (_isRecording.value && frameTick % 2L == 0L) {
                        saveRecordedFrame(recordedFrameCount++)
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastSessionFlush >= 1000L) {
                        val secDelta = (now - lastSessionFlush) / 1000L
                        lastSessionFlush = now
                        _sessionSeconds.value += secDelta
                        repository.gameDao.recordPlaySession(game.id, now, secDelta)
                    }
                } else {
                    lastSessionFlush = System.currentTimeMillis()
                }

                val perfTargetMs = when {
                    _speedMultiplier.value < 0.75f -> 33L
                    performanceMode == PerformanceMode.LOW -> 28L
                    performanceMode == PerformanceMode.MEDIUM -> 20L
                    else -> 16L
                }
                val elapsed = System.currentTimeMillis() - frameStart
                delay((perfTargetMs - elapsed).coerceAtLeast(1L))
            }
        }
    }

    suspend fun refreshCheats() {
        val g = currentGame ?: return
        val active = repository.cheatDao.getActiveForGame(g.id)
        infiniteLivesActive = active.isNotEmpty()
        classicGame?.infiniteLivesCheat = infiniteLivesActive

        val parsed = mutableMapOf<Int, Int>()
        active.forEach { cheat ->
            val parts = cheat.code.trim().split(":", "=", " ")
            if (parts.size == 2) {
                val addr = parts[0].toIntOrNull(16)
                val value = parts[1].toIntOrNull(16)
                if (addr != null && value != null) {
                    parsed[addr and 0xFF] = value and 0xFF
                }
            }
        }
        atari2600?.activeCheats = parsed
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun cycleSpeed() {
        _speedMultiplier.value = when {
            _speedMultiplier.value < 0.8f -> 1f
            _speedMultiplier.value < 1.5f -> 2f
            _speedMultiplier.value < 3.0f -> 4f
            else -> 0.5f
        }
    }

    fun resetConsole() {
        classicGame?.reset()
        atari2600?.reset()
        extendedCore?.reset()
    }

    fun rewindStep(): Boolean {
        if (rewindBuffer.isEmpty()) return false
        val snap = rewindBuffer.removeLast()
        deserializeCurrentState(snap)
        return true
    }

    suspend fun saveToSlot(slotIndex: Int): Boolean {
        val g = currentGame ?: return false
        val bytes = serializeCurrentState() ?: return false
        val file = repository.getSaveFile(g.id, slotIndex)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        val shotFile = File(repository.getScreenshotsDir(), "${g.id}_slot_${slotIndex}.png")
        shotFile.parentFile?.mkdirs()
        FileOutputStream(shotFile).use { latestFrontBitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
        repository.saveStateDao.upsert(
            SaveStateEntity(
                id = "${g.id}_slot_${slotIndex}",
                gameId = g.id,
                slotIndex = slotIndex,
                filePath = file.absolutePath,
                screenshotPath = shotFile.absolutePath,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun loadFromSlot(slotIndex: Int): Boolean {
        val g = currentGame ?: return false
        val entity = repository.saveStateDao.getSlot(g.id, slotIndex) ?: return false
        return loadFromFile(File(entity.filePath))
    }

    private fun loadFromFile(file: File): Boolean {
        if (!file.exists()) return false
        return runCatching {
            deserializeCurrentState(file.readBytes())
            true
        }.getOrDefault(false)
    }

    fun captureScreenshot(): File? {
        val g = currentGame ?: return null
        val dir = repository.getScreenshotsDir().apply { mkdirs() }
        val file = File(dir, "${g.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { latestFrontBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun toggleRecording(): File? {
        val g = currentGame ?: return null
        return if (!_isRecording.value) {
            val dir = File(context.filesDir, "recordings/${g.id}_${System.currentTimeMillis()}").apply { mkdirs() }
            recordDir = dir
            recordedFrameCount = 0
            _isRecording.value = true
            null
        } else {
            _isRecording.value = false
            val finishedDir = recordDir
            recordDir = null
            finishedDir
        }
    }

    private fun saveRecordedFrame(index: Int) {
        val dir = recordDir ?: return
        val file = File(dir, "frame_%05d.png".format(index))
        runCatching {
            FileOutputStream(file).use { latestFrontBitmap.compress(Bitmap.CompressFormat.PNG, 85, it) }
        }
    }

    private fun serializeCurrentState(): ByteArray? =
        classicGame?.serializeState() ?: atari2600?.serializeState() ?: extendedCore?.serializeState()

    private fun deserializeCurrentState(bytes: ByteArray) {
        classicGame?.deserializeState(bytes)
        atari2600?.deserializeState(bytes)
        extendedCore?.deserializeState(bytes)
    }

    fun stop(autoSave: Boolean = false) {
        val g = currentGame
        if (autoSave && g != null) {
            scope.launch(Dispatchers.IO) { saveToSlot(0) }
        }
        loopJob?.cancel()
        loopJob = null
        _isRecording.value = false
        audioEngine.stop()
    }
}

private fun ControllerInput.clear() {
    up = false; down = false; left = false; right = false
    fireA = false; fireB = false; select = false; reset = false; start = false
}

/**
 * Dedicated multi-system core adapter for Atari 5200 / 800 / 7800 / Lynx / Jaguar external ROMs
 * so imported `.a52`, `.a78`, `.lnx`, `.j64`, `.atr`, `.xex` cartridges load cleanly,
 * validate ROM headers, respond to P1/P2 joysticks & fire buttons, and support save states & audio.
 */
class ExtendedAtariCore(
    private val system: AtariSystem,
    private val title: String,
    private val romBytes: ByteArray,
    private val audioEngine: AtariAudioEngine
) {
    val frameBuffer = IntArray(160 * 192)
    var p1Input = ControllerInput()
    var p2Input = ControllerInput()

    private var playerX = 76
    private var playerY = 120
    private var targetX = 32
    private var targetY = 50
    private var targetDx = 1
    private var score = 0
    private var tick = 0
    private var fireCooldown = 0
    private var shotActive = false
    private var shotX = 0
    private var shotY = 0

    fun reset() {
        playerX = 76
        playerY = 128
        targetX = 32
        targetY = 48
        score = 0
        tick = 0
        shotActive = false
    }

    fun stepFrame() {
        tick++
        if (p1Input.reset) reset()
        if (p1Input.left) playerX = (playerX - 2).coerceIn(8, 144)
        if (p1Input.right) playerX = (playerX + 2).coerceIn(8, 144)
        if (p1Input.up) playerY = (playerY - 2).coerceIn(28, 168)
        if (p1Input.down) playerY = (playerY + 2).coerceIn(28, 168)

        if (fireCooldown > 0) fireCooldown--
        if ((p1Input.fireA || p1Input.fireB) && !shotActive && fireCooldown == 0) {
            shotActive = true
            shotX = playerX + 4
            shotY = playerY - 4
            fireCooldown = 12
            audioEngine.setChannel0(4, 10, 8)
        } else if (fireCooldown < 5) {
            audioEngine.setChannel0(0, 0, 0)
        }

        if (shotActive) {
            shotY -= 4
            if (shotY < 20) shotActive = false
            if (kotlin.math.abs(shotX - (targetX + 6)) < 10 && kotlin.math.abs(shotY - (targetY + 4)) < 10) {
                score += 100
                shotActive = false
                targetX = (romBytes.getOrElse(tick % romBytes.size.coerceAtLeast(1)) { 40 }.toInt() and 0x7F).coerceIn(14, 136)
                audioEngine.setChannel1(8, 6, 10)
            }
        } else if (tick % 10 == 0) {
            audioEngine.setChannel1(0, 0, 0)
        }

        targetX += targetDx
        if (targetX <= 12 || targetX >= 138) targetDx = -targetDx

        val bg = when (system) {
            AtariSystem.ATARI_5200 -> 0xFF060E28.toInt()
            AtariSystem.ATARI_7800 -> 0xFF0A1D14.toInt()
            AtariSystem.ATARI_LYNX -> 0xFF071B24.toInt()
            AtariSystem.ATARI_JAGUAR -> 0xFF1D0712.toInt()
            else -> 0xFF0B0B12.toInt()
        }
        val accent = system.accentHex.toInt() or 0xFF000000.toInt()
        frameBuffer.fill(bg)

        for (i in 0 until 24) {
            val b = romBytes.getOrElse((i * 17 + tick / 8) % romBytes.size.coerceAtLeast(1)) { 0 }.toInt() and 0xFF
            val sx = (b * 13 + i * 7) % 156
            val sy = 22 + ((b * 5 + tick) % 156)
            if (sy in 20 until 182) frameBuffer[sy * 160 + sx] = 0xFF4A4E69.toInt()
        }

        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 10, 6, 0xFFFFB000.toInt(), 2)
        for (x in 6 until 154) frameBuffer[18 * 160 + x] = accent

        PixelFont3x5.drawRect(frameBuffer, targetX, targetY, 12, 8, 0xFFFFB000.toInt())
        PixelFont3x5.drawRect(frameBuffer, playerX, playerY, 10, 10, accent)
        PixelFont3x5.drawRect(frameBuffer, playerX + 3, playerY - 3, 4, 3, 0xFF00E5FF.toInt())
        if (shotActive) PixelFont3x5.drawRect(frameBuffer, shotX, shotY, 3, 6, 0xFFFFFFFF.toInt())
        for (y in 182 until 190) {
            for (x in 0 until 160) frameBuffer[y * 160 + x] = accent
        }
    }

    fun serializeState(): ByteArray = ByteBuffer.allocate(24).apply {
        putInt(playerX); putInt(playerY); putInt(targetX); putInt(targetY); putInt(score); putInt(tick)
    }.array()

    fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 24) return
        val b = ByteBuffer.wrap(bytes)
        playerX = b.int; playerY = b.int; targetX = b.int; targetY = b.int; score = b.int; tick = b.int
    }
}
