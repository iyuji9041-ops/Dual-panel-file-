package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Enum constant names are kept so saved settings / other files keep compiling.
// Labels now describe the MP Manager themes.
enum class MaterialPaletteStyle(val label: String, val forcesAmoledBlack: Boolean) {
    FROSTED_AMOLED("MP Black (True AMOLED)", true),
    FROSTED_CHARCOAL("MP Dark (Dark grey)", false),
    AMOLED_SLATE("MP Dynamic (Wallpaper colors, black)", true)
}

/** Base MP dark scheme (Material 3 baseline, seed #AAC7FF). */
private val MpDarkBase = darkColorScheme(
    primary = MpPrimaryDark,
    onPrimary = MpOnPrimaryDark,
    primaryContainer = MpPrimaryContainerDark,
    onPrimaryContainer = MpOnPrimaryContainerDark,
    secondary = MpSecondaryDark,
    onSecondary = MpOnSecondaryDark,
    secondaryContainer = MpSecondaryContainerDark,
    onSecondaryContainer = MpOnSecondaryContainerDark,
    tertiary = MpTertiaryDark,
    onTertiary = MpOnTertiaryDark,
    tertiaryContainer = MpTertiaryContainerDark,
    onTertiaryContainer = MpOnTertiaryContainerDark,
    onBackground = MpOnSurfaceDark,
    onSurface = MpOnSurfaceDark,
    onSurfaceVariant = MpOnSurfaceVariantDark,
    outline = MpOutlineDark,
    outlineVariant = MpOutlineVariantDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/** MP "Black": everything pure black, like Theme.MyApp.Black. */
private val MpBlackScheme = MpDarkBase.copy(
    background = MpBlack,
    surface = MpBlack,
    surfaceVariant = MpBlackHigh,
    surfaceContainerLowest = MpBlack,
    surfaceContainerLow = MpBlackLow,
    surfaceContainer = MpBlackContainer,
    surfaceContainerHigh = MpBlackHigh,
    surfaceContainerHighest = MpBlackHighest
)

/** MP "Dark": dark grey, like Theme.MyApp.Dark. */
private val MpDarkScheme = MpDarkBase.copy(
    background = MpDarkBackground,
    surface = MpDarkBackground,
    surfaceVariant = Color(0xFF44474E),
    surfaceContainerLowest = MpDarkLowest,
    surfaceContainerLow = MpDarkLow,
    surfaceContainer = MpDarkContainer,
    surfaceContainerHigh = MpDarkHigh,
    surfaceContainerHighest = MpDarkHighest
)

/** MP "Light": Material 3 baseline light (Theme.MyApp.Light). */
private val MpLightScheme = lightColorScheme(
    primary = MpPrimaryLight,
    onPrimary = MpOnPrimaryLight,
    primaryContainer = MpPrimaryContainerLight,
    onPrimaryContainer = MpOnPrimaryContainerLight,
    secondary = MpSecondaryLight,
    secondaryContainer = MpSecondaryContainerLight,
    onSecondaryContainer = MpOnSecondaryContainerLight,
    tertiary = MpTertiaryLight,
    tertiaryContainer = MpTertiaryContainerLight,
    onTertiaryContainer = MpOnTertiaryContainerLight,
    background = MpBackgroundLight,
    surface = MpBackgroundLight,
    onBackground = MpOnSurfaceLight,
    onSurface = MpOnSurfaceLight,
    surfaceVariant = MpSurfaceVariantLight,
    onSurfaceVariant = MpOnSurfaceVariantLight,
    outline = MpOutlineLight,
    outlineVariant = MpOutlineVariantLight,
    surfaceContainerLowest = MpLowestLight,
    surfaceContainerLow = MpLowLight,
    surfaceContainer = MpContainerLight,
    surfaceContainerHigh = MpHighLight,
    surfaceContainerHighest = MpHighestLight
)

/** Default Material 3 shapes - MP Manager does not use custom rounded cards. */
val AppShapes = Shapes()

@Composable
fun DualFilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paletteStyle: MaterialPaletteStyle = MaterialPaletteStyle.FROSTED_AMOLED,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val canUseDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (!darkTheme) {
        // MP uses DynamicColors on Android 12+, baseline otherwise.
        if (canUseDynamic && paletteStyle == MaterialPaletteStyle.AMOLED_SLATE) {
            dynamicLightColorScheme(context)
        } else MpLightScheme
    } else {
        when (paletteStyle) {
            MaterialPaletteStyle.FROSTED_AMOLED -> MpBlackScheme
            MaterialPaletteStyle.FROSTED_CHARCOAL -> MpDarkScheme
            MaterialPaletteStyle.AMOLED_SLATE ->
                if (canUseDynamic) {
                    // Wallpaper accent colors, but pure-black surfaces (MP Theme.MyApp.Black)
                    dynamicDarkColorScheme(context).copy(
                        background = MpBlack,
                        surface = MpBlack,
                        surfaceVariant = MpBlackHigh,
                        surfaceContainerLowest = MpBlack,
                        surfaceContainerLow = MpBlackLow,
                        surfaceContainer = MpBlackContainer,
                        surfaceContainerHigh = MpBlackHigh,
                        surfaceContainerHighest = MpBlackHighest
                    )
                } else MpBlackScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
