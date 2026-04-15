package com.admoseley.quietforaminute.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LibrarianPrimary,
    secondary = LibrarianSecondary,
    tertiary = LibrarianTertiary,
    background = LibrarianDarkBackground,
    surface = LibrarianDarkSurface,
    onBackground = LibrarianOnBackground,
    onSurface = LibrarianOnBackground,
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondaryContainer = Color(0xFF311B92),
    onSecondaryContainer = Color(0xFFD1C4E9),
    surfaceVariant = Color(0xFF252238),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    secondary = IndigoGrey40,
    tertiary = Teal40
)

@Composable
fun QuietTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our custom Librarian theme
    content: @Composable () -> Unit
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
        content = content
    )
}
