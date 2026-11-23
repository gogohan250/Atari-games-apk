package com.example.emulator

import java.nio.ByteBuffer
import kotlin.math.abs

class PitfallClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private var px = 22f
    private var py = 122f
    private var vy = 0f
    private var onGround = true
    private var facingLeft = false
    private var score = 2000
    private var lives = 3
    private var screenIdx = 0
    private var logX = 140f
    private var crocOpen = 0
    private var treasureCollected = false
    private var tick = 0
    private var sfxTimer = 0
    private var hurtFlash = 0

    private val runner1 = intArrayOf(0x38, 0x38, 0x10, 0x7C, 0x10, 0x28, 0x44, 0x82)
    private val runner2 = intArrayOf(0x38, 0x38, 0x10, 0x38, 0x10, 0x38, 0x28, 0x28)

    override fun reset() {
        px = 22f; py = 122f; vy = 0f; onGround = true; facingLeft = false
        score = 2000; lives = 3; screenIdx = 0; logX = 140f; treasureCollected = false; hurtFlash = 0
    }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()

        if (p1Input.left) { px -= 1.8f; facingLeft = true }
        if (p1Input.right) { px += 1.8f; facingLeft = false }

        if ((p1Input.fireA || p1Input.fireB || p1Input.up) && onGround) {
            vy = -4.2f
            onGround = false
            audio.setChannel0(4, 14, 9)
            sfxTimer = 10
        }

        if (!onGround) {
            py += vy
            vy += 0.25f
            if (py >= 122f) {
                py = 122f
                vy = 0f
                onGround = true
            }
        }

        // Screen transitions
        if (px > 152f) { px = 6f; screenIdx++; treasureCollected = false }
        if (px < 4f) { px = 148f; screenIdx = (screenIdx - 1).coerceAtLeast(0); treasureCollected = false }

        // Rolling log
        logX -= 1.2f + (screenIdx % 3) * 0.25f
        if (logX < -10f) logX = 162f
        crocOpen = (tick / 35) % 2

        // Treasure pickup
        if (!treasureCollected && abs(px - 118f) < 10f && abs(py - 122f) < 12f) {
            treasureCollected = true
            score += 500
            audio.setChannel1(6, 5, 11)
            sfxTimer = 14
        }

        // Hazard collisions: log or crocodile pond at middle (x=66..94)
        val hitLog = abs(px - logX) < 8f && py > 114f
        val inPond = px in 66f..90f && py >= 120f && (screenIdx % 2 == 1)
        if ((hitLog || inPond) && hurtFlash == 0) {
            hurtFlash = 30
            if (!infiniteLivesCheat) {
                lives--
                if (lives <= 0) { lives = 3; score = 2000; screenIdx = 0 }
            }
            px = 20f; py = 122f
            audio.setChannel1(8, 22, 10)
            sfxTimer = 18
        }
        if (hurtFlash > 0) hurtFlash--
        if (sfxTimer > 0) {
            sfxTimer--
            if (sfxTimer == 0) { audio.setChannel0(0, 0, 0); audio.setChannel1(0, 0, 0) }
        }

        // Render 160x192 Atari Pitfall Jungle
        val sky = if (hurtFlash > 0 && hurtFlash % 6 < 3) 0xFF5E0800.toInt() else 0xFF1D4ED8.toInt()
        val canopy = 0xFF15803D.toInt()
        val trunk = 0xFF5D4100.toInt()
        val grass = 0xFF47B837.toInt()
        val dirt = 0xFF8C5500.toInt()
        val cave = 0xFF1A1A1A.toInt()

        PixelFont3x5.drawRect(frameBuffer, 0, 0, 160, 22, 0xFF000000.toInt())
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 6, 0xFFEDEDED.toInt(), 2)
        for (l in 0 until lives) PixelFont3x5.drawRect(frameBuffer, 122 + l * 10, 7, 6, 8, 0xFFFFB000.toInt())

        PixelFont3x5.drawRect(frameBuffer, 0, 22, 160, 22, canopy)
        PixelFont3x5.drawRect(frameBuffer, 0, 44, 160, 94, sky)
        // Jungle tree trunks
        for (tx in intArrayOf(26, 76, 128)) {
            PixelFont3x5.drawRect(frameBuffer, tx, 44, 8, 94, trunk)
            PixelFont3x5.drawRect(frameBuffer, tx - 6, 44, 20, 10, canopy)
        }
        // Swinging vine
        val vineX = 80 + (kotlin.math.sin(tick * 0.06) * 26).toInt()
        for (vyLine in 44..98) {
            val vx = 80 + ((vineX - 80) * (vyLine - 44)) / 54
            PixelFont3x5.drawRect(frameBuffer, vx, vyLine, 2, 2, 0xFF9ACE2E.toInt())
        }

        // Ground & Underground
        PixelFont3x5.drawRect(frameBuffer, 0, 138, 160, 14, grass)
        PixelFont3x5.drawRect(frameBuffer, 0, 152, 160, 40, dirt)
        PixelFont3x5.drawRect(frameBuffer, 10, 158, 140, 26, cave)
        PixelFont3x5.drawRect(frameBuffer, 10, 184, 140, 8, 0xFF673300.toInt())

        // Pond & Crocodiles on odd screens
        if (screenIdx % 2 == 1) {
            PixelFont3x5.drawRect(frameBuffer, 64, 138, 34, 10, 0xFF005199.toInt())
            val jaw = if (crocOpen == 1) 4 else 2
            PixelFont3x5.drawRect(frameBuffer, 72, 134, 14, jaw, 0xFF279118.toInt())
        }

        // Rolling Log
        PixelFont3x5.drawRect(frameBuffer, logX.toInt(), 128, 10, 10, 0xFFA94900.toInt())
        PixelFont3x5.drawRect(frameBuffer, logX.toInt() + 2, 130, 4, 4, 0xFFF58F17.toInt())

        // Gold Bar Treasure
        if (!treasureCollected) {
            PixelFont3x5.drawRect(frameBuffer, 118, 128, 12, 8, 0xFFFFFA40.toInt())
            PixelFont3x5.drawRect(frameBuffer, 120, 130, 8, 4, 0xFFFFB000.toInt())
        }

        // Explorer Sprite
        val sprite = if ((tick / 6) % 2 == 0 || !onGround) runner1 else runner2
        PixelFont3x5.drawBitmapSprite(frameBuffer, sprite, px.toInt(), py.toInt(), 0xFFFF5A1F.toInt(), 2, 2, facingLeft)
        PixelFont3x5.drawRect(frameBuffer, px.toInt() + 4, py.toInt(), 8, 4, 0xFF47B837.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(36).apply {
        putFloat(px); putFloat(py); putFloat(vy); putInt(score); putInt(lives); putInt(screenIdx); putFloat(logX)
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 28) return
        val b = ByteBuffer.wrap(bytes)
        px = b.float; py = b.float; vy = b.float; score = b.int; lives = b.int; screenIdx = b.int; logX = b.float
    }
}

class PacManClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private val cols = 15
    private val rows = 15
    private val dots = BooleanArray(cols * rows)
    private var px = 7
    private var py = 11
    private var dirX = 1
    private var dirY = 0
    private var score = 0
    private var lives = 3
    private var powerTimer = 0
    private var tick = 0
    private val gx = intArrayOf(6, 7, 8, 7)
    private val gy = intArrayOf(6, 6, 6, 4)

    // 15x15 classic Atari maze (1 = wall)
    private val maze = arrayOf(
        "111111111111111",
        "100000010000001",
        "101101010101101",
        "100000000000001",
        "101101111101101",
        "100000010000001",
        "111011000110111",
        "100010000010001",
        "111011111110111",
        "100000010000001",
        "101101010101101",
        "100100000001001",
        "110101111101011",
        "100000000000001",
        "111111111111111"
    )

    init { reset() }

    override fun reset() {
        px = 7; py = 11; dirX = 1; dirY = 0; score = 0; lives = 3; powerTimer = 0
        gx[0] = 6; gy[0] = 6; gx[1] = 7; gy[1] = 6; gx[2] = 8; gy[2] = 6; gx[3] = 7; gy[3] = 3
        for (r in 0 until rows) for (c in 0 until cols) dots[r * cols + c] = maze[r][c] == '0'
    }

    private fun isWall(c: Int, r: Int): Boolean =
        r !in 0 until rows || c !in 0 until cols || maze[r][c] == '1'

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()

        if (p1Input.left && !isWall(px - 1, py)) { dirX = -1; dirY = 0 }
        if (p1Input.right && !isWall(px + 1, py)) { dirX = 1; dirY = 0 }
        if (p1Input.up && !isWall(px, py - 1)) { dirX = 0; dirY = -1 }
        if (p1Input.down && !isWall(px, py + 1)) { dirX = 0; dirY = 1 }

        if (tick % 9 == 0) {
            if (!isWall(px + dirX, py + dirY)) {
                px += dirX; py += dirY
                val idx = py * cols + px
                if (dots[idx]) {
                    dots[idx] = false
                    score += 10
                    val isPower = (px == 1 || px == 13) && (py == 1 || py == 13)
                    if (isPower) { powerTimer = 240; score += 40 }
                    audio.setChannel0(12, if (isPower) 6 else 14, 7)
                } else {
                    audio.setChannel0(0, 0, 0)
                }
            } else {
                audio.setChannel0(0, 0, 0)
            }
            if (dots.none { it }) {
                for (r in 0 until rows) for (c in 0 until cols) dots[r * cols + c] = maze[r][c] == '0'
            }
        }

        if (powerTimer > 0) powerTimer--

        if (tick % 13 == 0) {
            for (g in 0 until 4) {
                val choices = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).filter { !isWall(gx[g] + it.first, gy[g] + it.second) }
                if (choices.isNotEmpty()) {
                    val best = if (powerTimer == 0 && (tick + g) % 2 == 0) {
                        choices.minByOrNull { abs((gx[g] + it.first) - px) + abs((gy[g] + it.second) - py) } ?: choices.first()
                    } else {
                        choices[(tick / 13 + g) % choices.size]
                    }
                    gx[g] += best.first; gy[g] += best.second
                }
            }
        }

        for (g in 0 until 4) {
            if (gx[g] == px && gy[g] == py) {
                if (powerTimer > 0) {
                    score += 200; gx[g] = 7; gy[g] = 6
                    audio.setChannel1(4, 8, 9)
                } else {
                    if (!infiniteLivesCheat) {
                        lives--
                        if (lives <= 0) reset()
                    }
                    px = 7; py = 11
                    audio.setChannel1(8, 20, 10)
                }
            }
        }

        frameBuffer.fill(0xFF000000.toInt())
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 5, 0xFFFFFA40.toInt(), 2)
        for (l in 0 until lives) PixelFont3x5.drawRect(frameBuffer, 124 + l * 10, 6, 7, 7, 0xFFFFFA40.toInt())

        val cellW = 10
        val cellH = 11
        val ox = 5
        val oy = 22
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val sx = ox + c * cellW
                val sy = oy + r * cellH
                if (maze[r][c] == '1') {
                    PixelFont3x5.drawRect(frameBuffer, sx, sy, cellW, cellH, 0xFF215CDD.toInt())
                    PixelFont3x5.drawRect(frameBuffer, sx + 2, sy + 2, cellW - 4, cellH - 4, 0xFF0B256B.toInt())
                } else if (dots[r * cols + c]) {
                    val isPower = (c == 1 || c == 13) && (r == 1 || r == 13)
                    if (isPower) {
                        if ((tick / 8) % 2 == 0) PixelFont3x5.drawRect(frameBuffer, sx + 2, sy + 3, 6, 6, 0xFFFFB439.toInt())
                    } else {
                        PixelFont3x5.drawRect(frameBuffer, sx + 4, sy + 5, 2, 2, 0xFFFFDF60.toInt())
                    }
                }
            }
        }

        // Pac-Man
        val pacX = ox + px * cellW + 1
        val pacY = oy + py * cellH + 1
        PixelFont3x5.drawRect(frameBuffer, pacX, pacY, 8, 8, 0xFFFFFA40.toInt())
        if ((tick / 5) % 2 == 0) {
            val mx = if (dirX >= 0) pacX + 5 else pacX
            PixelFont3x5.drawRect(frameBuffer, mx, pacY + 3, 3, 2, 0xFF000000.toInt())
        }

        // Ghosts
        val ghostCols = intArrayOf(0xFFE45335.toInt(), 0xFFFF67A8.toInt(), 0xFF00E5FF.toInt(), 0xFFFFB000.toInt())
        for (g in 0 until 4) {
            val col = if (powerTimer > 0) 0xFF4080FF.toInt() else ghostCols[g]
            val sx = ox + gx[g] * cellW + 1
            val sy = oy + gy[g] * cellH + 1
            PixelFont3x5.drawRect(frameBuffer, sx, sy, 8, 7, col)
            PixelFont3x5.drawRect(frameBuffer, sx + 1, sy + 2, 2, 2, 0xFFFFFFFF.toInt())
            PixelFont3x5.drawRect(frameBuffer, sx + 5, sy + 2, 2, 2, 0xFFFFFFFF.toInt())
        }
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(300).apply {
        putInt(px); putInt(py); putInt(dirX); putInt(dirY); putInt(score); putInt(lives); putInt(powerTimer)
        for (b in dots) put(if (b) 1 else 0)
        for (i in 0..3) { putInt(gx[i]); putInt(gy[i]) }
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 250) return
        val b = ByteBuffer.wrap(bytes)
        px = b.int; py = b.int; dirX = b.int; dirY = b.int; score = b.int; lives = b.int; powerTimer = b.int
        for (i in dots.indices) dots[i] = b.get().toInt() != 0
        for (i in 0..3) { gx[i] = b.int; gy[i] = b.int }
    }
}

class FroggerClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private var fx = 76
    private var fy = 172
    private var score = 0
    private var lives = 3
    private var moveCooldown = 0
    private var tick = 0
    private val carX = floatArrayOf(10f, 90f, 30f, 120f, 50f)
    private val logX = floatArrayOf(10f, 80f, 40f, 110f)

    override fun reset() { fx = 76; fy = 172; score = 0; lives = 3 }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()
        if (moveCooldown > 0) moveCooldown--

        if (moveCooldown == 0) {
            var moved = false
            if (p1Input.up) { fy = (fy - 14).coerceAtLeast(28); score += 10; moved = true }
            else if (p1Input.down) { fy = (fy + 14).coerceAtMost(172); moved = true }
            else if (p1Input.left) { fx = (fx - 10).coerceIn(6, 146); moved = true }
            else if (p1Input.right) { fx = (fx + 10).coerceIn(6, 146); moved = true }
            if (moved) {
                moveCooldown = 9
                audio.setChannel0(4, 16, 6)
            } else audio.setChannel0(0, 0, 0)
        }

        // Move highway cars (y=116, 130, 144, 158)
        for (i in carX.indices) {
            val spd = if (i % 2 == 0) 1.3f else -1.5f
            carX[i] = (carX[i] + spd + 160f) % 160f
        }
        // Move river logs (y=46, 60, 74, 88)
        for (i in logX.indices) {
            val spd = if (i % 2 == 0) 0.9f else -1.1f
            logX[i] = (logX[i] + spd + 160f) % 160f
            val laneY = 46 + i * 14
            if (abs(fy - laneY) < 7) {
                fx = (fx + spd.toInt()).coerceIn(4, 150)
            }
        }

        // Check goal
        if (fy <= 32) {
            score += 200
            fx = 76; fy = 172
            audio.setChannel1(6, 4, 10)
        }

        // Check car collisions
        var died = false
        for (i in carX.indices) {
            val laneY = 116 + (i % 4) * 14
            if (abs(fy - laneY) < 8 && abs(fx - carX[i].toInt()) < 14) died = true
        }
        // Check river drowning if not on a log
        if (fy in 44..94) {
            var onLog = false
            for (i in logX.indices) {
                val laneY = 46 + i * 14
                if (abs(fy - laneY) < 8 && (fx >= logX[i] - 4 && fx <= logX[i] + 32)) onLog = true
            }
            if (!onLog) died = true
        }
        if (died) {
            if (!infiniteLivesCheat) {
                lives--; if (lives <= 0) reset()
            }
            fx = 76; fy = 172
            audio.setChannel1(8, 24, 10)
        } else if (tick % 12 == 0) {
            audio.setChannel1(0, 0, 0)
        }

        // Render Frogger
        PixelFont3x5.drawRect(frameBuffer, 0, 0, 160, 22, 0xFF000000.toInt())
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 5, 0xFF69DF57.toInt(), 2)
        for (l in 0 until lives) PixelFont3x5.drawRect(frameBuffer, 124 + l * 10, 6, 6, 6, 0xFF69DF57.toInt())

        // Goal bushes & lily pads
        PixelFont3x5.drawRect(frameBuffer, 0, 22, 160, 20, 0xFF0D6C02.toInt())
        for (gx in intArrayOf(16, 52, 88, 124)) PixelFont3x5.drawRect(frameBuffer, gx, 26, 16, 12, 0xFF005199.toInt())

        // River
        PixelFont3x5.drawRect(frameBuffer, 0, 42, 160, 58, 0xFF073ABB.toInt())
        for (i in logX.indices) {
            PixelFont3x5.drawRect(frameBuffer, logX[i].toInt(), 46 + i * 14, 34, 9, 0xFFA94900.toInt())
        }

        // Safe median
        PixelFont3x5.drawRect(frameBuffer, 0, 100, 160, 14, 0xFF7D0584.toInt())

        // Highway
        PixelFont3x5.drawRect(frameBuffer, 0, 114, 160, 56, 0xFF1A1A1A.toInt())
        val carColors = intArrayOf(0xFFE45335.toInt(), 0xFFFFFA40.toInt(), 0xFF00E5FF.toInt(), 0xFFFF88FF.toInt(), 0xFFFF5A1F.toInt())
        for (i in carX.indices) {
            PixelFont3x5.drawRect(frameBuffer, carX[i].toInt(), 116 + (i % 4) * 14, 16, 9, carColors[i])
        }

        // Safe start curb
        PixelFont3x5.drawRect(frameBuffer, 0, 170, 160, 22, 0xFF7D0584.toInt())

        // Frog
        PixelFont3x5.drawRect(frameBuffer, fx, fy, 8, 8, 0xFF8DFF79.toInt())
        PixelFont3x5.drawRect(frameBuffer, fx + 1, fy + 1, 2, 2, 0xFF000000.toInt())
        PixelFont3x5.drawRect(frameBuffer, fx + 5, fy + 1, 2, 2, 0xFF000000.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(64).apply {
        putInt(fx); putInt(fy); putInt(score); putInt(lives)
        for (c in carX) putFloat(c)
        for (l in logX) putFloat(l)
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 52) return
        val b = ByteBuffer.wrap(bytes)
        fx = b.int; fy = b.int; score = b.int; lives = b.int
        for (i in carX.indices) carX[i] = b.float
        for (i in logX.indices) logX[i] = b.float
    }
}

class BreakoutClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private val bCols = 10
    private val bRows = 5
    private val bricks = BooleanArray(bCols * bRows) { true }
    private var padX = 68f
    private var pad2X = 68f
    private var bx = 80f
    private var by = 140f
    private var vx = 1.4f
    private var vy = -1.7f
    private var score = 0
    private var lives = 3
    private var tick = 0

    override fun reset() {
        bricks.fill(true); padX = 68f; pad2X = 68f; bx = 80f; by = 140f; vx = 1.4f; vy = -1.7f; score = 0; lives = 3
    }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()

        if (p1Input.left) padX = (padX - 3f).coerceIn(6f, 130f)
        if (p1Input.right) padX = (padX + 3f).coerceIn(6f, 130f)
        if (twoPlayerMode) {
            if (p2Input.left) pad2X = (pad2X - 3f).coerceIn(6f, 130f)
            if (p2Input.right) pad2X = (pad2X + 3f).coerceIn(6f, 130f)
        }

        bx += vx; by += vy
        if (bx <= 6f || bx >= 152f) { vx = -vx; audio.setChannel0(4, 18, 6) }
        if (by <= 24f) { vy = -vy; audio.setChannel0(4, 14, 6) }

        // Brick collisions
        val bw = 14; val bh = 7
        for (r in 0 until bRows) {
            for (c in 0 until bCols) {
                val idx = r * bCols + c
                if (!bricks[idx]) continue
                val rx = 10 + c * bw
                val ry = 34 + r * bh
                if (bx + 4 >= rx && bx <= rx + bw - 2 && by + 4 >= ry && by <= ry + bh - 2) {
                    bricks[idx] = false
                    vy = -vy
                    score += (bRows - r) * 10
                    audio.setChannel1(6, 5 + r * 2, 9)
                }
            }
        }
        if (bricks.none { it }) bricks.fill(true)

        // Paddle collision
        if (vy > 0 && by in 164f..172f) {
            val hitP1 = bx + 4 >= padX && bx <= padX + 26f
            val hitP2 = twoPlayerMode && bx + 4 >= pad2X && bx <= pad2X + 26f
            if (hitP1 || hitP2) {
                val center = (if (hitP1) padX else pad2X) + 13f
                vx = ((bx - center) / 13f) * 2.1f
                vy = -abs(vy)
                audio.setChannel0(4, 10, 8)
            }
        }
        if (tick % 10 == 0) { audio.setChannel0(0, 0, 0); audio.setChannel1(0, 0, 0) }

        if (by > 188f) {
            if (!infiniteLivesCheat) {
                lives--; if (lives <= 0) reset()
            }
            bx = 80f; by = 140f; vy = -1.7f
        }

        frameBuffer.fill(0xFF000000.toInt())
        PixelFont3x5.drawRect(frameBuffer, 0, 20, 160, 4, 0xFF7E7E7E.toInt())
        PixelFont3x5.drawRect(frameBuffer, 0, 20, 4, 172, 0xFF7E7E7E.toInt())
        PixelFont3x5.drawRect(frameBuffer, 156, 20, 4, 172, 0xFF7E7E7E.toInt())
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 5, 0xFFFF5A1F.toInt(), 2)
        for (l in 0 until lives) PixelFont3x5.drawRect(frameBuffer, 124 + l * 10, 6, 6, 6, 0xFFFFB000.toInt())

        val rowColors = intArrayOf(0xFFD62828.toInt(), 0xFFFF5A1F.toInt(), 0xFFFFB000.toInt(), 0xFF47B837.toInt(), 0xFF00E5FF.toInt())
        for (r in 0 until bRows) {
            for (c in 0 until bCols) {
                if (bricks[r * bCols + c]) {
                    PixelFont3x5.drawRect(frameBuffer, 10 + c * bw, 34 + r * bh, bw - 2, bh - 2, rowColors[r])
                }
            }
        }
        PixelFont3x5.drawRect(frameBuffer, padX.toInt(), 168, 26, 5, 0xFFFF5A1F.toInt())
        if (twoPlayerMode) PixelFont3x5.drawRect(frameBuffer, pad2X.toInt(), 176, 26, 5, 0xFF00E5FF.toInt())
        PixelFont3x5.drawRect(frameBuffer, bx.toInt(), by.toInt(), 4, 4, 0xFFFFFFFF.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(120).apply {
        putFloat(padX); putFloat(bx); putFloat(by); putFloat(vx); putFloat(vy); putInt(score); putInt(lives)
        for (b in bricks) put(if (b) 1 else 0)
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 78) return
        val b = ByteBuffer.wrap(bytes)
        padX = b.float; bx = b.float; by = b.float; vx = b.float; vy = b.float; score = b.int; lives = b.int
        for (i in bricks.indices) bricks[i] = b.get().toInt() != 0
    }
}
