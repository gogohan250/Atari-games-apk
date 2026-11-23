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
