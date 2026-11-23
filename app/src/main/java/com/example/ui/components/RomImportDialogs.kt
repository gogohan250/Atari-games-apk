package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RomImportProgress
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariSurfaceElevated
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily

@Composable
fun RomImportOptionsDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelectMultipleFiles: () -> Unit,
    onSelectFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AtariSurface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AtariFourStripes(modifier = Modifier.width(68.dp), stripeHeight = 3.5f)
                Text(
                    text = if (isArabic) "استيراد ألعاب من الجهاز" else "Import Games from Device",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isArabic) {
                        "اختر عدة ألعاب دفعة واحدة أو حدد مجلداً كاملاً وسيتم اكتشاف جميع الملفات تلقائياً (بما فيها المجلدات الفرعية وملفات ZIP)."
                    } else {
                        "Pick multiple ROM files at once or select an entire folder (including subfolders & ZIP archives)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtariTextSecondary
                )

                ImportChoiceCard(
                    icon = Icons.Default.LibraryAdd,
                    accent = AtariOrange,
                    title = if (isArabic) "اختيار ملف أو عدة ألعاب معاً" else "Select One or Multiple Games",
                    subtitle = if (isArabic) "تحديد متعدد لملفات a26 / bin / a78 / a52 / lnx / j64 / zip" else "Multi-select a26, bin, a78, a52, lnx, j64, zip files",
                    testTag = "import_multiple_files_option",
                    onClick = {
                        onDismiss()
                        onSelectMultipleFiles()
                    }
                )

                ImportChoiceCard(
                    icon = Icons.Default.CreateNewFolder,
                    accent = AtariCyan,
                    title = if (isArabic) "اختيار مجلد كامل للألعاب" else "Select Entire Games Folder",
                    subtitle = if (isArabic) "استيراد جميع الألعاب داخل المجلد والمجلدات الفرعية دفعة واحدة" else "Recursively import all Atari ROMs in folder & subfolders",
                    testTag = "import_folder_option",
                    onClick = {
                        onDismiss()
                        onSelectFolder()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    color = AtariAmber
                )
            }
        }
    )
}

@Composable
private fun ImportChoiceCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = AtariSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtariTextSecondary
                )
            }
        }
    }
}

@Composable
fun RomImportProgressDialog(
    progress: RomImportProgress,
    isArabic: Boolean
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = AtariSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isArabic) "جارٍ استيراد الألعاب..." else "Importing Atari ROMs...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (progress.total <= 0) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AtariOrange,
                        trackColor = AtariSurfaceElevated
                    )
                    Text(
                        text = if (isArabic) "جارٍ فحص المجلد والمجلدات الفرعية..." else "Scanning folder and subfolders...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtariTextSecondary
                    )
                } else {
                    val ratio = (progress.processed.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth(),
                        color = AtariOrange,
                        trackColor = AtariSurfaceElevated
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) {
                                "تم فحص ${progress.processed} من ${progress.total}"
                            } else {
                                "Checked ${progress.processed} of ${progress.total}"
                            },
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 13.sp,
                            color = AtariTextSecondary
                        )
                        Text(
                            text = if (isArabic) {
                                "تمت إضافة ${progress.importedCount}"
                            } else {
                                "Added ${progress.importedCount}"
                            },
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AtariAmber
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
