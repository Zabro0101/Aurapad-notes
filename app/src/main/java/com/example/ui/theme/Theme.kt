package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497E),
    onPrimaryContainer = BentoBlueContainer,
    secondary = Color(0xFFD0BCFF),
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = BentoPurpleContainer,
    tertiary = Color(0xFFFFB784),
    tertiaryContainer = Color(0xFF5B3000),
    onTertiaryContainer = BentoOrangeContainer,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF43474E),
    outlineVariant = Color(0xFF2D3548)
)

private val LightColorScheme = lightColorScheme(
    primary = BentoBlue,
    onPrimary = Color.White,
    primaryContainer = BentoBlueContainer,
    onPrimaryContainer = BentoOnBlueContainer,
    secondary = BentoPurple,
    secondaryContainer = BentoPurpleContainer,
    onSecondaryContainer = BentoOnPurpleContainer,
    tertiary = BentoOrange,
    tertiaryContainer = BentoOrangeContainer,
    onTertiaryContainer = BentoOnOrangeContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFE2E8F0)
)

val BentoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to prioritize our Bento Grid theme colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BentoShapes,
        content = content
    )
}
