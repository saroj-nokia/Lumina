package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = LuminaDarkPrimary,
    onPrimary = LuminaDarkOnPrimary,
    primaryContainer = LuminaDarkPrimaryContainer,
    onPrimaryContainer = LuminaDarkOnPrimaryContainer,
    secondary = LuminaDarkSecondary,
    onSecondary = LuminaDarkOnSecondary,
    secondaryContainer = LuminaDarkSecondaryContainer,
    onSecondaryContainer = LuminaDarkOnSecondaryContainer,
    tertiary = LuminaDarkTertiary,
    onTertiary = LuminaDarkOnTertiary,
    tertiaryContainer = LuminaDarkTertiaryContainer,
    onTertiaryContainer = LuminaDarkOnTertiaryContainer,
    background = LuminaDarkBackground,
    onBackground = LuminaDarkOnBackground,
    surface = LuminaDarkSurface,
    onSurface = LuminaDarkOnSurface,
    surfaceVariant = LuminaDarkSurfaceVariant,
    onSurfaceVariant = LuminaDarkOnSurfaceVariant,
    outline = LuminaDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LuminaLightPrimary,
    onPrimary = LuminaLightOnPrimary,
    primaryContainer = LuminaLightPrimaryContainer,
    onPrimaryContainer = LuminaLightOnPrimaryContainer,
    secondary = LuminaLightSecondary,
    onSecondary = LuminaLightOnSecondary,
    secondaryContainer = LuminaLightSecondaryContainer,
    onSecondaryContainer = LuminaLightOnSecondaryContainer,
    tertiary = LuminaLightTertiary,
    onTertiary = LuminaLightOnTertiary,
    tertiaryContainer = LuminaLightTertiaryContainer,
    onTertiaryContainer = LuminaLightOnTertiaryContainer,
    background = LuminaLightBackground,
    onBackground = LuminaLightOnBackground,
    surface = LuminaLightSurface,
    onSurface = LuminaLightOnSurface,
    surfaceVariant = LuminaLightSurfaceVariant,
    onSurfaceVariant = LuminaLightOnSurfaceVariant,
    outline = LuminaLightOutline
)

// Standard M3 Shapes (Android 12 to 15)
val StandardShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Material 3 Expressive Shapes (Android 16+, SDK_INT >= 36)
val ExpressiveShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Immutable
data class ExpressiveMotionConfig(
    val isExpressiveActive: Boolean = false,
    val bouncySpring: androidx.compose.animation.core.SpringSpec<Float> = spring(
        dampingRatio = if (isExpressiveActive) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
        stiffness = if (isExpressiveActive) Spring.StiffnessMediumLow else Spring.StiffnessMedium
    )
)

val LocalExpressiveMotion = staticCompositionLocalOf { ExpressiveMotionConfig() }

@Composable
fun LuminaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isAndroid16Plus = Build.VERSION.SDK_INT >= 36

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Material 3 Expressive vs Standard M3 fallback
    val shapes = if (isAndroid16Plus) ExpressiveShapes else StandardShapes
    val motionConfig = ExpressiveMotionConfig(isExpressiveActive = isAndroid16Plus)

    CompositionLocalProvider(LocalExpressiveMotion provides motionConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = Typography,
            content = content
        )
    }
}
