package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HubDarkColorScheme = darkColorScheme(
    primary = HubGreenLight,
    onPrimary = Color(0xFF052E16),
    primaryContainer = HubGreenPrimary,
    onPrimaryContainer = HubGreenContainer,
    secondary = HubAmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = HubAmberContainer,
    tertiary = HubTealTertiary,
    background = HubSurfaceDark,
    surface = HubCardDark,
    onBackground = HubTextPrimaryDark,
    onSurface = HubTextPrimaryDark,
    onSurfaceVariant = HubTextSecondaryDark,
    outline = HubBorderDark,
    error = ErrorRed,
    errorContainer = Color(0xFF7F1D1D)
)

private val HubLightColorScheme = lightColorScheme(
    primary = HubGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = HubGreenContainer,
    onPrimaryContainer = OnHubGreenContainer,
    secondary = HubGreenMid,
    onSecondary = Color.White,
    secondaryContainer = HubGreenSoftBg,
    onSecondaryContainer = HubGreenPrimary,
    tertiary = HubTealTertiary,
    tertiaryContainer = HubTealContainer,
    onTertiaryContainer = OnHubTealContainer,
    background = HubSurfaceLight,
    surface = HubCardLight,
    surfaceVariant = HubBackgroundLight,
    onBackground = HubTextPrimaryLight,
    onSurface = HubTextPrimaryLight,
    onSurfaceVariant = HubTextSecondaryLight,
    outline = HubBorderLight,
    error = ErrorRed,
    errorContainer = ErrorContainer
)

@Composable
fun CollectionHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HubDarkColorScheme
        else -> HubLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = CollectionHubTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
