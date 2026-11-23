package com.example.emulator

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.data.prefs.GamepadMapping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class GamepadQuickAction {
    QUICK_SAVE,
    QUICK_LOAD,
    FAST_FORWARD_START,
    FAST_FORWARD_STOP,
    REWIND
}

/**
 * Full Bluetooth & USB Gamepad manager:
 * - Monitors connected PS / Xbox / 8BitDo / generic HID controllers via InputManager
 * - Captures raw key & trigger presses for the button remapper dialog
 * - Routes D-pad, analog sticks, triggers, and remapped buttons to P1 & P2 ControllerInput
 */
class GamepadInputHandler(context: Context) {
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

    private val _connectedGamepadName = MutableStateFlow<String?>(null)
    val connectedGamepadName: StateFlow<String?> = _connectedGamepadName.asStateFlow()

    private val _connectedGamepadCount = MutableStateFlow(0)
    val connectedGamepadCount: StateFlow<Int> = _connectedGamepadCount.asStateFlow()

    private var activeDeviceIds: List<Int> = emptyList()

    // Track trigger & analog thresholds to emit edge events cleanly
    private var lastL2Pressed = false
    private var lastR2Pressed = false

    @Volatile
    var captureKeyCallback: ((Int) -> Unit)? = null

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refreshConnectedDevices()
        override fun onInputDeviceRemoved(deviceId: Int) = refreshConnectedDevices()
        override fun onInputDeviceChanged(deviceId: Int) = refreshConnectedDevices()
    }

    init {
        refreshConnectedDevices()
        inputManager.registerInputDeviceListener(deviceListener, Handler(Looper.getMainLooper()))
    }

    fun release() {
        runCatching { inputManager.unregisterInputDeviceListener(deviceListener) }
    }

    fun refreshConnectedDevices() {
        val gamepads = mutableListOf<Pair<Int, String>>()
        for (id in InputDevice.getDeviceIds()) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if ((isGamepad || isJoystick) && !dev.isVirtual) {
                gamepads += id to (dev.name ?: "Gamepad")
            }
        }
        activeDeviceIds = gamepads.map { it.first }
        _connectedGamepadCount.value = gamepads.size
        _connectedGamepadName.value = gamepads.firstOrNull()?.second
    }

    private fun targetInputForDevice(deviceId: Int, emulator: EmulatorController): ControllerInput {
        val idx = activeDeviceIds.indexOf(deviceId)
        return if (idx >= 1) emulator.p2Input else emulator.p1Input
    }

    fun handleKeyEvent(
        ev: KeyEvent,
        mapping: GamepadMapping,
        emulator: EmulatorController,
        isPlayerActive: Boolean,
        onQuickAction: (GamepadQuickAction) -> Unit
    ): Boolean {
        val code = ev.keyCode
        val isDown = ev.action == KeyEvent.ACTION_DOWN
        val isUp = ev.action == KeyEvent.ACTION_UP
        if (!isDown && !isUp) return false

        // Ignore system volume/back/home keys so Android handles them normally
        if (code == KeyEvent.KEYCODE_BACK ||
            code == KeyEvent.KEYCODE_VOLUME_UP ||
            code == KeyEvent.KEYCODE_VOLUME_DOWN ||
            code == KeyEvent.KEYCODE_VOLUME_MUTE ||
            code == KeyEvent.KEYCODE_POWER
        ) {
            return false
        }

        // Capture mode for Gamepad Mapping dialog
        val capture = captureKeyCallback
        if (capture != null && isDown && ev.repeatCount == 0) {
            captureKeyCallback = null
            capture(code)
            return true
        }

        if (!isPlayerActive) return false

        val target = targetInputForDevice(ev.deviceId, emulator)

        return when (code) {
            mapping.upKeyCode, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                target.up = isDown; true
            }
            mapping.downKeyCode, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                target.down = isDown; true
            }
            mapping.leftKeyCode, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                target.left = isDown; true
            }
            mapping.rightKeyCode, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                target.right = isDown; true
            }
            mapping.fireAKeyCode, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_1 -> {
                target.fireA = isDown; true
            }
            mapping.fireBKeyCode, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_BUTTON_2 -> {
                target.fireB = isDown; true
            }
            mapping.selectKeyCode -> {
                target.select = isDown; true
            }
            mapping.startKeyCode, KeyEvent.KEYCODE_ENTER -> {
                target.start = isDown; true
            }
            mapping.resetKeyCode -> {
                target.reset = isDown; true
            }
            mapping.quickSaveKeyCode -> {
                if (isDown && ev.repeatCount == 0) onQuickAction(GamepadQuickAction.QUICK_SAVE)
                true
            }
            mapping.quickLoadKeyCode -> {
                if (isDown && ev.repeatCount == 0) onQuickAction(GamepadQuickAction.QUICK_LOAD)
                true
            }
            mapping.fastForwardKeyCode -> {
                if (ev.repeatCount == 0) {
                    onQuickAction(if (isDown) GamepadQuickAction.FAST_FORWARD_START else GamepadQuickAction.FAST_FORWARD_STOP)
                }
                true
            }
            mapping.rewindKeyCode -> {
                if (isDown) onQuickAction(GamepadQuickAction.REWIND)
                true
            }
            else -> false
        }
    }

    fun handleMotionEvent(
        ev: MotionEvent,
        mapping: GamepadMapping,
        emulator: EmulatorController,
        isPlayerActive: Boolean,
        onQuickAction: (GamepadQuickAction) -> Unit
    ): Boolean {
        if ((ev.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK &&
            (ev.source and InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
        ) {
            return false
        }
        if (ev.action != MotionEvent.ACTION_MOVE) return false

        val lTrigger = maxOf(
            ev.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            ev.getAxisValue(MotionEvent.AXIS_BRAKE)
        )
        val rTrigger = maxOf(
            ev.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            ev.getAxisValue(MotionEvent.AXIS_GAS)
        )

        val l2Now = lTrigger > 0.55f
        val r2Now = rTrigger > 0.55f

        // Capture analog triggers or hat directions if Remapper is listening
        val capture = captureKeyCallback
        if (capture != null) {
            if (l2Now && !lastL2Pressed) {
                lastL2Pressed = true
                captureKeyCallback = null
                capture(KeyEvent.KEYCODE_BUTTON_L2)
                return true
            }
            if (r2Now && !lastR2Pressed) {
                lastR2Pressed = true
                captureKeyCallback = null
                capture(KeyEvent.KEYCODE_BUTTON_R2)
                return true
            }
            val hx = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hy = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)
            when {
                hy < -0.5f -> { captureKeyCallback = null; capture(KeyEvent.KEYCODE_DPAD_UP); return true }
                hy > 0.5f -> { captureKeyCallback = null; capture(KeyEvent.KEYCODE_DPAD_DOWN); return true }
                hx < -0.5f -> { captureKeyCallback = null; capture(KeyEvent.KEYCODE_DPAD_LEFT); return true }
                hx > 0.5f -> { captureKeyCallback = null; capture(KeyEvent.KEYCODE_DPAD_RIGHT); return true }
            }
            if (!l2Now) lastL2Pressed = false
            if (!r2Now) lastR2Pressed = false
            return true
        }

        if (!isPlayerActive) {
            lastL2Pressed = l2Now
            lastR2Pressed = r2Now
            return false
        }

        val target = targetInputForDevice(ev.deviceId, emulator)
        val stickX = ev.getAxisValue(MotionEvent.AXIS_X)
        val stickY = ev.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val x = if (abs(hatX) > abs(stickX)) hatX else stickX
        val y = if (abs(hatY) > abs(stickY)) hatY else stickY
        val deadzone = 0.28f

        target.left = x < -deadzone
        target.right = x > deadzone
        target.up = y < -deadzone
        target.down = y > deadzone

        // Synthesize L2 / R2 trigger actions according to mapping
        if (l2Now != lastL2Pressed) {
            lastL2Pressed = l2Now
            dispatchMappedTrigger(KeyEvent.KEYCODE_BUTTON_L2, l2Now, mapping, target, onQuickAction)
        }
        if (r2Now != lastR2Pressed) {
            lastR2Pressed = r2Now
            dispatchMappedTrigger(KeyEvent.KEYCODE_BUTTON_R2, r2Now, mapping, target, onQuickAction)
        }

        return true
    }

    private fun dispatchMappedTrigger(
        triggerCode: Int,
        isDown: Boolean,
        mapping: GamepadMapping,
        target: ControllerInput,
        onQuickAction: (GamepadQuickAction) -> Unit
    ) {
        when (triggerCode) {
            mapping.fireAKeyCode -> target.fireA = isDown
            mapping.fireBKeyCode -> target.fireB = isDown
            mapping.selectKeyCode -> target.select = isDown
            mapping.startKeyCode -> target.start = isDown
            mapping.resetKeyCode -> target.reset = isDown
            mapping.quickSaveKeyCode -> if (isDown) onQuickAction(GamepadQuickAction.QUICK_SAVE)
            mapping.quickLoadKeyCode -> if (isDown) onQuickAction(GamepadQuickAction.QUICK_LOAD)
            mapping.fastForwardKeyCode -> onQuickAction(if (isDown) GamepadQuickAction.FAST_FORWARD_START else GamepadQuickAction.FAST_FORWARD_STOP)
            mapping.rewindKeyCode -> if (isDown) onQuickAction(GamepadQuickAction.REWIND)
        }
    }
}
