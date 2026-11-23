package com.example.emulator

import java.nio.ByteBuffer
import kotlin.math.abs

class AsteroidsClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private var sx = 76f
    private var sy = 156f
    private var bulletActive = false
    private var bx = 0f
    private var by = 0f
    private var cooldown = 0
    private var score = 0
    private var lives = 3
    private var tick = 0
    private val ax = floatArrayOf(20f, 60f, 100f, 135f, 45f, 115f)
    private val ay = floatArrayOf(30f, 55f, 40f, 70f, 95f, 110f)
    private val adx = floatArrayOf(0.8f, -0.7f, 0.9f, -0.9f, 0.6f, -0.6f)
    private val ady = floatArrayOf(0.9f, 1.1f, 0.8f, 1.0f, 1.2f, 0.9f)

    override fun reset() { sx = 76f; sy = 156f; bulletActive = false; score = 0; lives = 3 }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()
        if (p1Input.left) sx = (sx - 2.3f).coerceIn(8f, 144f)
        if (p1Input.right) sx = (sx + 2.3f).coerceIn(8f, 144f)
        if (p1Input.up) sy = (sy - 2.0f).coerceIn(40f, 174f)
        if (p1Input.down) sy = (sy + 2.0f).coerceIn(40f, 174f)

        if (cooldown > 0) cooldown--
        if ((p1Input.fireA || p1Input.fireB) && !bulletActive && cooldown == 0) {
            bulletActive = true; bx = sx + 4f; by = sy - 4f; cooldown = 10
            audio.setChannel0(4, 9, 8)
        } else if (cooldown < 4) audio.setChannel0(0, 0, 0)

        if (bulletActive) {
            by -= 4.2f
            if (by < 22f) bulletActive = false
        }

        for (i in ax.indices) {
            ax[i] = (ax[i] + adx[i] + 160f) % 160f
            ay[i] += ady[i]
            if (ay[i] > 186f) { ay[i] = 24f }
            if (bulletActive && abs(bx - (ax[i] + 6)) < 10f && abs(by - (ay[i] + 5)) < 10f) {
                bulletActive = false; score += 100
                ay[i] = 24f; ax[i] = ((tick * 29 + i * 37) % 140).toFloat()
                audio.setChannel1(8, 7, 10)
            }
            if (abs(sx - ax[i]) < 10f && abs(sy - ay[i]) < 10f) {
                if (!infiniteLivesCheat) {
                    lives--; if (lives <= 0) reset()
                }
                ay[i] = 24f
                audio.setChannel1(8, 22, 11)
            }
        }
        if (tick % 14 == 0) audio.setChannel1(0, 0, 0)

        frameBuffer.fill(0xFF02040D.toInt())
        for (i in 0..28) {
            val stX = (i * 53 + 17) % 158
            val stY = 24 + ((i * 31 + tick / 3) % 164)
            frameBuffer[stY * 160 + stX] = 0xFF5B5B85.toInt()
        }
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 5, 0xFF00E5FF.toInt(), 2)
        for (l in 0 until lives) PixelFont3x5.drawRect(frameBuffer, 124 + l * 10, 6, 6, 6, 0xFF00E5FF.toInt())

        for (i in ax.indices) {
            PixelFont3x5.drawRect(frameBuffer, ax[i].toInt(), ay[i].toInt(), 11, 10, 0xFF9E9EB5.toInt())
            PixelFont3x5.drawRect(frameBuffer, ax[i].toInt() + 2, ay[i].toInt() + 2, 4, 4, 0xFF5B5B70.toInt())
        }
        if (bulletActive) PixelFont3x5.drawRect(frameBuffer, bx.toInt(), by.toInt(), 2, 6, 0xFFFFFA40.toInt())

        // Spaceship
        PixelFont3x5.drawRect(frameBuffer, sx.toInt() + 3, sy.toInt(), 4, 10, 0xFF00E5FF.toInt())
        PixelFont3x5.drawRect(frameBuffer, sx.toInt(), sy.toInt() + 5, 10, 5, 0xFFFFFFFF.toInt())
        if (tick % 4 < 2) PixelFont3x5.drawRect(frameBuffer, sx.toInt() + 3, sy.toInt() + 10, 4, 3, 0xFFFF5A1F.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(80).apply {
        putFloat(sx); putFloat(sy); putInt(score); putInt(lives)
        for (i in ax.indices) { putFloat(ax[i]); putFloat(ay[i]) }
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 64) return
        val b = ByteBuffer.wrap(bytes)
        sx = b.float; sy = b.float; score = b.int; lives = b.int
        for (i in ax.indices) { ax[i] = b.float; ay[i] = b.float }
    }
}

class MissileClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private var cx = 80f
    private var cy = 90f
    private var blastR = 0
    private var blastX = 0
    private var blastY = 0
    private var score = 0
    private var cities = BooleanArray(6) { true }
    private val mx = floatArrayOf(25f, 70f, 115f, 140f)
    private val my = floatArrayOf(22f, 45f, 28f, 55f)
    private var tick = 0

    override fun reset() { cx = 80f; cy = 90f; blastR = 0; score = 0; cities.fill(true) }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()
        if (p1Input.left) cx = (cx - 2.6f).coerceIn(8f, 152f)
        if (p1Input.right) cx = (cx + 2.6f).coerceIn(8f, 152f)
        if (p1Input.up) cy = (cy - 2.6f).coerceIn(28f, 160f)
        if (p1Input.down) cy = (cy + 2.6f).coerceIn(28f, 160f)

        if ((p1Input.fireA || p1Input.fireB) && blastR == 0) {
            blastX = cx.toInt(); blastY = cy.toInt(); blastR = 16
            audio.setChannel0(8, 12, 10)
        }
        if (blastR > 0) blastR-- else audio.setChannel0(0, 0, 0)

        for (i in mx.indices) {
            my[i] += 0.65f + i * 0.1f
            if (blastR > 0 && abs(mx[i] - blastX) < 15 && abs(my[i] - blastY) < 15) {
                score += 50
                my[i] = 22f; mx[i] = ((tick * 37 + i * 43) % 144 + 8).toFloat()
            }
            if (my[i] >= 172f) {
                val cIdx = ((mx[i] / 160f) * 6).toInt().coerceIn(0, 5)
                if (!infiniteLivesCheat) cities[cIdx] = false
                my[i] = 22f; mx[i] = ((tick * 19 + i * 53) % 144 + 8).toFloat()
                if (cities.none { it }) reset()
            }
        }

        frameBuffer.fill(0xFF000000.toInt())
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 5, 0xFFFFB000.toInt(), 2)
        PixelFont3x5.drawRect(frameBuffer, 0, 176, 160, 16, 0xFFA94900.toInt())
        for (i in 0 until 6) {
            if (cities[i]) {
                val x = 14 + i * 24
                PixelFont3x5.drawRect(frameBuffer, x, 166, 14, 10, 0xFF00E5FF.toInt())
                PixelFont3x5.drawRect(frameBuffer, x + 4, 162, 6, 4, 0xFFFFFA40.toInt())
            }
        }
        for (i in mx.indices) {
            PixelFont3x5.drawRect(frameBuffer, mx[i].toInt(), 22, 1, (my[i] - 22).toInt(), 0xFFD62828.toInt())
            PixelFont3x5.drawRect(frameBuffer, mx[i].toInt() - 1, my[i].toInt(), 3, 3, 0xFFFFFFFF.toInt())
        }
        if (blastR > 0) {
            val r = 18 - blastR
            PixelFont3x5.drawRect(frameBuffer, blastX - r, blastY - r, r * 2, r * 2, if (blastR % 2 == 0) 0xFFFFFA40.toInt() else 0xFFFF5A1F.toInt())
        }
        // Crosshair
        PixelFont3x5.drawRect(frameBuffer, cx.toInt() - 4, cy.toInt(), 9, 1, 0xFFFFFFFF.toInt())
        PixelFont3x5.drawRect(frameBuffer, cx.toInt(), cy.toInt() - 4, 1, 9, 0xFFFFFFFF.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(64).apply {
        putFloat(cx); putFloat(cy); putInt(score)
        for (c in cities) put(if (c) 1 else 0)
        for (i in mx.indices) { putFloat(mx[i]); putFloat(my[i]) }
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 48) return
        val b = ByteBuffer.wrap(bytes)
        cx = b.float; cy = b.float; score = b.int
        for (i in cities.indices) cities[i] = b.get().toInt() != 0
        for (i in mx.indices) { mx[i] = b.float; my[i] = b.float }
    }
}

class AdventureClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private var px = 76f
    private var py = 136f
    private var dx = 76f
    private var dy = 56f
    private var hasKey = false
    private var hasSword = false
    private var dragonAlive = true
    private var score = 0
    private var tick = 0

    override fun reset() { px = 76f; py = 136f; dx = 76f; dy = 56f; hasKey = false; hasSword = false; dragonAlive = true; score = 0 }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()
        if (p1Input.left) px = (px - 2f).coerceIn(14f, 138f)
        if (p1Input.right) px = (px + 2f).coerceIn(14f, 138f)
        if (p1Input.up) py = (py - 2f).coerceIn(32f, 174f)
        if (p1Input.down) py = (py + 2f).coerceIn(32f, 174f)

        if (!hasKey && abs(px - 26f) < 10f && abs(py - 140f) < 10f) { hasKey = true; score += 100; audio.setChannel0(6, 8, 9) }
        if (!hasSword && abs(px - 126f) < 10f && abs(py - 140f) < 10f) { hasSword = true; score += 100; audio.setChannel0(6, 5, 9) }
        if (tick % 12 == 0) audio.setChannel0(0, 0, 0)

        if (dragonAlive) {
            if (dx < px) dx += 0.65f else dx -= 0.65f
            if (dy < py) dy += 0.65f else dy -= 0.65f
            if (abs(dx - px) < 10f && abs(dy - py) < 10f) {
                if (hasSword || p1Input.fireA) {
                    dragonAlive = false; score += 500; audio.setChannel1(8, 9, 10)
                } else if (!infiniteLivesCheat) {
                    px = 76f; py = 160f; audio.setChannel1(8, 22, 10)
                }
            }
        }
        // Golden Chalice
        if (hasKey && abs(px - 76f) < 12f && abs(py - 46f) < 12f) {
            score += 1000
            hasKey = false; hasSword = false; dragonAlive = true; dx = 76f; dy = 70f; px = 76f; py = 156f
            audio.setChannel1(4, 4, 10)
        } else if (tick % 14 == 0) audio.setChannel1(0, 0, 0)

        frameBuffer.fill(0xFF1A1A26.toInt())
        PixelFont3x5.drawText(frameBuffer, score.toString().padStart(5, '0'), 8, 5, 0xFFFFB000.toInt(), 2)
        // Castle Walls
        PixelFont3x5.drawRect(frameBuffer, 6, 24, 148, 162, 0xFF47B837.toInt())
        PixelFont3x5.drawRect(frameBuffer, 14, 32, 132, 146, 0xFF1A1A26.toInt())
        PixelFont3x5.drawRect(frameBuffer, 34, 82, 92, 10, 0xFF47B837.toInt())
        PixelFont3x5.drawRect(frameBuffer, 68, 82, 24, 10, 0xFF1A1A26.toInt())

        // Chalice
        PixelFont3x5.drawRect(frameBuffer, 74, 42, 12, 10, if ((tick / 6) % 2 == 0) 0xFFFFFA40.toInt() else 0xFFFFB000.toInt())
        if (!hasKey) PixelFont3x5.drawRect(frameBuffer, 26, 140, 8, 5, 0xFFFFFA40.toInt())
        if (!hasSword) PixelFont3x5.drawRect(frameBuffer, 126, 140, 10, 4, 0xFF00E5FF.toInt())
        if (dragonAlive) {
            PixelFont3x5.drawRect(frameBuffer, dx.toInt(), dy.toInt(), 12, 12, 0xFFD62828.toInt())
            PixelFont3x5.drawRect(frameBuffer, dx.toInt() + 2, dy.toInt() + 2, 3, 3, 0xFFFFFA40.toInt())
        }
        // Square Hero
        PixelFont3x5.drawRect(frameBuffer, px.toInt(), py.toInt(), 8, 8, 0xFFFFB000.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(32).apply {
        putFloat(px); putFloat(py); putFloat(dx); putFloat(dy)
        putInt(score); put(if (hasKey) 1 else 0); put(if (hasSword) 1 else 0); put(if (dragonAlive) 1 else 0)
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 23) return
        val b = ByteBuffer.wrap(bytes)
        px = b.float; py = b.float; dx = b.float; dy = b.float
        score = b.int; hasKey = b.get().toInt() != 0; hasSword = b.get().toInt() != 0; dragonAlive = b.get().toInt() != 0
    }
}

class CombatClassicEngine(private val audio: AtariAudioEngine) : PlayableAtariEngine {
    override val frameBuffer = IntArray(160 * 192)
    override var p1Input = ControllerInput()
    override var p2Input = ControllerInput()
    override var twoPlayerMode = false
    override var infiniteLivesCheat = false

    private var t1x = 24f; private var t1y = 100f; private var t1DirX = 1f; private var t1DirY = 0f
    private var t2x = 128f; private var t2y = 100f; private var t2DirX = -1f; private var t2DirY = 0f
    private var b1Active = false; private var b1x = 0f; private var b1y = 0f; private var b1vx = 0f; private var b1vy = 0f
    private var b2Active = false; private var b2x = 0f; private var b2y = 0f; private var b2vx = 0f; private var b2vy = 0f
    private var s1 = 0; private var s2 = 0; private var tick = 0

    override fun reset() {
        t1x = 24f; t1y = 100f; t2x = 128f; t2y = 100f; b1Active = false; b2Active = false; s1 = 0; s2 = 0
    }

    override fun stepFrame() {
        tick++
        if (p1Input.reset || p1Input.start) reset()

        if (p1Input.left) { t1x -= 1.6f; t1DirX = -1f; t1DirY = 0f }
        if (p1Input.right) { t1x += 1.6f; t1DirX = 1f; t1DirY = 0f }
        if (p1Input.up) { t1y -= 1.6f; t1DirX = 0f; t1DirY = -1f }
        if (p1Input.down) { t1y += 1.6f; t1DirX = 0f; t1DirY = 1f }
        t1x = t1x.coerceIn(10f, 140f); t1y = t1y.coerceIn(30f, 174f)

        if ((p1Input.fireA || p1Input.fireB) && !b1Active) {
            b1Active = true; b1x = t1x + 4f; b1y = t1y + 4f; b1vx = t1DirX * 3.5f; b1vy = t1DirY * 3.5f
            audio.setChannel0(4, 11, 9)
        }

        if (twoPlayerMode) {
            if (p2Input.left) { t2x -= 1.6f; t2DirX = -1f; t2DirY = 0f }
            if (p2Input.right) { t2x += 1.6f; t2DirX = 1f; t2DirY = 0f }
            if (p2Input.up) { t2y -= 1.6f; t2DirX = 0f; t2DirY = -1f }
            if (p2Input.down) { t2y += 1.6f; t2DirX = 0f; t2DirY = 1f }
            if ((p2Input.fireA || p2Input.fireB) && !b2Active) {
                b2Active = true; b2x = t2x + 4f; b2y = t2y + 4f; b2vx = t2DirX * 3.5f; b2vy = t2DirY * 3.5f
            }
        } else {
            // Simple tactical AI tank
            if (t2y < t1y) { t2y += 0.7f; t2DirX = 0f; t2DirY = 1f } else if (t2y > t1y) { t2y -= 0.7f; t2DirX = 0f; t2DirY = -1f }
            if (tick % 60 == 0 && !b2Active) {
                b2Active = true; b2x = t2x + 4f; b2y = t2y + 4f
                b2vx = if (t1x < t2x) -3f else 3f; b2vy = 0f
            }
        }
        t2x = t2x.coerceIn(10f, 140f); t2y = t2y.coerceIn(30f, 174f)

        if (b1Active) {
            b1x += b1vx; b1y += b1vy
            if (b1x !in 8f..152f || b1y !in 26f..184f) b1Active = false
            if (abs(b1x - (t2x + 5)) < 9f && abs(b1y - (t2y + 5)) < 9f) {
                b1Active = false; s1 += 1; audio.setChannel1(8, 8, 10)
            }
        }
        if (b2Active) {
            b2x += b2vx; b2y += b2vy
            if (b2x !in 8f..152f || b2y !in 26f..184f) b2Active = false
            if (abs(b2x - (t1x + 5)) < 9f && abs(b2y - (t1y + 5)) < 9f) {
                b2Active = false; if (!infiniteLivesCheat) s2 += 1; audio.setChannel1(8, 12, 10)
            }
        }
        if (tick % 10 == 0) { audio.setChannel0(0, 0, 0); audio.setChannel1(0, 0, 0) }

        frameBuffer.fill(0xFF335E00.toInt())
        PixelFont3x5.drawRect(frameBuffer, 0, 0, 160, 22, 0xFF000000.toInt())
        PixelFont3x5.drawText(frameBuffer, s1.toString().padStart(2, '0'), 24, 5, 0xFFFF5A1F.toInt(), 2)
        PixelFont3x5.drawText(frameBuffer, s2.toString().padStart(2, '0'), 112, 5, 0xFF00E5FF.toInt(), 2)

        // Arena barriers
        PixelFont3x5.drawRect(frameBuffer, 4, 24, 152, 4, 0xFFF58F17.toInt())
        PixelFont3x5.drawRect(frameBuffer, 4, 184, 152, 4, 0xFFF58F17.toInt())
        PixelFont3x5.drawRect(frameBuffer, 76, 62, 8, 30, 0xFFF58F17.toInt())
        PixelFont3x5.drawRect(frameBuffer, 76, 118, 8, 30, 0xFFF58F17.toInt())

        PixelFont3x5.drawRect(frameBuffer, t1x.toInt(), t1y.toInt(), 10, 10, 0xFFFF5A1F.toInt())
        PixelFont3x5.drawRect(frameBuffer, t2x.toInt(), t2y.toInt(), 10, 10, 0xFF00E5FF.toInt())
        if (b1Active) PixelFont3x5.drawRect(frameBuffer, b1x.toInt(), b1y.toInt(), 3, 3, 0xFFFFFFFF.toInt())
        if (b2Active) PixelFont3x5.drawRect(frameBuffer, b2x.toInt(), b2y.toInt(), 3, 3, 0xFFFFFA40.toInt())
    }

    override fun serializeState(): ByteArray = ByteBuffer.allocate(32).apply {
        putFloat(t1x); putFloat(t1y); putFloat(t2x); putFloat(t2y); putInt(s1); putInt(s2)
    }.array()

    override fun deserializeState(bytes: ByteArray) {
        if (bytes.size < 24) return
        val b = ByteBuffer.wrap(bytes)
        t1x = b.float; t1y = b.float; t2x = b.float; t2y = b.float; s1 = b.int; s2 = b.int
    }
}
