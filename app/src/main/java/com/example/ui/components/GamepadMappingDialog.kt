package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.GamepadMapping
import com.example.emulator.GamepadInputHandler
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariSurfaceBorder
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily

@Composable
fun GamepadMappingDialog(
    mapping: GamepadMapping,
    connectedGamepadName: String?,
    gamepadHandler: GamepadInputHandler,
    onSave: (GamepadMapping) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(mapping) }
    var listeningTarget by remember { mutableStateOf<String?>(null) }

    DisposableEffect(listeningTarget) {
        if (listeningTarget != null) {
            gamepadHandler.captureKeyCallback = { code ->
                draft = when (listeningTarget) {
                    "UP" -> draft.copy(upKeyCode = code)
                    "DOWN" -> draft.copy(downKeyCode = code)
                    "LEFT" -> draft.copy(leftKeyCode = code)
                    "RIGHT" -> draft.copy(rightKeyCode = code)
                    "A" -> draft.copy(fireAKeyCode = code)
                    "B" -> draft.copy(fireBKeyCode = code)
                    "SELECT" -> draft.copy(selectKeyCode = code)
                    "START" -> draft.copy(startKeyCode = code)
                    "RESET" -> draft.copy(resetKeyCode = code)
                    "QSAVE" -> draft.copy(quickSaveKeyCode = code)
                    "QLOAD" -> draft.copy(quickLoadKeyCode = code)
                    "FF" -> draft.copy(fastForwardKeyCode = code)
                    "REWIND" -> draft.copy(rewindKeyCode = code)
                    else -> draft
                }
                listeningTarget = null
            }
        } else {
            gamepadHandler.captureKeyCallback = null
        }
        onDispose {
            gamepadHandler.captureKeyCallback = null
        }
    }

    val rows = listOf(
        Triple("UP", "أعلى (D-Pad Up)", draft.upKeyCode),
        Triple("DOWN", "أسفل (D-Pad Down)", draft.downKeyCode),
        Triple("LEFT", "يسار (D-Pad Left)", draft.leftKeyCode),
        Triple("RIGHT", "يمين (D-Pad Right)", draft.rightKeyCode),
        Triple("A", "زر A (إطلاق / قفز)", draft.fireAKeyCode),
        Triple("B", "زر B (إجراء ثانوي)", draft.fireBKeyCode),
        Triple("SELECT", "SELECT", draft.selectKeyCode),
        Triple("START", "START", draft.startKeyCode),
        Triple("RESET", "RESET", draft.resetKeyCode),
        Triple("QSAVE", "حفظ سريع (Quick Save)", draft.quickSaveKeyCode),
        Triple("QLOAD", "تحميل سريع (Quick Load)", draft.quickLoadKeyCode),
        Triple("FF", "تسريع (Fast Forward)", draft.fastForwardKeyCode),
        Triple("REWIND", "ريوايند (Rewind)", draft.rewindKeyCode)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AtariSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = AtariOrange)
                Text(
                    text = "تخصيص أزرار الريموت (BT / USB)",
                    color = AtariAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0B12), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (connectedGamepadName != null) "متصل: $connectedGamepadName" else "وصل يد PS / Xbox / 8BitDo بالبلوتوث أو USB",
                        color = if (connectedGamepadName != null) AtariCyan else AtariTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Text(
                    text = if (listeningTarget != null) {
                        "اضغط الآن على أي زر أو اتجاه في يد التحكم..."
                    } else {
                        "اضغط على أي صف ثم اضغط الزر المطلوب على يد التحكم:"
                    },
                    color = if (listeningTarget != null) AtariCyan else AtariTextSecondary,
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(rows, key = { it.first }) { (key, label, code) ->
                        val isListening = listeningTarget == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B12), RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isListening) AtariCyan else AtariSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { listeningTarget = if (isListening) null else key }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isListening) "..." else KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_"),
                                color = if (isListening) AtariCyan else AtariAmber,
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("حفظ", color = AtariOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { draft = GamepadMapping(); listeningTarget = null }) {
                    Text("افتراضي", color = AtariTextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", color = AtariTextSecondary)
                }
            }
        }
    )
}
