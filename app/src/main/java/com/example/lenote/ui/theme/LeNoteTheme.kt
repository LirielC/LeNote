package com.example.lenote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.example.lenote.data.AppLanguage
import com.example.lenote.ui.i18n.LeNoteStrings
import com.example.lenote.ui.i18n.LocalLeNoteStrings
import com.example.lenote.ui.i18n.resolveAppLanguage
import com.example.lenote.ui.i18n.stringsForLanguage

object LeNoteTheme {
    val colors: LeNoteExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLeNoteColors.current

    val strings: LeNoteStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalLeNoteStrings.current
}

@Composable
fun LeNoteTheme(
    darkTheme: Boolean = false,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LeNoteDarkColorScheme else LeNoteLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val strings = stringsForLanguage(resolveAppLanguage(appLanguage))

    CompositionLocalProvider(
        LocalLeNoteColors provides extendedColors,
        LocalLeNoteStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LeNoteTypography,
            content = content
        )
    }
}
