package com.example.emulator

import java.nio.ByteBuffer

interface PlayableAtariEngine {
    val frameBuffer: IntArray
    var p1Input: ControllerInput
    var p2Input: ControllerInput
    var twoPlayerMode: Boolean
    var infiniteLivesCheat: Boolean

    fun reset()
    fun stepFrame()
    fun serializeState(): ByteArray
    fun deserializeState(bytes: ByteArray)
}

object PixelFont3x5 {
    private val glyphs = mapOf(
        '0' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        '3' to intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '6' to intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        '7' to intArrayOf(0b111, 0b001, 0b001, 0b010, 0b010),
        '8' to intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        '9' to intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111),
        'P' to intArrayOf(0b111, 0b101, 0b111, 0b100, 0b100),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'S' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '-' to intArrayOf(0b000, 0b000, 0b111, 0b000, 0b000)
    )

    fun drawText(buf: IntArray, text: String, x: Int, y: Int, color: Int, scale: Int = 2) {
        var cx = x
        for (ch in text.uppercase()) {
            val rows = glyphs[ch]
            if (rows != null) {
                for (r in 0 until 5) {
                    val bits = rows[r]
                    for (c in 0 until 3) {
                        if ((bits and (1 shl (2 - c))) != 0) {
                            drawRect(buf, cx + c * scale, y + r * scale, scale, scale, color)
                        }
                    }
                }
            }
            cx += 4 * scale
        }
    }

    fun drawRect(buf: IntArray, rx: Int, ry: Int, w: Int, h: Int, color: Int) {
        for (dy in 0 until h) {
            val py = ry + dy
            if (py !in 0 until 192) continue
            val rowOff = py * 160
            for (dx in 0 until w) {
                val px = rx + dx
                if (px in 0 until 160) buf[rowOff + px] = color
            }
        }
    }

    fun drawBitmapSprite(buf: IntArray, rows: IntArray, rx: Int, ry: Int, color: Int, scaleX: Int = 2, scaleY: Int = 2, flipX: Boolean = false) {
        for (r in rows.indices) {
            val bits = rows[r]
            for (c in 0 until 8) {
                val mask = if (flipX) (1 shl c) else (0x80 ushr c)
                if ((bits and mask) != 0) {
                    drawRect(buf, rx + c * scaleX, ry + r * scaleY, scaleX, scaleY, color)
                }
            }
        }
    }
}

object ClassicGameFactory {
    fun createForTitle(title: String, genre: String, audio: AtariAudioEngine): PlayableAtariEngine {
        val t = title.uppercase()
        return when {
            "PAC" in t || "DIG" in t -> PacManClassicEngine(audio)
            "FROGGER" in t || "FREEWAY" in t -> FroggerClassicEngine(audio)
            "BREAKOUT" in t || "KABOOM" in t || "BALLBLAZER" in t || "PONG" in t || "TENNIS" in t -> BreakoutClassicEngine(audio)
            "ASTEROID" in t || "INVADER" in t || "GALAX" in t || "DEFENDER" in t || "RAID" in t || "STAR" in t || "YARS" in t || "CYBER" in t -> AsteroidsClassicEngine(audio)
            "MISSILE" in t || "CENTIPEDE" in t || "TEMPEST" in t || "PATROL" in t -> MissileClassicEngine(audio)
            "ADVENTURE" in t || "BERZERK" in t || "CHIP" in t || "HAUNTED" in t -> AdventureClassicEngine(audio)
            "COMBAT" in t || "TANK" in t || "ENDURO" in t || "POLE" in t || "GOLF" in t -> CombatClassicEngine(audio)
            genre == "أركيد" -> PacManClassicEngine(audio)
            genre == "تصويب" || genre == "فضاء" -> AsteroidsClassicEngine(audio)
            genre == "سباق" || genre == "رياضة" -> CombatClassicEngine(audio)
            genre == "ألغاز" -> AdventureClassicEngine(audio)
            else -> PitfallClassicEngine(audio)
        }
    }
}
