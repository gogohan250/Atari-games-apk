package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.i18n.ArabicStrings
import com.example.i18n.EnglishStrings

enum class AppThemeStyle { DARK, MIDNIGHT, WOODGRAIN }

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { ArabicStrings }

private val AtariShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AtariClassicsTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.DARK,
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        AppThemeStyle.DARK -> darkColorScheme(
            primary = AtariOrange,
            onPrimary = Color.White,
            secondary = AtariAmber,
            onSecondary = AtariBackground,
            tertiary = AtariCyan,
            onTertiary = AtariBackground,
            error = AtariRed,
            onError = Color.White,
            background = AtariBackground,
            onBackground = AtariTextPrimary,
            surface = AtariSurface,
            onSurface = AtariTextPrimary,
            surfaceVariant = AtariSurfaceElevated,
            onSurfaceVariant = AtariTextSecondary,
            outline = AtariSurfaceBorder
        )
        AppThemeStyle.MIDNIGHT -> darkColorScheme(
            primary = MidnightAccent,
            onPrimary = MidnightBackground,
            secondary = AtariOrange,
            onSecondary = Color.White,
            tertiary = AtariAmber,
            onTertiary = MidnightBackground,
            error = AtariRed,
            onError = Color.White,
            background = MidnightBackground,
            onBackground = AtariTextPrimary,
            surface = MidnightSurface,
            onSurface = AtariTextPrimary,
            surfaceVariant = Color(0xFF18223A),
            onSurfaceVariant = AtariTextSecondary,
            outline = Color(0xFF24324E)
        )
        AppThemeStyle.WOODGRAIN -> darkColorScheme(
            primary = WoodgrainAccent,
            onPrimary = WoodgrainBackground,
            secondary = AtariAmber,
            onSecondary = WoodgrainBackground,
            tertiary = AtariCyan,
            onTertiary = WoodgrainBackground,
            error = AtariRed,
            onError = Color.White,
            background = WoodgrainBackground,
            onBackground = AtariTextPrimary,
            surface = WoodgrainSurface,
            onSurface = AtariTextPrimary,
            surfaceVariant = Color(0xFF332018),
            onSurfaceVariant = Color(0xFFD0B8A8),
            outline = Color(0xFF472E23)
        )
    }

    val strings = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AtariTypography,
            shapes = AtariShapes,
            content = content
        )
    }
}
