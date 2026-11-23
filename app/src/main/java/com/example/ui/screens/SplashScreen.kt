package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emulator.AtariAudioEngine
import com.example.ui.theme.AtariAmber
import com.example.ui.theme.AtariBackground
import com.example.ui.theme.AtariCyan
import com.example.ui.theme.AtariOrange
import com.example.ui.theme.AtariStripeColors
import com.example.ui.theme.AtariTextSecondary
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun SplashScreen(
    progress: Float
) {
    val strings = LocalAppStrings.current
    val logoAlpha = remember { Animatable(0f) }
    val stripeScale = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        AtariAudioEngine.playStartupChime()
        logoAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        stripeScale.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF28120B), AtariBackground)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .alpha(logoAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Iconic 4 Atari Vertical/Sweeping Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(78.dp)
            ) {
                val heights = listOf(42.dp, 54.dp, 66.dp, 78.dp)
                AtariStripeColors.forEachIndexed { idx, col ->
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(heights[idx] * stripeScale.value)
                            .clip(RoundedCornerShape(6.dp))
                            .background(col)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "ATARI CLASSICS",
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = AtariAmber,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(34.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AtariOrange,
                trackColor = Color(0xFF1F1F32)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.loadingCatalog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtariTextSecondary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = AtariCyan
                )
            }
        }
    }
}
