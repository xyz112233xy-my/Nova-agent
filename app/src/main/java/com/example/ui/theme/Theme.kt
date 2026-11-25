package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val NovaCartDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = WarmGold,
    onSecondary = ObsidianEmerald,
    secondaryContainer = WarmGoldContainer,
    onSecondaryContainer = WarmGoldOnContainer,
    tertiary = MintTealTertiary,
    onTertiary = ObsidianEmerald,
    tertiaryContainer = MintTealContainer,
    onTertiaryContainer = EmeraldOnContainer,
    background = ObsidianEmerald,
    onBackground = TextPrimary,
    surface = ForestSlateDeep,
    onSurface = TextPrimary,
    surfaceVariant = ForestSlateSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianEmerald,
    surfaceContainerLow = ForestSlateDeep,
    surfaceContainer = ForestSlateSurface,
    surfaceContainerHigh = ForestSlateElevated,
    surfaceContainerHighest = ForestOutlineSubtle,
    outline = ForestOutline,
    outlineVariant = ForestOutlineSubtle,
    error = StatusDanger,
    onError = ObsidianEmerald,
    errorContainer = DangerContainer,
    onErrorContainer = DangerOnContainer
)

private val NovaCartShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun NovaCartTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NovaCartDarkColorScheme,
        typography = NovaCartTypography,
        shapes = NovaCartShapes,
        content = content
    )
}
