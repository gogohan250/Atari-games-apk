package com.example.data.prefs

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "atari_settings")

enum class CrtFilterMode { CRT, SCANLINES, SMOOTH, PIXEL_PERFECT }
enum class AspectRatioMode { ORIGINAL, FILL, STRETCH }
enum class PerformanceMode { LOW, MEDIUM, HIGH }

data class GamepadMapping(
    val upKeyCode: Int = KeyEvent.KEYCODE_DPAD_UP,
    val downKeyCode: Int = KeyEvent.KEYCODE_DPAD_DOWN,
    val leftKeyCode: Int = KeyEvent.KEYCODE_DPAD_LEFT,
    val rightKeyCode: Int = KeyEvent.KEYCODE_DPAD_RIGHT,
    val fireAKeyCode: Int = KeyEvent.KEYCODE_BUTTON_A,
    val fireBKeyCode: Int = KeyEvent.KEYCODE_BUTTON_B,
    val selectKeyCode: Int = KeyEvent.KEYCODE_BUTTON_SELECT,
    val startKeyCode: Int = KeyEvent.KEYCODE_BUTTON_START,
    val resetKeyCode: Int = KeyEvent.KEYCODE_BUTTON_X,
    val quickSaveKeyCode: Int = KeyEvent.KEYCODE_BUTTON_L1,
    val quickLoadKeyCode: Int = KeyEvent.KEYCODE_BUTTON_R1,
    val fastForwardKeyCode: Int = KeyEvent.KEYCODE_BUTTON_R2,
    val rewindKeyCode: Int = KeyEvent.KEYCODE_BUTTON_L2
) {
    fun toJson(): String = JSONObject().apply {
        put("up", upKeyCode)
        put("down", downKeyCode)
        put("left", leftKeyCode)
        put("right", rightKeyCode)
        put("a", fireAKeyCode)
        put("b", fireBKeyCode)
        put("select", selectKeyCode)
        put("start", startKeyCode)
        put("reset", resetKeyCode)
        put("qsave", quickSaveKeyCode)
        put("qload", quickLoadKeyCode)
        put("ff", fastForwardKeyCode)
        put("rewind", rewindKeyCode)
    }.toString()

    companion object {
        fun fromJson(json: String?): GamepadMapping {
            if (json.isNullOrBlank()) return GamepadMapping()
            return try {
                val obj = JSONObject(json)
                GamepadMapping(
                    upKeyCode = obj.optInt("up", KeyEvent.KEYCODE_DPAD_UP),
                    downKeyCode = obj.optInt("down", KeyEvent.KEYCODE_DPAD_DOWN),
                    leftKeyCode = obj.optInt("left", KeyEvent.KEYCODE_DPAD_LEFT),
                    rightKeyCode = obj.optInt("right", KeyEvent.KEYCODE_DPAD_RIGHT),
                    fireAKeyCode = obj.optInt("a", KeyEvent.KEYCODE_BUTTON_A),
                    fireBKeyCode = obj.optInt("b", KeyEvent.KEYCODE_BUTTON_B),
                    selectKeyCode = obj.optInt("select", KeyEvent.KEYCODE_BUTTON_SELECT),
                    startKeyCode = obj.optInt("start", KeyEvent.KEYCODE_BUTTON_START),
                    resetKeyCode = obj.optInt("reset", KeyEvent.KEYCODE_BUTTON_X),
                    quickSaveKeyCode = obj.optInt("qsave", KeyEvent.KEYCODE_BUTTON_L1),
                    quickLoadKeyCode = obj.optInt("qload", KeyEvent.KEYCODE_BUTTON_R1),
                    fastForwardKeyCode = obj.optInt("ff", KeyEvent.KEYCODE_BUTTON_R2),
                    rewindKeyCode = obj.optInt("rewind", KeyEvent.KEYCODE_BUTTON_L2)
                )
            } catch (_: Exception) {
                GamepadMapping()
            }
        }
    }
}

data class AtariSettings(
    val isArabic: Boolean = true,
    val themeStyle: String = "DARK",
    val crtFilter: CrtFilterMode = CrtFilterMode.PIXEL_PERFECT,
    val aspectRatio: AspectRatioMode = AspectRatioMode.ORIGINAL,
    val imageEnhance: Boolean = false,
    val audioEnabled: Boolean = true,
    val audioVolume: Float = 0.85f,
    val hapticsEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val performanceMode: PerformanceMode = PerformanceMode.HIGH,
    val watchDownloadsFolder: Boolean = false,
    val watchedTreeUri: String = "",
    val controlsOpacity: Float = 0.88f,
    val controlsScale: Float = 1.0f,
    val dpadScale: Float = 1.0f,
    val aBtnScale: Float = 1.0f,
    val bBtnScale: Float = 1.0f,
    val useAnalogJoystick: Boolean = true,
    val hideTouchOnGamepad: Boolean = false,
    val dpadOffsetX: Float = 0f,
    val dpadOffsetY: Float = 0f,
    val buttonsOffsetX: Float = 0f,
    val buttonsOffsetY: Float = 0f,
    val aBtnOffsetX: Float = 0f,
    val aBtnOffsetY: Float = 0f,
    val bBtnOffsetX: Float = 0f,
    val bBtnOffsetY: Float = 0f,
    val sysBtnsOffsetX: Float = 0f,
    val sysBtnsOffsetY: Float = 0f,
    val twoPlayerMode: Boolean = false,
    val gamepadMapping: GamepadMapping = GamepadMapping(),
    val telegramSubscribed: Boolean = false
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val IS_ARABIC = booleanPreferencesKey("is_arabic")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val CRT_FILTER = stringPreferencesKey("crt_filter")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val IMAGE_ENHANCE = booleanPreferencesKey("image_enhance")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val AUDIO_VOLUME = floatPreferencesKey("audio_volume")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val PERFORMANCE = stringPreferencesKey("performance_mode")
        val WATCH_DOWNLOADS = booleanPreferencesKey("watch_downloads")
        val WATCHED_TREE_URI = stringPreferencesKey("watched_tree_uri")
        val CONTROLS_OPACITY = floatPreferencesKey("controls_opacity")
        val CONTROLS_SCALE = floatPreferencesKey("controls_scale")
        val DPAD_SCALE = floatPreferencesKey("dpad_scale")
        val A_BTN_SCALE = floatPreferencesKey("a_btn_scale")
        val B_BTN_SCALE = floatPreferencesKey("b_btn_scale")
        val USE_ANALOG_JOYSTICK = booleanPreferencesKey("use_analog_joystick")
        val HIDE_TOUCH_ON_GAMEPAD = booleanPreferencesKey("hide_touch_on_gamepad")
        val DPAD_OFFSET_X = floatPreferencesKey("dpad_offset_x")
        val DPAD_OFFSET_Y = floatPreferencesKey("dpad_offset_y")
        val BTNS_OFFSET_X = floatPreferencesKey("btns_offset_x")
        val BTNS_OFFSET_Y = floatPreferencesKey("btns_offset_y")
        val A_BTN_OFFSET_X = floatPreferencesKey("a_btn_offset_x")
        val A_BTN_OFFSET_Y = floatPreferencesKey("a_btn_offset_y")
        val B_BTN_OFFSET_X = floatPreferencesKey("b_btn_offset_x")
        val B_BTN_OFFSET_Y = floatPreferencesKey("b_btn_offset_y")
        val SYS_BTNS_OFFSET_X = floatPreferencesKey("sys_btns_offset_x")
        val SYS_BTNS_OFFSET_Y = floatPreferencesKey("sys_btns_offset_y")
        val TWO_PLAYER = booleanPreferencesKey("two_player_mode")
        val GAMEPAD_MAPPING = stringPreferencesKey("gamepad_mapping")
        val TELEGRAM_SUBSCRIBED = booleanPreferencesKey("telegram_subscribed")
    }

    private fun readFromPreferences(p: Preferences): AtariSettings = AtariSettings(
        isArabic = p[Keys.IS_ARABIC] ?: true,
        themeStyle = p[Keys.THEME_STYLE] ?: "DARK",
        crtFilter = runCatching { CrtFilterMode.valueOf(p[Keys.CRT_FILTER] ?: "PIXEL_PERFECT") }.getOrDefault(CrtFilterMode.PIXEL_PERFECT),
        aspectRatio = runCatching { AspectRatioMode.valueOf(p[Keys.ASPECT_RATIO] ?: "ORIGINAL") }.getOrDefault(AspectRatioMode.ORIGINAL),
        imageEnhance = p[Keys.IMAGE_ENHANCE] ?: false,
        audioEnabled = p[Keys.AUDIO_ENABLED] ?: true,
        audioVolume = p[Keys.AUDIO_VOLUME] ?: 0.85f,
        hapticsEnabled = p[Keys.HAPTICS_ENABLED] ?: true,
        autoSaveEnabled = p[Keys.AUTO_SAVE] ?: true,
        keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: true,
        performanceMode = runCatching { PerformanceMode.valueOf(p[Keys.PERFORMANCE] ?: "HIGH") }.getOrDefault(PerformanceMode.HIGH),
        watchDownloadsFolder = p[Keys.WATCH_DOWNLOADS] ?: false,
        watchedTreeUri = p[Keys.WATCHED_TREE_URI] ?: "",
        controlsOpacity = p[Keys.CONTROLS_OPACITY] ?: 0.88f,
        controlsScale = p[Keys.CONTROLS_SCALE] ?: 1.0f,
        dpadScale = p[Keys.DPAD_SCALE] ?: 1.0f,
        aBtnScale = p[Keys.A_BTN_SCALE] ?: 1.0f,
        bBtnScale = p[Keys.B_BTN_SCALE] ?: 1.0f,
        useAnalogJoystick = p[Keys.USE_ANALOG_JOYSTICK] ?: true,
        hideTouchOnGamepad = p[Keys.HIDE_TOUCH_ON_GAMEPAD] ?: false,
        dpadOffsetX = p[Keys.DPAD_OFFSET_X] ?: 0f,
        dpadOffsetY = p[Keys.DPAD_OFFSET_Y] ?: 0f,
        buttonsOffsetX = p[Keys.BTNS_OFFSET_X] ?: 0f,
        buttonsOffsetY = p[Keys.BTNS_OFFSET_Y] ?: 0f,
        aBtnOffsetX = p[Keys.A_BTN_OFFSET_X] ?: 0f,
        aBtnOffsetY = p[Keys.A_BTN_OFFSET_Y] ?: 0f,
        bBtnOffsetX = p[Keys.B_BTN_OFFSET_X] ?: 0f,
        bBtnOffsetY = p[Keys.B_BTN_OFFSET_Y] ?: 0f,
        sysBtnsOffsetX = p[Keys.SYS_BTNS_OFFSET_X] ?: 0f,
        sysBtnsOffsetY = p[Keys.SYS_BTNS_OFFSET_Y] ?: 0f,
        twoPlayerMode = p[Keys.TWO_PLAYER] ?: false,
        gamepadMapping = GamepadMapping.fromJson(p[Keys.GAMEPAD_MAPPING]),
        telegramSubscribed = p[Keys.TELEGRAM_SUBSCRIBED] ?: false
    )

    val settingsFlow: Flow<AtariSettings> = context.dataStore.data.map(::readFromPreferences)

    suspend fun update(transform: (AtariSettings) -> AtariSettings) {
        context.dataStore.edit { p ->
            val next = transform(readFromPreferences(p))
            p[Keys.IS_ARABIC] = next.isArabic
            p[Keys.THEME_STYLE] = next.themeStyle
            p[Keys.CRT_FILTER] = next.crtFilter.name
            p[Keys.ASPECT_RATIO] = next.aspectRatio.name
            p[Keys.IMAGE_ENHANCE] = next.imageEnhance
            p[Keys.AUDIO_ENABLED] = next.audioEnabled
            p[Keys.AUDIO_VOLUME] = next.audioVolume
            p[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
            p[Keys.AUTO_SAVE] = next.autoSaveEnabled
            p[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            p[Keys.PERFORMANCE] = next.performanceMode.name
            p[Keys.WATCH_DOWNLOADS] = next.watchDownloadsFolder
            p[Keys.WATCHED_TREE_URI] = next.watchedTreeUri
            p[Keys.CONTROLS_OPACITY] = next.controlsOpacity
            p[Keys.CONTROLS_SCALE] = next.controlsScale
            p[Keys.DPAD_SCALE] = next.dpadScale
            p[Keys.A_BTN_SCALE] = next.aBtnScale
            p[Keys.B_BTN_SCALE] = next.bBtnScale
            p[Keys.USE_ANALOG_JOYSTICK] = next.useAnalogJoystick
            p[Keys.HIDE_TOUCH_ON_GAMEPAD] = next.hideTouchOnGamepad
            p[Keys.DPAD_OFFSET_X] = next.dpadOffsetX
            p[Keys.DPAD_OFFSET_Y] = next.dpadOffsetY
            p[Keys.BTNS_OFFSET_X] = next.buttonsOffsetX
            p[Keys.BTNS_OFFSET_Y] = next.buttonsOffsetY
            p[Keys.A_BTN_OFFSET_X] = next.aBtnOffsetX
            p[Keys.A_BTN_OFFSET_Y] = next.aBtnOffsetY
            p[Keys.B_BTN_OFFSET_X] = next.bBtnOffsetX
            p[Keys.B_BTN_OFFSET_Y] = next.bBtnOffsetY
            p[Keys.SYS_BTNS_OFFSET_X] = next.sysBtnsOffsetX
            p[Keys.SYS_BTNS_OFFSET_Y] = next.sysBtnsOffsetY
            p[Keys.TWO_PLAYER] = next.twoPlayerMode
            p[Keys.GAMEPAD_MAPPING] = next.gamepadMapping.toJson()
            p[Keys.TELEGRAM_SUBSCRIBED] = next.telegramSubscribed
        }
    }
}
