package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AtariSystem
import com.example.data.prefs.AtariSettings
import com.example.data.prefs.CrtFilterMode
import com.example.data.prefs.PerformanceMode
import com.example.emulator.GamepadInputHandler
import com.example.ui.components.AtariHeader
import com.example.ui.components.GamepadMappingDialog
import com.example.ui.components.TelegramChannelsCard
import com.example.ui.theme.AppThemeStyle
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun SettingsScreen(
    settings: AtariSettings,
    connectedGamepadName: String?,
    gamepadHandler: GamepadInputHandler,
    onUpdateSettings: ((AtariSettings) -> AtariSettings) -> Unit,
    onScanWatchedNow: () -> Unit,
    onBackupSaves: (android.net.Uri) -> Unit,
    onRestoreSaves: (android.net.Uri) -> Unit,
    onClearCache: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showGamepadDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val treePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateSettings {
                it.copy(watchDownloadsFolder = true, watchedTreeUri = uri.toString())
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onBackupSaves(uri)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreSaves(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AtariHeader(title = strings.settingsTitle)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gamepad Controller & Remapping section
            item {
                SettingsActionRow(
                    title = strings.settingGamepadMap,
                    subtitle = if (connectedGamepadName != null) {
                        "متصل الآن: $connectedGamepadName"
                    } else {
                        "يد PS / Xbox / 8BitDo (تخصيص كامل للأزرار والاختصارات)"
                    },
                    onClick = { showGamepadDialog = true },
                    testTag = "setting_gamepad_map"
                )
            }

            // Touch Controller Style & Auto-hide
            item {
                Surface(
                    color = AtariSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.useAnalogJoystick) "نوع تحكم اللمس — عصا تحكم" else "نوع تحكم اللمس — أسهم D-Pad",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = settings.useAnalogJoystick,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(useAnalogJoystick = v) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AtariOrange)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إخفاء أزرار اللمس عند توصيل Gamepad", color = AtariTextSecondary, fontSize = 13.sp)
                            Switch(
                                checked = settings.hideTouchOnGamepad,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(hideTouchOnGamepad = v) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AtariCyan)
                            )
                        }
                        Text("${strings.controlsScale}: ${(settings.controlsScale * 100).toInt()}%", color = AtariAmber, fontSize = 12.sp)
                        Slider(
                            value = settings.controlsScale,
                            onValueChange = { v -> onUpdateSettings { it.copy(controlsScale = v) } },
                            valueRange = 0.65f..1.45f,
                            colors = SliderDefaults.colors(thumbColor = AtariAmber, activeTrackColor = AtariAmber)
                        )
                        Text("${strings.controlsOpacity}: ${(settings.controlsOpacity * 100).toInt()}%", color = AtariOrange, fontSize = 12.sp)
                        Slider(
                            value = settings.controlsOpacity,
                            onValueChange = { v -> onUpdateSettings { it.copy(controlsOpacity = v) } },
                            valueRange = 0.25f..1f,
                            colors = SliderDefaults.colors(thumbColor = AtariOrange, activeTrackColor = AtariOrange)
                        )
                    }
                }
            }

            // Video Filter & Enhance
            item {
                val filterLabel = when (settings.crtFilter) {
                    CrtFilterMode.CRT -> "CRT"
                    CrtFilterMode.SCANLINES -> "Scanlines"
                    CrtFilterMode.SMOOTH -> "تنعيم"
                    CrtFilterMode.PIXEL_PERFECT -> "Pixel Perfect"
                }
                SettingsActionRow(
                    title = "${strings.settingFilters} — $filterLabel",
                    subtitle = "اضغط للتبديل بين Pixel Perfect / CRT / Scanlines / تنعيم",
                    onClick = {
                        onUpdateSettings { s ->
                            val next = when (s.crtFilter) {
                                CrtFilterMode.PIXEL_PERFECT -> CrtFilterMode.CRT
                                CrtFilterMode.CRT -> CrtFilterMode.SCANLINES
                                CrtFilterMode.SCANLINES -> CrtFilterMode.SMOOTH
                                CrtFilterMode.SMOOTH -> CrtFilterMode.PIXEL_PERFECT
                            }
                            s.copy(crtFilter = next)
                        }
                    },
                    testTag = "setting_crt_filter"
                )
            }

            item {
                SettingsToggleRow(
                    title = strings.settingEnhance,
                    checked = settings.imageEnhance,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(imageEnhance = v) } },
                    testTag = "setting_enhance"
                )
            }

            // Audio
            item {
                Surface(
                    color = AtariSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.settingAudio, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Switch(
                                checked = settings.audioEnabled,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(audioEnabled = v) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AtariOrange)
                            )
                        }
                        if (settings.audioEnabled) {
                            Slider(
                                value = settings.audioVolume,
                                onValueChange = { v -> onUpdateSettings { it.copy(audioVolume = v) } },
                                colors = SliderDefaults.colors(thumbColor = AtariOrange, activeTrackColor = AtariOrange)
                            )
                        }
                    }
                }
            }

            item {
                SettingsToggleRow(
                    title = strings.settingHaptics,
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(hapticsEnabled = v) } }
                )
            }

            item {
                SettingsToggleRow(
                    title = strings.settingAutoSave,
                    checked = settings.autoSaveEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(autoSaveEnabled = v) } }
                )
            }

            item {
                SettingsToggleRow(
                    title = strings.settingKeepScreenOn,
                    checked = settings.keepScreenOn,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(keepScreenOn = v) } }
                )
            }

            item {
                val perfLabel = when (settings.performanceMode) {
                    PerformanceMode.LOW -> strings.perfLow
                    PerformanceMode.MEDIUM -> strings.perfMedium
                    PerformanceMode.HIGH -> strings.perfHigh
                }
                SettingsActionRow(
                    title = "${strings.settingPerformance} — $perfLabel",
                    onClick = {
                        onUpdateSettings { s ->
                            val next = when (s.performanceMode) {
                                PerformanceMode.HIGH -> PerformanceMode.MEDIUM
                                PerformanceMode.MEDIUM -> PerformanceMode.LOW
                                PerformanceMode.LOW -> PerformanceMode.HIGH
                            }
                            s.copy(performanceMode = next)
                        }
                    }
                )
            }

            item {
                SettingsToggleRow(
                    title = strings.settingWatchDownloads,
                    checked = settings.watchDownloadsFolder,
                    onCheckedChange = { enabled ->
                        if (enabled && settings.watchedTreeUri.isBlank()) {
                            treePickerLauncher.launch(null)
                        } else {
                            onUpdateSettings { it.copy(watchDownloadsFolder = enabled) }
                        }
                    }
                )
            }

            if (settings.watchDownloadsFolder) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { treePickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = AtariSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.settingSelectDownloadsFolder, color = AtariAmber, fontSize = 12.sp)
                        }
                        Button(
                            onClick = onScanWatchedNow,
                            colors = ButtonDefaults.buttonColors(containerColor = AtariSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("فحص الآن", color = AtariCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                SettingsActionRow(
                    title = strings.settingLanguage,
                    onClick = { onUpdateSettings { it.copy(isArabic = !it.isArabic) } },
                    testTag = "setting_language"
                )
            }

            item {
                val themeLabel = when (settings.themeStyle) {
                    AppThemeStyle.MIDNIGHT.name -> "Midnight Cyan"
                    AppThemeStyle.WOODGRAIN.name -> "Classic Woodgrain"
                    else -> "Atari Dark"
                }
                SettingsActionRow(
                    title = "${strings.settingTheme} — $themeLabel",
                    onClick = {
                        onUpdateSettings { s ->
                            val next = when (s.themeStyle) {
                                AppThemeStyle.DARK.name -> AppThemeStyle.MIDNIGHT.name
                                AppThemeStyle.MIDNIGHT.name -> AppThemeStyle.WOODGRAIN.name
                                else -> AppThemeStyle.DARK.name
                            }
                            s.copy(themeStyle = next)
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { backupLauncher.launch("atari_classics_backup.zip") },
                        colors = ButtonDefaults.buttonColors(containerColor = AtariSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.settingBackupSaves, color = AtariAmber, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                        colors = ButtonDefaults.buttonColors(containerColor = AtariSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.settingRestoreSaves, color = AtariCyan, fontSize = 13.sp)
                    }
                }
            }

            item {
                TelegramChannelsCard(isArabic = settings.isArabic)
            }

            item {
                SettingsToggleRow(
                    title = if (settings.isArabic) "رسالة الاشتراك بالقنوات عند الفتح" else "Show Telegram prompt on startup",
                    checked = !settings.telegramSubscribed,
                    onCheckedChange = { showOnLaunch ->
                        onUpdateSettings { it.copy(telegramSubscribed = !showOnLaunch) }
                    }
                )
            }

            item {
                SettingsActionRow(
                    title = strings.settingClearCache,
                    onClick = onClearCache
                )
            }

            item {
                SettingsActionRow(
                    title = strings.settingAbout,
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    if (showGamepadDialog) {
        GamepadMappingDialog(
            mapping = settings.gamepadMapping,
            connectedGamepadName = connectedGamepadName,
            gamepadHandler = gamepadHandler,
            onSave = { newMap ->
                onUpdateSettings { it.copy(gamepadMapping = newMap) }
                showGamepadDialog = false
            },
            onDismiss = { showGamepadDialog = false }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = AtariSurface,
            title = {
                Text("Atari Classics v1.0.0", fontFamily = ChakraPetchFontFamily, color = AtariAmber)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "محاكي ألعاب أتاري الكلاسيكية أوفلاين بسرعة 60fps",
                        color = Color.White
                    )
                    AtariSystem.entries.forEach { sys ->
                        Text(
                            "• ${sys.displayName}: ${sys.libretroCore}",
                            fontSize = 12.sp,
                            color = AtariTextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = AtariOrange)
                }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Surface(
        color = AtariSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AtariOrange,
                    uncheckedThumbColor = AtariTextSecondary,
                    uncheckedTrackColor = Color(0xFF28283E)
                )
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        color = AtariSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = AtariTextSecondary, maxLines = 1)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AtariTextSecondary
            )
        }
    }
}
