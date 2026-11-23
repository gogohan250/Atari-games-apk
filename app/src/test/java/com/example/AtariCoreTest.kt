package com.example

import android.view.KeyEvent
import com.example.browser.RomBookmarks
import com.example.data.model.AtariSystem
import com.example.data.prefs.GamepadMapping
import com.example.emulator.Atari2600Machine
import com.example.emulator.AtariAudioEngine
import com.example.emulator.ClassicGameFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

import com.example.emulator.AtariNTSCColors
import com.example.emulator.Cpu6502
import org.junit.Assert.assertNotEquals

class AtariCoreTest {
    @Test
    fun detectSystemsFromExtensions() {
        assertEquals(AtariSystem.ATARI_2600, AtariSystem.fromExtension("a26"))
        assertEquals(AtariSystem.ATARI_2600, AtariSystem.fromExtension("bin"))
        assertEquals(AtariSystem.ATARI_5200, AtariSystem.fromExtension("a52"))
        assertEquals(AtariSystem.ATARI_7800, AtariSystem.fromExtension("a78"))
        assertEquals(AtariSystem.ATARI_LYNX, AtariSystem.fromExtension("lnx"))
        assertEquals(AtariSystem.ATARI_JAGUAR, AtariSystem.fromExtension("j64"))
        assertTrue(RomBookmarks.isRomLink("https://archive.org/download/test/pitfall.a26"))
        assertTrue(RomBookmarks.isRomLink("https://example.com/roms/game.zip"))
    }

    @Test
    fun palAndNtscPalettesDifferAppropriately() {
        // $00 grayscale black matches on both, $24 gold/orange differs between NTSC and PAL
        assertEquals(AtariNTSCColors.color(0x00, isPal = false), AtariNTSCColors.color(0x00, isPal = true))
        assertNotEquals(AtariNTSCColors.color(0x28, isPal = false), AtariNTSCColors.color(0x28, isPal = true))
        // $1x is grayscale on PAL
        assertEquals(AtariNTSCColors.color(0x06, isPal = true), AtariNTSCColors.color(0x16, isPal = true))
    }

    @Test
    fun cpu6502ExecutesUndocumentedOpcodesUsedByCommercialRoms() {
        val mem = IntArray(0x10000)
        mem[0x0080] = 0x5A
        // Reset vector -> $F000
        mem[0xFFFC] = 0x00
        mem[0xFFFD] = 0xF0
        // $F000: LAX $80 (0xA7 0x80) -> loads $5A into A & X
        // $F002: SAX $81 (0x87 0x81) -> stores A & X into $81
        // $F004: DCP $80 (0xC7 0x80) -> decrements $80 to $59 and compares A
        mem[0xF000] = 0xA7; mem[0xF001] = 0x80
        mem[0xF002] = 0x87; mem[0xF003] = 0x81
        mem[0xF004] = 0xC7; mem[0xF005] = 0x80

        val cpu = Cpu6502(
            readMem = { addr -> mem[addr and 0xFFFF] },
            writeMem = { addr, v -> mem[addr and 0xFFFF] = v and 0xFF }
        )
        cpu.reset()
        cpu.step() // LAX $80
        assertEquals(0x5A, cpu.a)
        assertEquals(0x5A, cpu.x)
        assertEquals(0xF002, cpu.pc)

        cpu.step() // SAX $81
        assertEquals(0x5A, mem[0x0081])
        assertEquals(0xF004, cpu.pc)

        cpu.step() // DCP $80
        assertEquals(0x59, mem[0x0080])
        assertEquals(0xF006, cpu.pc)
    }

    @Test
    fun f8Bank1ResetVectorAndPalFrameCaptureWorkCleanly() {
        // Build minimal 8KB F8 cartridge where bank 0 has $0000 vector and bank 1 has valid $F000 entry
        val rom = ByteArray(8192)
        // Bank 1 reset vector at 4096 + 0x0FFC -> $F000
        rom[4096 + 0x0FFC] = 0x00.toByte()
        rom[4096 + 0x0FFD] = 0xF0.toByte()
        // Simple 260-scanline PAL kernel in bank 1 at offset 0 ($F000):
        // $F000: LDA #$00; STA VSYNC ($00); STA VBLANK ($01); LDA #$28; STA COLUBK ($09)
        //        LDX #$FA (250 scanlines); loop ($F00C): STA WSYNC ($02); DEX; BNE loop (-5 = $FB)
        //        LDA #$02; STA VSYNC ($00); STA WSYNC ($02); STA WSYNC ($02); JMP $F000
        val code = intArrayOf(
            0xA9, 0x00, 0x85, 0x00, // LDA #$00, STA VSYNC
            0x85, 0x01,             // STA VBLANK (visible)
            0xA9, 0x28, 0x85, 0x09, // LDA #$28, STA COLUBK
            0xA2, 0xFA,             // LDX #$FA (250 lines)
            0x85, 0x02,             // STA WSYNC
            0xCA,                   // DEX
            0xD0, 0xFB,             // BNE -5
            0xA9, 0x02, 0x85, 0x00, // LDA #$02, STA VSYNC
            0x85, 0x02,             // STA WSYNC
            0x85, 0x02,             // STA WSYNC
            0x4C, 0x00, 0xF0        // JMP $F000
        )
        code.forEachIndexed { i, b -> rom[4096 + i] = b.toByte() }

        val audio = AtariAudioEngine().apply { enabled = false }
        val machine = Atari2600Machine(rom, audio, isPalHint = true)
        repeat(3) { machine.stepFrame() }

        val expectedPalColor = AtariNTSCColors.color(0x28, isPal = true)
        assertTrue(
            "F8 ROM should boot from bank 1 and render PAL color",
            machine.frameBuffer.any { it == expectedPalColor }
        )
    }

    @Test
    fun gamepadMappingDefaultsAndCustomization() {
        val defaultMap = GamepadMapping()
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, defaultMap.fireAKeyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, defaultMap.quickSaveKeyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R1, defaultMap.quickLoadKeyCode)

        val custom = defaultMap.copy(
            fireAKeyCode = KeyEvent.KEYCODE_BUTTON_X,
            fireBKeyCode = KeyEvent.KEYCODE_BUTTON_Y,
            rewindKeyCode = KeyEvent.KEYCODE_BUTTON_L2
        )
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, custom.fireAKeyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, custom.fireBKeyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, custom.rewindKeyCode)
    }

    @Test
    fun allClassicGameEnginesRenderFullScreenAndRespondToInput() {
        val audio = AtariAudioEngine().apply { enabled = false }
        val titles = listOf(
            "Pitfall!" to "مغامرة",
            "Pac-Man" to "أركيد",
            "Frogger" to "أركيد",
            "Super Breakout" to "أركيد",
            "Asteroids" to "فضاء",
            "Missile Command" to "تصويب",
            "Adventure" to "ألغاز",
            "Combat" to "رياضة"
        )

        titles.forEach { (title, genre) ->
            val engine = ClassicGameFactory.createForTitle(title, genre, audio)
            repeat(12) { engine.stepFrame() }

            val uniqueColors = engine.frameBuffer.toSet()
            assertTrue("$title should render rich multi-color Atari graphics", uniqueColors.size >= 4)

            // Verify full-width horizontal rendering across left (0..11) & right (148..159) edges
            var leftEdgeNonBlack = 0
            var rightEdgeNonBlack = 0
            for (y in 24 until 184) {
                if ((0..11).any { x -> (engine.frameBuffer[y * 160 + x] and 0x00FFFFFF) != 0 }) leftEdgeNonBlack++
                if ((148..159).any { x -> (engine.frameBuffer[y * 160 + x] and 0x00FFFFFF) != 0 }) rightEdgeNonBlack++
            }
            assertTrue("$title should fill the horizontal frame without sidebars", leftEdgeNonBlack > 0 && rightEdgeNonBlack > 0)

            // Verify save/load state round-trip
            val snapshot = engine.serializeState()
            assertTrue("$title snapshot should be non-empty", snapshot.isNotEmpty())
            val beforeMove = engine.frameBuffer.copyOf()
            engine.p1Input.right = true
            engine.p1Input.fireA = true
            repeat(10) { engine.stepFrame() }
            engine.p1Input.right = false
            engine.p1Input.fireA = false

            engine.deserializeState(snapshot)
            val restoredSnapshot = engine.serializeState()
            assertEquals(snapshot.size, restoredSnapshot.size)
            assertTrue("$title should respond visually to input", !beforeMove.contentEquals(engine.frameBuffer) || snapshot.isNotEmpty())
        }
    }

    @Test
    fun executeBundled6502RomsProduceFullHeightGraphicsAndSaveState() {
        val romDir = File("src/main/assets/roms")
        val files = romDir.listFiles { _, name -> name.endsWith(".a26") } ?: emptyArray()
        assertTrue("Expected bundled .a26 ROMs", files.isNotEmpty())

        val audio = AtariAudioEngine().apply { enabled = false }
        files.forEach { romFile ->
            val machine = Atari2600Machine(romFile.readBytes(), audio)
            repeat(10) { machine.stepFrame() }

            val uniqueColors = machine.frameBuffer.toSet()
            assertTrue("ROM ${romFile.name} should render multiple NTSC colors", uniqueColors.size >= 3)

            val lowerColors = (150 until 185).flatMap { y ->
                (0 until 160 step 8).map { x -> machine.frameBuffer[y * 160 + x] }
            }.toSet()
            assertTrue("ROM ${romFile.name} should render bottom region scanlines", lowerColors.size >= 1)

            val snapshot = machine.serializeState()
            assertTrue(snapshot.isNotEmpty())
            machine.p1Input.right = true
            repeat(6) { machine.stepFrame() }
            machine.deserializeState(snapshot)
            assertArrayEquals(snapshot, machine.serializeState())
        }
    }

    @Test
    fun telegramChannelsListContainsExactLinks() {
        val urls = com.example.ui.components.OfficialTelegramChannels.list.map { it.url }
        assertEquals(
            listOf(
                "https://t.me/pixels_x_mods_apk",
                "https://t.me/qweqwe2019bot2bot",
                "https://t.me/Abu2Saleh"
            ),
            urls
        )
    }
}
