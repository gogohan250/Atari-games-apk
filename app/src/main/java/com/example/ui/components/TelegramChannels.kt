package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariSurface
import com.example.ui.theme.AtariSurfaceBorder
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily

data class TelegramChannelInfo(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val handle: String,
    val url: String
)

object OfficialTelegramChannels {
    val list = listOf(
        TelegramChannelInfo(
            id = "pixels_x_mods_apk",
            titleAr = "قناة Pixels X Mods APK",
            titleEn = "Pixels X Mods APK",
            handle = "@pixels_x_mods_apk",
            url = "https://t.me/pixels_x_mods_apk"
        ),
        TelegramChannelInfo(
            id = "qweqwe2019bot2bot",
            titleAr = "قناة التحديثات والألعاب",
            titleEn = "Games & Updates Channel",
            handle = "@qweqwe2019bot2bot",
            url = "https://t.me/qweqwe2019bot2bot"
        ),
        TelegramChannelInfo(
            id = "abu2saleh",
            titleAr = "قناة Abu2Saleh",
            titleEn = "Abu2Saleh Channel",
            handle = "@Abu2Saleh",
            url = "https://t.me/Abu2Saleh"
        )
    )
}

fun openTelegramUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun TelegramSubscribeStartupDialog(
    isArabic: Boolean,
    onDismissSession: () -> Unit,
    onMarkSubscribed: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissSession,
        containerColor = AtariSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AtariCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Telegram",
                            tint = AtariCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (isArabic) "اشترك في قنواتنا على تليجرام" else "Subscribe on Telegram",
                        color = AtariAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                AtariFourStripes(modifier = Modifier.fillMaxWidth(), stripeHeight = 3f)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) {
                        "انضم إلى قنواتنا الرسمية للحصول على أحدث الألعاب والتحديثات والإضافات مجاناً:"
                    } else {
                        "Join our official Telegram channels for the latest ROMs, updates, and mods:"
                    },
                    color = Color.White,
                    fontSize = 13.sp
                )

                OfficialTelegramChannels.list.forEach { channel ->
                    TelegramChannelRow(
                        channel = channel,
                        isArabic = isArabic,
                        onClick = { openTelegramUrl(context, channel.url) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onMarkSubscribed,
                colors = ButtonDefaults.buttonColors(containerColor = AtariOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("telegram_subscribed_button")
            ) {
                Text(
                    text = if (isArabic) "تم الاشتراك ✓" else "Subscribed ✓",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissSession,
                modifier = Modifier.testTag("telegram_later_button")
            ) {
                Text(
                    text = if (isArabic) "لاحقاً" else "Later",
                    color = AtariTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    )
}

@Composable
fun TelegramChannelsCard(
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        color = AtariSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AtariCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("telegram_channels_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Telegram",
                    tint = AtariCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "قنواتنا الرسمية على تليجرام" else "Official Telegram Channels",
                        style = MaterialTheme.typography.titleMedium,
                        color = AtariAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "اضغط للاشتراك ومتابعة كل جديد" else "Tap to join and follow updates",
                        fontSize = 11.sp,
                        color = AtariTextSecondary
                    )
                }
            }

            OfficialTelegramChannels.list.forEach { channel ->
                TelegramChannelRow(
                    channel = channel,
                    isArabic = isArabic,
                    onClick = { openTelegramUrl(context, channel.url) }
                )
            }
        }
    }
}

@Composable
private fun TelegramChannelRow(
    channel: TelegramChannelInfo,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0B0B12),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AtariSurfaceBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("telegram_channel_${channel.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) channel.titleAr else channel.titleEn,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.handle,
                    fontFamily = ChakraPetchFontFamily,
                    fontSize = 11.sp,
                    color = AtariCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = AtariCyan.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(1.dp, AtariCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = if (isArabic) "اشتراك" else "Join",
                    color = AtariCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
