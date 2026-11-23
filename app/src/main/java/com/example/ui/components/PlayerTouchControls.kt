package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.AtariSettings
import com.example.emulator.ControllerInput
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariRed
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariSurfaceBorder
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class TouchEditTarget(val labelAr: String, val labelEn: String) {
    ALL("الكل", "All"),
    DPAD("الاتجاهات", "D-Pad"),
    BTN_A("زر A", "Btn A"),
    BTN_B("زر B", "Btn B")
}

@Composable
fun TouchCustomizationPanel(
    settings: AtariSettings,
    onUpdateSettings: ((AtariSettings) -> AtariSettings) -> Unit
) {
    val strings = LocalAppStrings.current
    var target by remember { mutableStateOf(TouchEditTarget.ALL) }

    val activeScale = when (target) {
        TouchEditTarget.ALL -> settings.controlsScale
        TouchEditTarget.DPAD -> settings.dpadScale
        TouchEditTarget.BTN_A -> settings.aBtnScale
        TouchEditTarget.BTN_B -> settings.bBtnScale
    }

    Surface(
        color = AtariSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, AtariAmber.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.editControls} — اسحب أي زر لتحريكه",
                    color = AtariAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                TextButton(
                    onClick = {
                        onUpdateSettings {
                            it.copy(
                                controlsOpacity = 0.88f,
                                controlsScale = 1.0f,
                                dpadScale = 1.0f,
                                aBtnScale = 1.0f,
                                bBtnScale = 1.0f,
                                dpadOffsetX = 0f,
                                dpadOffsetY = 0f,
                                buttonsOffsetX = 0f,
                                buttonsOffsetY = 0f,
                                aBtnOffsetX = 0f,
                                aBtnOffsetY = 0f,
                                bBtnOffsetX = 0f,
                                bBtnOffsetY = 0f,
                                sysBtnsOffsetX = 0f,
                                sysBtnsOffsetY = 0f
                            )
                        }
                    }
                ) {
                    Text(strings.resetLayout, color = AtariCyan, fontSize = 11.sp)
                }
            }

            // Target selector pills for individual resizing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TouchEditTarget.entries.forEach { t ->
                    val selected = t == target
                    Surface(
                        color = if (selected) AtariOrange else Color(0xFF0B0B12),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { target = t }
                    ) {
                        Text(
                            text = if (settings.isArabic) t.labelAr else t.labelEn,
                            color = if (selected) Color.White else AtariTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.controlsScale, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(82.dp))
                Slider(
                    value = activeScale,
                    onValueChange = { v ->
                        onUpdateSettings { s ->
                            when (target) {
                                TouchEditTarget.ALL -> s.copy(controlsScale = v)
                                TouchEditTarget.DPAD -> s.copy(dpadScale = v)
                                TouchEditTarget.BTN_A -> s.copy(aBtnScale = v)
                                TouchEditTarget.BTN_B -> s.copy(bBtnScale = v)
                            }
                        }
                    },
                    valueRange = 0.65f..1.45f,
                    colors = SliderDefaults.colors(thumbColor = AtariAmber, activeTrackColor = AtariAmber),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.controlsOpacity, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(82.dp))
                Slider(
                    value = settings.controlsOpacity,
                    onValueChange = { v -> onUpdateSettings { it.copy(controlsOpacity = v) } },
                    valueRange = 0.25f..1f,
                    colors = SliderDefaults.colors(thumbColor = AtariOrange, activeTrackColor = AtariOrange),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (settings.useAnalogJoystick) "عصا تحكم" else "أسهم D-Pad",
                        color = AtariCyan,
                        fontSize = 11.sp
                    )
                    Switch(
                        checked = settings.useAnalogJoystick,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(useAnalogJoystick = v) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AtariCyan)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("إخفاء مع الريموت", color = Color.White, fontSize = 11.sp)
                    Switch(
                        checked = settings.hideTouchOnGamepad,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(hideTouchOnGamepad = v) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AtariOrange)
                    )
                }
            }
        }
    }
}

@Composable
fun VirtualJoystick(
    input: ControllerInput,
    editMode: Boolean,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var lastDirMask by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .then(
                if (!editMode) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxRadius = size.width * 0.32f
                                val active = event.changes.firstOrNull { it.pressed }
                                if (active != null && event.type != PointerEventType.Release) {
                                    val delta = active.position - center
                                    val dist = hypot(delta.x, delta.y)
                                    val clamped = if (dist > maxRadius) {
                                        val angle = atan2(delta.y, delta.x)
                                        Offset(cos(angle) * maxRadius, sin(angle) * maxRadius)
                                    } else delta
                                    knobOffset = clamped

                                    val normX = clamped.x / maxRadius
                                    val normY = clamped.y / maxRadius
                                    val threshold = 0.25f
                                    input.left = normX < -threshold
                                    input.right = normX > threshold
                                    input.up = normY < -threshold
                                    input.down = normY > threshold

                                    val mask = (if (input.up) 1 else 0) or (if (input.down) 2 else 0) or
                                        (if (input.left) 4 else 0) or (if (input.right) 8 else 0)
                                    if (mask != lastDirMask && mask != 0 && hapticsEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    lastDirMask = mask
                                    active.consume()
                                } else {
                                    knobOffset = Offset.Zero
                                    input.up = false; input.down = false
                                    input.left = false; input.right = false
                                    lastDirMask = 0
                                }
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = AtariSurface, radius = r, center = center)
            drawCircle(
                color = if (editMode) AtariCyan else AtariSurfaceBorder,
                radius = r,
                center = center,
                style = Stroke(if (editMode) 4f else 3f)
            )

            val arrowDist = r * 0.72f
            val tri = 10f
            val arrowCol = Color(0xFF7A7D96)
            drawPath(Path().apply {
                moveTo(center.x, center.y - arrowDist - tri)
                lineTo(center.x - tri, center.y - arrowDist + tri * 0.5f)
                lineTo(center.x + tri, center.y - arrowDist + tri * 0.5f)
                close()
            }, arrowCol)
            drawPath(Path().apply {
                moveTo(center.x, center.y + arrowDist + tri)
                lineTo(center.x - tri, center.y + arrowDist - tri * 0.5f)
                lineTo(center.x + tri, center.y + arrowDist - tri * 0.5f)
                close()
            }, arrowCol)
            drawPath(Path().apply {
                moveTo(center.x - arrowDist - tri, center.y)
                lineTo(center.x - arrowDist + tri * 0.5f, center.y - tri)
                lineTo(center.x - arrowDist + tri * 0.5f, center.y + tri)
                close()
            }, arrowCol)
            drawPath(Path().apply {
                moveTo(center.x + arrowDist + tri, center.y)
                lineTo(center.x + arrowDist - tri * 0.5f, center.y - tri)
                lineTo(center.x + arrowDist - tri * 0.5f, center.y + tri)
                close()
            }, arrowCol)

            drawCircle(
                color = AtariOrange,
                radius = r * 0.36f,
                center = center + knobOffset
            )
        }
    }
}

@Composable
fun ClassicCrossDpad(
    input: ControllerInput,
    editMode: Boolean,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var activeMask by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .then(
                if (!editMode) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val active = event.changes.firstOrNull { it.pressed }
                                if (active != null && event.type != PointerEventType.Release) {
                                    val dx = (active.position.x - center.x) / (size.width / 2f)
                                    val dy = (active.position.y - center.y) / (size.height / 2f)
                                    val threshold = 0.24f
                                    input.left = dx < -threshold
                                    input.right = dx > threshold
                                    input.up = dy < -threshold
                                    input.down = dy > threshold
                                    val mask = (if (input.up) 1 else 0) or (if (input.down) 2 else 0) or
                                        (if (input.left) 4 else 0) or (if (input.right) 8 else 0)
                                    if (mask != activeMask && mask != 0 && hapticsEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    activeMask = mask
                                    active.consume()
                                } else {
                                    input.up = false; input.down = false
                                    input.left = false; input.right = false
                                    activeMask = 0
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val arm = w * 0.34f
            val armOffset = (w - arm) / 2f

            if (editMode) {
                drawCircle(color = AtariCyan.copy(alpha = 0.25f), radius = w * 0.49f, style = Stroke(3f))
            }

            // Vertical bar
            drawRoundRect(
                color = AtariSurface,
                topLeft = Offset(armOffset, 2f),
                size = Size(arm, h - 4f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Horizontal bar
            drawRoundRect(
                color = AtariSurface,
                topLeft = Offset(2f, armOffset),
                size = Size(w - 4f, arm),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Active directional highlights
            if ((activeMask and 1) != 0) {
                drawRoundRect(AtariOrange, Offset(armOffset + 3f, 4f), Size(arm - 6f, armOffset), CornerRadius(10f, 10f))
            }
            if ((activeMask and 2) != 0) {
                drawRoundRect(AtariOrange, Offset(armOffset + 3f, h - armOffset - 4f), Size(arm - 6f, armOffset), CornerRadius(10f, 10f))
            }
            if ((activeMask and 4) != 0) {
                drawRoundRect(AtariOrange, Offset(4f, armOffset + 3f), Size(armOffset, arm - 6f), CornerRadius(10f, 10f))
            }
            if ((activeMask and 8) != 0) {
                drawRoundRect(AtariOrange, Offset(w - armOffset - 4f, armOffset + 3f), Size(armOffset, arm - 6f), CornerRadius(10f, 10f))
            }

            drawCircle(color = Color(0xFF28283E), radius = arm * 0.32f, center = Offset(w / 2f, h / 2f))
        }
    }
}

@Composable
fun HoldTouchButton(
    label: String,
    bgColor: Color,
    textColor: Color,
    sizeDp: Dp,
    editMode: Boolean = false,
    onDragDelta: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onStateChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(if (pressed) bgColor.copy(alpha = 0.72f) else bgColor)
            .border(
                width = if (editMode) 2.5.dp else 2.dp,
                color = if (editMode) AtariCyan else Color.White.copy(alpha = if (pressed) 0.7f else 0.18f),
                shape = CircleShape
            )
            .then(
                if (editMode && onDragDelta != null) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val isDown = ev.changes.any { it.pressed }
                                if (isDown != pressed) {
                                    pressed = isDown
                                    onStateChange(isDown)
                                }
                                ev.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = ChakraPetchFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            color = textColor
        )
    }
}

@Composable
fun HoldSystemPill(
    label: String,
    editMode: Boolean = false,
    onStateChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) AtariOrange else AtariSurface)
            .border(
                width = 1.dp,
                color = if (editMode) AtariCyan else AtariSurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (!editMode) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val isDown = ev.changes.any { it.pressed }
                                if (isDown != pressed) {
                                    pressed = isDown
                                    onStateChange(isDown)
                                }
                                ev.changes.forEach { it.consume() }
                            }
                        }
                    }
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = ChakraPetchFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (pressed) Color.White else AtariTextSecondary
        )
    }
}

@Composable
fun DraggableControlBox(
    offsetX: Float,
    offsetY: Float,
    editMode: Boolean,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .then(
                if (editMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                } else Modifier
            )
    ) {
        content()
    }
}
