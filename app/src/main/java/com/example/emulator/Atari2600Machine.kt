package com.example.emulator

import java.nio.ByteBuffer

data class ControllerInput(
    var up: Boolean = false,
    var down: Boolean = false,
    var left: Boolean = false,
    var right: Boolean = false,
    var fireA: Boolean = false,
    var fireB: Boolean = false,
    var select: Boolean = false,
    var reset: Boolean = false,
    var start: Boolean = false
)

/**
 * Genuine Atari 2600 (Stella-compatible) Hardware Machine:
 * MOS 6507 CPU + TIA (160x192 NTSC Video + 2ch Polynomial Audio + Collisions) + RIOT 6532 + F8/F6/F4 Bankswitching.
 */
class Atari2600Machine(
    private val romBytes: ByteArray,
    private val audioEngine: AtariAudioEngine
) {
    companion object {
        const val WIDTH = 160
        const val HEIGHT = 192
    }

    val frameBuffer = IntArray(WIDTH * HEIGHT)
    val ram = IntArray(128) // RIOT 6532 RAM ($80-$FF)

    private var currentBank = 0
    private val bankCount = when {
        romBytes.size <= 4096 -> 1
        romBytes.size <= 8192 -> 2
        romBytes.size <= 16384 -> 4
        else -> (romBytes.size / 4096).coerceAtLeast(1)
    }

    // Input state
    var p1Input = ControllerInput()
    var p2Input = ControllerInput()

    // TIA registers
    private var vsync = 0
    private var vblank = 0
    private var wsyncHalted = false
    private var nusiz0 = 0
    private var nusiz1 = 0
    private var colup0 = 0
    private var colup1 = 0
    private var colupf = 0
    private var colubk = 0
    private var ctrlpf = 0
    private var refp0 = 0
    private var refp1 = 0
    private var pf0 = 0
    private var pf1 = 0
    private var pf2 = 0
    private var p0x = 80
    private var p1x = 80
    private var m0x = 0
    private var m1x = 0
    private var blx = 0
    private var grp0 = 0
    private var grp1 = 0
    private var grp0Old = 0
    private var grp1Old = 0
    private var vdelp0 = 0
    private var vdelp1 = 0
    private var vdelbl = 0
    private var enam0 = 0
    private var enam1 = 0
    private var enabl = 0
    private var enablOld = 0
    private var resmp0 = 0
    private var resmp1 = 0
    private var hmp0 = 0
    private var hmp1 = 0
    private var hmm0 = 0
    private var hmm1 = 0
    private var hmbl = 0

    // Audio shadows
    private var audc0 = 0
    private var audf0 = 0
    private var audv0 = 0
    private var audc1 = 0
    private var audf1 = 0
    private var audv1 = 0

    // Collision latches
    private var cxm0p = 0
    private var cxm1p = 0
    private var cxp0fb = 0
    private var cxp1fb = 0
    private var cxm0fb = 0
    private var cxm1fb = 0
    private var cxblpf = 0
    private var cxppmm = 0

    // RIOT Timer
    private var riotTimer = 0xFF
    private var riotInterval = 64
    private var riotDivider = 0
    private var riotUnderflow = false

    // Scanline tracking
    private var cycleInLine = 0
    private var beamLine = 0
    private var frameEndedByVsync = false
    private var visibleLineStart = 37
    private var vblankClearedThisFrame = false

    // Active cheats: Map<RAM_Address_00_FF, Value>
    @Volatile var activeCheats: Map<Int, Int> = emptyMap()

    val cpu = Cpu6502(
        readMem = { addr -> readMemory(addr) },
        writeMem = { addr, value -> writeMemory(addr, value) }
    )

    init {
        reset()
    }

    fun reset() {
        ram.fill(0)
        frameBuffer.fill(0xFF000000.toInt())
        currentBank = 0
        vsync = 0
        vblank = 0
        wsyncHalted = false
        beamLine = 0
        visibleLineStart = 37
        vblankClearedThisFrame = false
        audc0 = 0; audf0 = 0; audv0 = 0
        audc1 = 0; audf1 = 0; audv1 = 0
        audioEngine.setChannel0(0, 0, 0)
        audioEngine.setChannel1(0, 0, 0)
        clearCollisions()
        riotTimer = 0xFF
        riotInterval = 64
        riotDivider = 0
        cpu.reset()
    }

    private fun clearCollisions() {
        cxm0p = 0; cxm1p = 0; cxp0fb = 0; cxp1fb = 0
        cxm0fb = 0; cxm1fb = 0; cxblpf = 0; cxppmm = 0
    }

    private fun readRom(offset: Int): Int {
        if (romBytes.isEmpty()) return 0xEA
        val bankBase = currentBank * 4096
        val inBank = if (romBytes.size <= 2048) offset and 0x07FF else offset and 0x0FFF
        val idx = (bankBase + inBank).coerceIn(0, romBytes.lastIndex)
        return romBytes[idx].toInt() and 0xFF
    }

    private fun checkBankSwitch(a13: Int) {
        val off = a13 and 0x0FFF
        when (bankCount) {
            2 -> if (off == 0xFF8) currentBank = 0 else if (off == 0xFF9) currentBank = 1
            4 -> if (off in 0xFF6..0xFF9) currentBank = off - 0xFF6
            8 -> if (off in 0xFF4..0xFFB) currentBank = off - 0xFF4
        }
    }

    private fun readMemory(addr: Int): Int {
        val a13 = addr and 0x1FFF
        if ((a13 and 0x1000) != 0) {
            checkBankSwitch(a13)
            return readRom(a13)
        }
        if ((a13 and 0x0080) == 0) {
            // TIA Read ($00-$0D)
            return when (a13 and 0x0F) {
                0x00 -> cxm0p
                0x01 -> cxm1p
                0x02 -> cxp0fb
                0x03 -> cxp1fb
                0x04 -> cxm0fb
                0x05 -> cxm1fb
                0x06 -> cxblpf
                0x07 -> cxppmm
                0x0C -> if (p1Input.fireA || p1Input.fireB) 0x00 else 0x80 // INPT4
                0x0D -> if (p2Input.fireA || p2Input.fireB) 0x00 else 0x80 // INPT5
                else -> 0x00
            }
        }
        if ((a13 and 0x0200) == 0) {
            val ramAddr = a13 and 0x7F
            return activeCheats[0x80 + ramAddr] ?: ram[ramAddr]
        }
        // RIOT I/O & Timer ($280-$297)
        return when (a13 and 0x07) {
            0x00 -> buildSwcha()
            0x02 -> buildSwchb()
            0x04, 0x06 -> {
                riotUnderflow = false
                riotTimer and 0xFF
            }
            0x05, 0x07 -> if (riotUnderflow) 0x80 else 0x00
            else -> 0xFF
        }
    }

    private fun writeMemory(addr: Int, value: Int) {
        val a13 = addr and 0x1FFF
        val v = value and 0xFF
        if ((a13 and 0x1000) != 0) {
            checkBankSwitch(a13)
            return
        }
        if ((a13 and 0x0080) == 0) {
            writeTia(a13 and 0x3F, v)
            return
        }
        if ((a13 and 0x0200) == 0) {
            val ramAddr = a13 and 0x7F
            ram[ramAddr] = activeCheats[0x80 + ramAddr] ?: v
            return
        }
        // RIOT Timer writes
        when (a13 and 0x1F) {
            0x14 -> { riotTimer = v; riotInterval = 1; riotDivider = 0; riotUnderflow = false }
            0x15 -> { riotTimer = v; riotInterval = 8; riotDivider = 0; riotUnderflow = false }
            0x16 -> { riotTimer = v; riotInterval = 64; riotDivider = 0; riotUnderflow = false }
            0x17 -> { riotTimer = v; riotInterval = 1024; riotDivider = 0; riotUnderflow = false }
        }
    }

    private fun buildSwcha(): Int {
        var res = 0xFF
        // P0 high nibble
        if (p1Input.up) res = res and 0xEF
        if (p1Input.down) res = res and 0xDF
        if (p1Input.left) res = res and 0xBF
        if (p1Input.right) res = res and 0x7F
        // P1 low nibble
        if (p2Input.up) res = res and 0xFE
        if (p2Input.down) res = res and 0xFD
        if (p2Input.left) res = res and 0xFB
        if (p2Input.right) res = res and 0xF7
        return res
    }

    private fun buildSwchb(): Int {
        var res = 0x0B // Color TV (bit 3=1), Difficulty A/B
        if (!p1Input.reset) res = res or 0x01 else res = res and 0xFE
        if (!p1Input.select && !p1Input.start) res = res or 0x02 else res = res and 0xFD
        return res
    }

    private fun currentVisibleX(): Int {
        val colorClocks = cycleInLine * 3
        return (colorClocks - 68 + 5).coerceIn(0, WIDTH - 1)
    }

    private fun writeTia(reg: Int, v: Int) {
        when (reg) {
            0x00 -> {
                val turningOn = (v and 0x02) != 0 && (vsync and 0x02) == 0
                val turningOff = (v and 0x02) == 0 && (vsync and 0x02) != 0
                if (turningOn && beamLine > 180) {
                    frameEndedByVsync = true
                }
                if (turningOff) {
                    beamLine = 0
                    vblankClearedThisFrame = false
                }
                vsync = v
            }
            0x01 -> {
                if ((v and 0x02) == 0 && (vblank and 0x02) != 0 && !vblankClearedThisFrame) {
                    visibleLineStart = beamLine.coerceIn(10, 60)
                    vblankClearedThisFrame = true
                }
                vblank = v
            }
            0x02 -> wsyncHalted = true // WSYNC
            0x04 -> nusiz0 = v
            0x05 -> nusiz1 = v
            0x06 -> colup0 = v
            0x07 -> colup1 = v
            0x08 -> colupf = v
            0x09 -> colubk = v
            0x0A -> ctrlpf = v
            0x0B -> refp0 = v
            0x0C -> refp1 = v
            0x0D -> pf0 = v
            0x0E -> pf1 = v
            0x0F -> pf2 = v
            0x10 -> p0x = currentVisibleX()
            0x11 -> p1x = currentVisibleX()
            0x12 -> m0x = currentVisibleX()
            0x13 -> m1x = currentVisibleX()
            0x14 -> blx = currentVisibleX()
            0x15 -> { audc0 = v; audioEngine.setChannel0(audc0, audf0, audv0) }
            0x16 -> { audc1 = v; audioEngine.setChannel1(audc1, audf1, audv1) }
            0x17 -> { audf0 = v; audioEngine.setChannel0(audc0, audf0, audv0) }
            0x18 -> { audf1 = v; audioEngine.setChannel1(audc1, audf1, audv1) }
            0x19 -> { audv0 = v; audioEngine.setChannel0(audc0, audf0, audv0) }
            0x1A -> { audv1 = v; audioEngine.setChannel1(audc1, audf1, audv1) }
            0x1B -> {
                grp0 = v
                grp1Old = grp1
            }
            0x1C -> {
                grp1 = v
                grp0Old = grp0
                enablOld = enabl
            }
            0x1D -> enam0 = v
            0x1E -> enam1 = v
            0x1F -> enabl = v
            0x20 -> hmp0 = (v.toByte().toInt() shr 4)
            0x21 -> hmp1 = (v.toByte().toInt() shr 4)
            0x22 -> hmm0 = (v.toByte().toInt() shr 4)
            0x23 -> hmm1 = (v.toByte().toInt() shr 4)
            0x24 -> hmbl = (v.toByte().toInt() shr 4)
            0x25 -> vdelp0 = v
            0x26 -> vdelp1 = v
            0x27 -> vdelbl = v
            0x28 -> resmp0 = v
            0x29 -> resmp1 = v
            0x2A -> { // HMOVE
                p0x = (p0x - hmp0).coerceIn(0, WIDTH - 1)
                p1x = (p1x - hmp1).coerceIn(0, WIDTH - 1)
                m0x = (m0x - hmm0).coerceIn(0, WIDTH - 1)
                m1x = (m1x - hmm1).coerceIn(0, WIDTH - 1)
                blx = (blx - hmbl).coerceIn(0, WIDTH - 1)
            }
            0x2B -> {
                hmp0 = 0; hmp1 = 0; hmm0 = 0; hmm1 = 0; hmbl = 0
            }
            0x2C -> clearCollisions()
        }
    }

    private fun tickRiotTimer(cpuCycles: Int) {
        for (i in 0 until cpuCycles) {
            if (riotUnderflow) {
                riotTimer = (riotTimer - 1) and 0xFF
            } else {
                riotDivider++
                if (riotDivider >= riotInterval) {
                    riotDivider = 0
                    riotTimer--
                    if (riotTimer < 0) {
                        riotTimer = 0xFF
                        riotUnderflow = true
                    }
                }
            }
        }
    }

    fun stepFrame() {
        frameEndedByVsync = false
        var safetyLines = 0
        while (!frameEndedByVsync && safetyLines < 312) {
            cycleInLine = 0
            wsyncHalted = false
            while (cycleInLine < 76) {
                if (wsyncHalted) {
                    val rem = 76 - cycleInLine
                    tickRiotTimer(rem)
                    cycleInLine = 76
                } else {
                    val cyc = cpu.step()
                    cycleInLine += cyc
                    tickRiotTimer(cyc)
                }
            }
            val visibleY = beamLine - visibleLineStart
            if (visibleY in 0 until HEIGHT) {
                renderScanline(visibleY)
            }
            beamLine++
            safetyLines++
        }
    }

    private fun renderScanline(y: Int) {
        val rowOffset = y * WIDTH
        if ((vblank and 0x02) != 0) {
            val blk = 0xFF000000.toInt()
            for (x in 0 until WIDTH) frameBuffer[rowOffset + x] = blk
            return
        }

        val bgArgb = AtariNTSCColors.color(colubk)
        val pfArgb = AtariNTSCColors.color(colupf)
        val p0Argb = AtariNTSCColors.color(colup0)
        val p1Argb = AtariNTSCColors.color(colup1)
        val reflectPf = (ctrlpf and 0x01) != 0
        val scoreMode = (ctrlpf and 0x02) != 0
        val pfPriority = (ctrlpf and 0x04) != 0

        val g0 = if ((vdelp0 and 1) != 0) grp0Old else grp0
        val g1 = if ((vdelp1 and 1) != 0) grp1Old else grp1
        val ballOn = ((if ((vdelbl and 1) != 0) enablOld else enabl) and 0x02) != 0
        val m0On = (enam0 and 0x02) != 0 && (resmp0 and 0x02) == 0
        val m1On = (enam1 and 0x02) != 0 && (resmp1 and 0x02) == 0

        val p0Scale = when (nusiz0 and 0x07) {
            5 -> 2
            7 -> 4
            else -> 1
        }
        val p1Scale = when (nusiz1 and 0x07) {
            5 -> 2
            7 -> 4
            else -> 1
        }

        for (x in 0 until WIDTH) {
            val pfBit = samplePlayfieldBit(x, reflectPf)
            val p0Bit = samplePlayerBit(x, p0x, g0, (refp0 and 0x08) != 0, p0Scale, nusiz0 and 0x07)
            val p1Bit = samplePlayerBit(x, p1x, g1, (refp1 and 0x08) != 0, p1Scale, nusiz1 and 0x07)
            val m0Bit = m0On && x in m0x until (m0x + 2)
            val m1Bit = m1On && x in m1x until (m1x + 2)
            val blBit = ballOn && x in blx until (blx + 2)

            // Update hardware collisions
            if (m0Bit && p1Bit) cxm0p = cxm0p or 0x80
            if (m0Bit && p0Bit) cxm0p = cxm0p or 0x40
            if (m1Bit && p0Bit) cxm1p = cxm1p or 0x80
            if (m1Bit && p1Bit) cxm1p = cxm1p or 0x40
            if (p0Bit && pfBit) cxp0fb = cxp0fb or 0x80
            if (p0Bit && blBit) cxp0fb = cxp0fb or 0x40
            if (p1Bit && pfBit) cxp1fb = cxp1fb or 0x80
            if (p1Bit && blBit) cxp1fb = cxp1fb or 0x40
            if (m0Bit && pfBit) cxm0fb = cxm0fb or 0x80
            if (m1Bit && pfBit) cxm1fb = cxm1fb or 0x80
            if (blBit && pfBit) cxblpf = cxblpf or 0x80
            if (p0Bit && p1Bit) cxppmm = cxppmm or 0x80
            if (m0Bit && m1Bit) cxppmm = cxppmm or 0x40

            val pfColor = if (scoreMode) (if (x < 80) p0Argb else p1Argb) else pfArgb
            val pixel = if (pfPriority) {
                when {
                    pfBit || blBit -> pfColor
                    p0Bit || m0Bit -> p0Argb
                    p1Bit || m1Bit -> p1Argb
                    else -> bgArgb
                }
            } else {
                when {
                    p0Bit || m0Bit -> p0Argb
                    p1Bit || m1Bit -> p1Argb
                    pfBit || blBit -> pfColor
                    else -> bgArgb
                }
            }
            frameBuffer[rowOffset + x] = pixel
        }
    }

    private fun samplePlayfieldBit(x: Int, reflect: Boolean): Boolean {
        val pfPix = x / 4 // 0..39
        val col = if (pfPix < 20) pfPix else if (reflect) 39 - pfPix else pfPix - 20
        return when {
            col < 4 -> (pf0 and (1 shl (4 + col))) != 0
            col < 12 -> (pf1 and (1 shl (11 - col))) != 0
            else -> (pf2 and (1 shl (col - 12))) != 0
        }
    }

    private fun samplePlayerBit(
        x: Int,
        px: Int,
        grp: Int,
        reflect: Boolean,
        scale: Int,
        nusiz: Int
    ): Boolean {
        if (grp == 0) return false
        val width = 8 * scale
        val offsets = when (nusiz) {
            1 -> intArrayOf(0, 16)
            2 -> intArrayOf(0, 32)
            3 -> intArrayOf(0, 16, 32)
            4 -> intArrayOf(0, 64)
            6 -> intArrayOf(0, 32, 64)
            else -> intArrayOf(0)
        }
        for (off in offsets) {
            val rel = x - (px + off)
            if (rel in 0 until width) {
                val bitIdx = rel / scale
                val mask = if (reflect) (1 shl bitIdx) else (0x80 ushr bitIdx)
                if ((grp and mask) != 0) return true
            }
        }
        return false
    }

    fun serializeState(): ByteArray {
        val buf = ByteBuffer.allocate(1024)
        cpu.writeState(buf)
        buf.putInt(currentBank)
        for (b in ram) buf.putInt(b)
        buf.putInt(p0x); buf.putInt(p1x); buf.putInt(m0x); buf.putInt(m1x); buf.putInt(blx)
        buf.putInt(colup0); buf.putInt(colup1); buf.putInt(colupf); buf.putInt(colubk)
        buf.putInt(ctrlpf); buf.putInt(refp0); buf.putInt(refp1)
        buf.putInt(pf0); buf.putInt(pf1); buf.putInt(pf2)
        buf.putInt(grp0); buf.putInt(grp1); buf.putInt(nusiz0); buf.putInt(nusiz1)
        buf.putInt(riotTimer); buf.putInt(riotInterval); buf.putInt(riotDivider)
        val arr = ByteArray(buf.position())
        buf.flip()
        buf.get(arr)
        return arr
    }

    fun deserializeState(bytes: ByteArray) {
        val buf = ByteBuffer.wrap(bytes)
        cpu.readState(buf)
        currentBank = buf.int
        for (i in ram.indices) ram[i] = buf.int
        p0x = buf.int; p1x = buf.int; m0x = buf.int; m1x = buf.int; blx = buf.int
        colup0 = buf.int; colup1 = buf.int; colupf = buf.int; colubk = buf.int
        ctrlpf = buf.int; refp0 = buf.int; refp1 = buf.int
        pf0 = buf.int; pf1 = buf.int; pf2 = buf.int
        grp0 = buf.int; grp1 = buf.int; nusiz0 = buf.int; nusiz1 = buf.int
        riotTimer = buf.int; riotInterval = buf.int; riotDivider = buf.int
    }
}
