package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LensTokens(
    val isIos: Boolean,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val line: Color,
    val glass: Color,
    val fallbackColor: Color,
    val cameraGlass: Color,
    val fill: Color,
    val segmentActive: Color,
    val cameraHighlight: Color,
    val switchActive: Color,
    val cardRadius: Dp,
    val buttonRadius: Dp,
    val titleSize: TextUnit,
    val titleWeight: FontWeight,
    val titleLetterSpacing: TextUnit
)

val IosLightTokens = LensTokens(
    isIos = true,
    isDark = false,
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF000000),
    textSecondary = Color(0xFF6B6B73),
    accent = Color(0xFF007AFF),
    onAccent = Color(0xFFFFFFFF),
    line = Color(0x2E3C3C43),
    glass = Color(0xBCF9F9F9),
    fallbackColor = Color(0xEBF9F9F9),
    cameraGlass = Color(0x8028282C),
    fill = Color(0x1F767680),
    segmentActive = Color(0xFFFFFFFF),
    cameraHighlight = Color(0xFFFFD60A),
    switchActive = Color(0xFF34C759),
    cardRadius = 12.dp,
    buttonRadius = 14.dp,
    titleSize = 34.sp,
    titleWeight = FontWeight.Bold,
    titleLetterSpacing = (-0.4).sp
)

val IosDarkTokens = LensTokens(
    isIos = true,
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF98989F),
    accent = Color(0xFF0A84FF),
    onAccent = Color(0xFFFFFFFF),
    line = Color(0x80545458),
    glass = Color(0xBC1C1C1E),
    fallbackColor = Color(0xEB1C1C1E),
    cameraGlass = Color(0x8028282C),
    fill = Color(0x3D767680),
    segmentActive = Color(0xFF636366),
    cameraHighlight = Color(0xFFFFD60A),
    switchActive = Color(0xFF34C759),
    cardRadius = 12.dp,
    buttonRadius = 14.dp,
    titleSize = 34.sp,
    titleWeight = FontWeight.Bold,
    titleLetterSpacing = (-0.4).sp
)

val MaterialLightTokens = LensTokens(
    isIos = false,
    isDark = false,
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFF3EDF7),
    text = Color(0xFF1D1B20),
    textSecondary = Color(0xFF49454F),
    accent = Color(0xFF6750A4),
    onAccent = Color(0xFFFFFFFF),
    line = Color(0xFFCAC4D0),
    glass = Color(0xD1F3EDF7),
    fallbackColor = Color(0xEBF3EDF7),
    cameraGlass = Color(0x8028282C),
    fill = Color(0xFFE7E0EC),
    segmentActive = Color(0xFFE8DEF8),
    cameraHighlight = Color(0xFFD0BCFF),
    switchActive = Color(0xFF6750A4),
    cardRadius = 24.dp,
    buttonRadius = 99.dp,
    titleSize = 28.sp,
    titleWeight = FontWeight.Normal,
    titleLetterSpacing = 0.sp
)

val MaterialDarkTokens = LensTokens(
    isIos = false,
    isDark = true,
    background = Color(0xFF141218),
    surface = Color(0xFF211F26),
    text = Color(0xFFE6E0E9),
    textSecondary = Color(0xFFCAC4D0),
    accent = Color(0xFFD0BCFF),
    onAccent = Color(0xFF381E72),
    line = Color(0xFF49454F),
    glass = Color(0xD1211F26),
    fallbackColor = Color(0xEB211F26),
    cameraGlass = Color(0x8028282C),
    fill = Color(0xFF36343B),
    segmentActive = Color(0xFF4A4458),
    cameraHighlight = Color(0xFFD0BCFF),
    switchActive = Color(0xFFD0BCFF),
    cardRadius = 24.dp,
    buttonRadius = 99.dp,
    titleSize = 28.sp,
    titleWeight = FontWeight.Normal,
    titleLetterSpacing = 0.sp
)

val LocalLensTokens = staticCompositionLocalOf { IosLightTokens }

object LensTheme {
    val tokens: LensTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLensTokens.current
}

@Composable
fun LensAppTheme(
    designLook: String = "ios",
    appearance: String = "auto",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appearance) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val tokens = when {
        designLook == "ios" && !isDark -> IosLightTokens
        designLook == "ios" && isDark -> IosDarkTokens
        designLook != "ios" && !isDark -> MaterialLightTokens
        else -> MaterialDarkTokens
    }

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = tokens.accent,
            onPrimary = tokens.onAccent,
            background = tokens.background,
            onBackground = tokens.text,
            surface = tokens.surface,
            onSurface = tokens.text,
            surfaceVariant = tokens.fill,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.line
        )
    } else {
        lightColorScheme(
            primary = tokens.accent,
            onPrimary = tokens.onAccent,
            background = tokens.background,
            onBackground = tokens.text,
            surface = tokens.surface,
            onSurface = tokens.text,
            surfaceVariant = tokens.fill,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.line
        )
    }

    CompositionLocalProvider(LocalLensTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
