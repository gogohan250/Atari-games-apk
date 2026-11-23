package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AtariSystem
import com.example.data.model.CheatEntity
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import com.example.data.prefs.AspectRatioMode
import com.example.data.prefs.AtariSettings
import com.example.data.prefs.CrtFilterMode
import com.example.emulator.EmulatorController
import com.example.emulator.GamepadInputHandler
import com.example.ui.components.ClassicCrossDpad
import com.example.ui.components.DraggableControlBox
import com.example.ui.components.GamepadMappingDialog
import com.example.ui.components.HoldSystemPill
import com.example.ui.components.HoldTouchButton
import com.example.ui.components.TouchCustomizationPanel
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.formatPlayDuration
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariBackground
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariRed
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariSurfaceBorder
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerScreen(
    game: GameEntity,
    emulator: EmulatorController,
    settings: AtariSettings,
    connectedGamepadName: String?,
    gamepadHandler: GamepadInputHandler,
    saveSlots: List<SaveStateEntity>,
    cheats: List<CheatEntity>,
    onUpdateSettings: ((AtariSettings) -> AtariSettings) -> Unit,
    onAddCheat: (name: String, code: String) -> Unit,
    onToggleCheat: (id: String, enabled: Boolean) -> Unit,
    onDeleteCheat: (id: String) -> Unit,
    onToast: (String) -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val bitmap by emulator.bitmapState.collectAsStateWithLifecycle()
    val frameCount by emulator.frameCounter.collectAsStateWithLifecycle()
    val speed by emulator.speedMultiplier.collectAsStateWithLifecycle()
    val isPaused by emulator.isPaused.collectAsStateWithLifecycle()
    val isRecording by emulator.isRecording.collectAsStateWithLifecycle()
    val sessionSeconds by emulator.sessionSeconds.collectAsStateWithLifecycle()

    var showSaveDialogMode by remember { mutableStateOf<String?>(null) }
    var showCheatsDialog by remember { mutableStateOf(false) }
    var showGamepadDialog by remember { mutableStateOf(false) }
    var editControlsMode by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var forceShowTouch by remember { mutableStateOf(false) }

    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler {
        onExit()
    }

    val sys = AtariSystem.fromCode(game.system)
    val shouldHideTouch = settings.hideTouchOnGamepad &&
        connectedGamepadName != null &&
        !editControlsMode &&
        !forceShowTouch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AtariBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (!isFullscreen) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = game.title,
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${sys.libretroCore} • ${formatPlayDuration(sessionSeconds)}",
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 11.sp,
                            color = AtariAmber
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showGamepadDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("player_gamepad_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = strings.settingGamepadMap,
                                tint = if (connectedGamepadName != null) AtariCyan else AtariTextSecondary
                            )
                        }
                        IconButton(onClick = { showCheatsDialog = true }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = strings.cheatsTitle,
                                tint = if (cheats.any { it.enabled }) AtariOrange else AtariCyan
                            )
                        }
                        IconButton(
                            onClick = { onUpdateSettings { it.copy(twoPlayerMode = !it.twoPlayerMode) } },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = strings.twoPlayerMode,
                                tint = if (settings.twoPlayerMode) AtariOrange else AtariTextSecondary
                            )
                        }
                        IconButton(
                            onClick = { editControlsMode = !editControlsMode },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("player_edit_touch_button")
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = strings.editControls,
                                tint = if (editControlsMode) AtariAmber else AtariTextSecondary
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp)
                        .background(AtariOrange)
                )
            }
        }

        // CRT Display Viewport — FillBounds inside 4:3 stretches NTSC 160x192 pixels authentically with ZERO black sidebars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        isFullscreen -> Modifier.weight(1f)
                        shouldHideTouch -> Modifier.weight(0.76f)
                        else -> Modifier.weight(0.45f)
                    }
                )
                .padding(horizontal = if (isFullscreen) 4.dp else 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val aspectModifier = when (settings.aspectRatio) {
                AspectRatioMode.ORIGINAL -> Modifier.aspectRatio(sys.defaultAspectRatio)
                AspectRatioMode.FILL -> Modifier.fillMaxSize()
                AspectRatioMode.STRETCH -> Modifier.fillMaxSize()
            }
            val contentScale = when (settings.aspectRatio) {
                AspectRatioMode.ORIGINAL -> ContentScale.FillBounds
                AspectRatioMode.FILL -> ContentScale.Crop
                AspectRatioMode.STRETCH -> ContentScale.FillBounds
            }

            Box(
                modifier = aspectModifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(2.dp, AtariOrange, RoundedCornerShape(10.dp))
                    .testTag("emulator_crt_screen")
            ) {
                val bmp = bitmap
                val currentTick = frameCount
                if (bmp != null && currentTick >= 0L) {
                    val filterQuality = if (settings.crtFilter == CrtFilterMode.SMOOTH || settings.imageEnhance) {
                        FilterQuality.Low
                    } else {
                        FilterQuality.None
                    }
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = game.title,
                        contentScale = contentScale,
                        filterQuality = filterQuality,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (settings.crtFilter == CrtFilterMode.CRT || settings.crtFilter == CrtFilterMode.SCANLINES) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 5f
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = Color.Black.copy(alpha = if (settings.crtFilter == CrtFilterMode.CRT) 0.16f else 0.12f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                        if (settings.crtFilter == CrtFilterMode.CRT) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                                    radius = size.maxDimension * 0.85f
                                )
                            )
                        }
                    }
                }

                if (isRecording) {
                    Surface(
                        color = AtariRed,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "REC",
                            color = Color.White,
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                    }
                }
            }
        }

        // Action Pill Toolbar
        val filterPillLabel = when (settings.crtFilter) {
            CrtFilterMode.CRT -> "CRT"
            CrtFilterMode.SCANLINES -> "Scan"
            CrtFilterMode.SMOOTH -> "Smooth"
            CrtFilterMode.PIXEL_PERFECT -> "Pixel"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarPill(strings.saveAction, AtariAmber) { showSaveDialogMode = "SAVE" }
            ToolbarPill(strings.loadAction, AtariAmber) { showSaveDialogMode = "LOAD" }
            ToolbarPill(
                label = if (speed == 1f) strings.fastAction else "${speed}x",
                color = if (speed != 1f) AtariCyan else AtariAmber
            ) { emulator.cycleSpeed() }
            ToolbarPill(strings.rewindAction, AtariAmber) {
                if (!emulator.rewindStep()) onToast("لا توجد إطارات للرجوع")
            }
            ToolbarPill(strings.shotAction, AtariAmber) {
                val file = emulator.captureScreenshot()
                onToast(if (file != null) "تم حفظ اللقطة: ${file.name}" else "تعذر التقاط الشاشة")
            }
            ToolbarPill(
                label = strings.recordAction,
                color = if (isRecording) AtariRed else AtariAmber
            ) {
                val dir = emulator.toggleRecording()
                if (dir != null) onToast("تم حفظ الفيديو (${dir.listFiles()?.size ?: 0} إطار)")
            }
            ToolbarPill(strings.fillAction, AtariAmber) {
                val next = when (settings.aspectRatio) {
                    AspectRatioMode.ORIGINAL -> AspectRatioMode.FILL
                    AspectRatioMode.FILL -> AspectRatioMode.STRETCH
                    AspectRatioMode.STRETCH -> AspectRatioMode.ORIGINAL
                }
                onUpdateSettings { it.copy(aspectRatio = next) }
            }
            ToolbarPill(filterPillLabel, AtariCyan) {
                val next = when (settings.crtFilter) {
                    CrtFilterMode.PIXEL_PERFECT -> CrtFilterMode.CRT
                    CrtFilterMode.CRT -> CrtFilterMode.SCANLINES
                    CrtFilterMode.SCANLINES -> CrtFilterMode.SMOOTH
                    CrtFilterMode.SMOOTH -> CrtFilterMode.PIXEL_PERFECT
                }
                onUpdateSettings { it.copy(crtFilter = next) }
            }
            IconButton(
                onClick = { emulator.togglePause() },
                modifier = Modifier
                    .size(36.dp)
                    .background(AtariSurface, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = AtariOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { onUpdateSettings { it.copy(audioEnabled = !it.audioEnabled) } },
                modifier = Modifier
                    .size(36.dp)
                    .background(AtariSurface, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = if (settings.audioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = strings.settingAudio,
                    tint = if (settings.audioEnabled) AtariCyan else AtariTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .size(36.dp)
                    .background(AtariSurface, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        if (editControlsMode) {
            TouchCustomizationPanel(settings = settings, onUpdateSettings = onUpdateSettings)
        }

        if (shouldHideTouch) {
            Surface(
                color = AtariSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .border(1.dp, AtariCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { forceShowTouch = true }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = AtariCyan)
                    Text(
                        text = "  ${connectedGamepadName ?: "Gamepad"} متصل — اضغط لإظهار أزرار اللمس",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Virtual Touch Controls (Forced LTR so Joystick is Left & A/B are Right)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isFullscreen) 0.42f else 0.55f)
                        .alpha(settings.controlsOpacity)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    val masterScale = settings.controlsScale
                    val dpadSize = (154f * masterScale * settings.dpadScale).dp
                    val aSize = (74f * masterScale * settings.aBtnScale).dp
                    val bSize = (66f * masterScale * settings.bBtnScale).dp

                    // Player 1 Joystick OR Cross D-Pad (Left)
                    DraggableControlBox(
                        offsetX = settings.dpadOffsetX,
                        offsetY = settings.dpadOffsetY,
                        editMode = editControlsMode,
                        onDrag = { dx, dy ->
                            onUpdateSettings {
                                it.copy(dpadOffsetX = it.dpadOffsetX + dx, dpadOffsetY = it.dpadOffsetY + dy)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        if (settings.useAnalogJoystick) {
                            VirtualJoystick(
                                input = emulator.p1Input,
                                editMode = editControlsMode,
                                hapticsEnabled = settings.hapticsEnabled,
                                modifier = Modifier.size(dpadSize)
                            )
                        } else {
                            ClassicCrossDpad(
                                input = emulator.p1Input,
                                editMode = editControlsMode,
                                hapticsEnabled = settings.hapticsEnabled,
                                modifier = Modifier.size(dpadSize)
                            )
                        }
                    }

                    // Optional Player 2 Mini Pad (Top Center)
                    if (settings.twoPlayerMode) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .background(AtariSurface.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                                .border(1.dp, AtariCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("P2", color = AtariCyan, fontFamily = ChakraPetchFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            HoldTouchButton("◀", AtariSurface, Color.White, 34.dp) { emulator.p2Input.left = it }
                            HoldTouchButton("▲", AtariSurface, Color.White, 34.dp) { emulator.p2Input.up = it }
                            HoldTouchButton("▼", AtariSurface, Color.White, 34.dp) { emulator.p2Input.down = it }
                            HoldTouchButton("▶", AtariSurface, Color.White, 34.dp) { emulator.p2Input.right = it }
                            HoldTouchButton("F2", AtariRed, Color.White, 38.dp) { emulator.p2Input.fireA = it }
                        }
                    }

                    // Action Buttons A (Red) & B (Amber) — Independently draggable & scalable!
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size((170f * masterScale).dp)
                    ) {
                        DraggableControlBox(
                            offsetX = settings.buttonsOffsetX + settings.bBtnOffsetX,
                            offsetY = settings.buttonsOffsetY + settings.bBtnOffsetY,
                            editMode = editControlsMode,
                            onDrag = { dx, dy ->
                                onUpdateSettings {
                                    it.copy(bBtnOffsetX = it.bBtnOffsetX + dx, bBtnOffsetY = it.bBtnOffsetY + dy)
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomStart)
                        ) {
                            HoldTouchButton(
                                label = "B",
                                bgColor = AtariAmber,
                                textColor = Color(0xFF140D00),
                                sizeDp = bSize,
                                editMode = editControlsMode,
                                onStateChange = { pressed ->
                                    if (pressed && settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    emulator.p1Input.fireB = pressed
                                }
                            )
                        }

                        DraggableControlBox(
                            offsetX = settings.buttonsOffsetX + settings.aBtnOffsetX,
                            offsetY = settings.buttonsOffsetY + settings.aBtnOffsetY,
                            editMode = editControlsMode,
                            onDrag = { dx, dy ->
                                onUpdateSettings {
                                    it.copy(aBtnOffsetX = it.aBtnOffsetX + dx, aBtnOffsetY = it.aBtnOffsetY + dy)
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            HoldTouchButton(
                                label = "A",
                                bgColor = AtariRed,
                                textColor = Color.White,
                                sizeDp = aSize,
                                editMode = editControlsMode,
                                onStateChange = { pressed ->
                                    if (pressed && settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    emulator.p1Input.fireA = pressed
                                }
                            )
                        }
                    }

                    // Bottom SELECT / RESET / START bar (Draggable in Edit mode!)
                    DraggableControlBox(
                        offsetX = settings.sysBtnsOffsetX,
                        offsetY = settings.sysBtnsOffsetY,
                        editMode = editControlsMode,
                        onDrag = { dx, dy ->
                            onUpdateSettings {
                                it.copy(sysBtnsOffsetX = it.sysBtnsOffsetX + dx, sysBtnsOffsetY = it.sysBtnsOffsetY + dy)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HoldSystemPill("SELECT", editMode = editControlsMode) { emulator.p1Input.select = it }
                            HoldSystemPill("RESET", editMode = editControlsMode) { emulator.p1Input.reset = it }
                            HoldSystemPill("START", editMode = editControlsMode) { emulator.p1Input.start = it }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialogMode != null) {
        SaveSlotsDialog(
            mode = showSaveDialogMode!!,
            slots = saveSlots,
            onSelectSlot = { slotIdx ->
                scope.launch {
                    if (showSaveDialogMode == "SAVE") {
                        val ok = emulator.saveToSlot(slotIdx)
                        onToast(if (ok) "تم الحفظ في الخانة $slotIdx" else "فشل الحفظ")
                    } else {
                        val ok = emulator.loadFromSlot(slotIdx)
                        onToast(if (ok) "تم التحميل من الخانة $slotIdx" else "الخانة فارغة")
                    }
                    showSaveDialogMode = null
                }
            },
            onDismiss = { showSaveDialogMode = null }
        )
    }

    if (showCheatsDialog) {
        CheatsModalDialog(
            cheats = cheats,
            onAddCheat = onAddCheat,
            onToggleCheat = onToggleCheat,
            onDeleteCheat = onDeleteCheat,
            onDismiss = { showCheatsDialog = false }
        )
    }

    if (showGamepadDialog) {
        GamepadMappingDialog(
            mapping = settings.gamepadMapping,
            connectedGamepadName = connectedGamepadName,
            gamepadHandler = gamepadHandler,
            onSave = { newMap ->
                onUpdateSettings { it.copy(gamepadMapping = newMap) }
                showGamepadDialog = false
                onToast("تم حفظ إعدادات الريموت")
            },
            onDismiss = { showGamepadDialog = false }
        )
    }
}

@Composable
private fun ToolbarPill(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = AtariSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, AtariSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SaveSlotsDialog(
    mode: String,
    slots: List<SaveStateEntity>,
    onSelectSlot: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val slotMap = slots.associateBy { it.slotIndex }
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AtariSurface,
        title = {
            Text(
                text = if (mode == "SAVE") "حفظ الحالة (10 خانات)" else "تحميل الحالة (10 خانات)",
                color = AtariAmber,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((0..10).toList()) { idx ->
                    val existing = slotMap[idx]
                    Surface(
                        color = Color(0xFF0B0B12),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSlot(idx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (idx == 0) "حفظ تلقائي (Auto)" else "خانة $idx",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (existing != null) fmt.format(Date(existing.updatedAt)) else "فارغة",
                                color = if (existing != null) AtariCyan else AtariTextSecondary,
                                fontFamily = ChakraPetchFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = AtariOrange)
            }
        }
    )
}

@Composable
private fun CheatsModalDialog(
    cheats: List<CheatEntity>,
    onAddCheat: (String, String) -> Unit,
    onToggleCheat: (String, Boolean) -> Unit,
    onDeleteCheat: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AtariSurface,
        title = {
            Text(strings.cheatsTitle, color = AtariAmber, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onAddCheat("حياة لانهائية (Infinite Lives)", "FF:09") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ تفعيل غش الحياة اللانهائية فوراً", color = AtariCyan, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسم الغش (مثال: حياة لانهائية)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("ADDR:VAL (e.g. 85:09)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (codeInput.isNotBlank()) {
                                onAddCheat(nameInput, codeInput)
                                nameInput = ""; codeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AtariOrange)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = strings.addCheat)
                    }
                }

                LazyColumn(
                    modifier = Modifier.height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cheats, key = { it.id }) { ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B12), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ch.name, color = Color.White, fontSize = 13.sp)
                                Text(ch.code, color = AtariCyan, fontFamily = ChakraPetchFontFamily, fontSize = 11.sp)
                            }
                            Switch(
                                checked = ch.enabled,
                                onCheckedChange = { onToggleCheat(ch.id, it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AtariOrange)
                            )
                            IconButton(onClick = { onDeleteCheat(ch.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AtariRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم", color = AtariOrange)
            }
        }
    )
}
