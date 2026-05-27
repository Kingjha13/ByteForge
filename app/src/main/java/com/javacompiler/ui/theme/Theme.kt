package com.javacompiler.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext
val EditorBackground   = Color(0xFF0D1117)
val EditorSurface      = Color(0xFF161B22)
val EditorBorder       = Color(0xFF30363D)
val EditorText         = Color(0xFFE6EDF3)
val EditorComment      = Color(0xFF8B949E)
val AccentBlue         = Color(0xFF58A6FF)
val AccentGreen        = Color(0xFF3FB950)
val AccentRed          = Color(0xFFFF7B72)
val AccentYellow       = Color(0xFFE3B341)

private val DarkColors = darkColorScheme(
    primary          = AccentBlue,
    background       = EditorBackground,
    surface          = EditorSurface,
    onBackground     = EditorText,
    onSurface        = EditorText,
    error            = AccentRed,
    outline          = EditorBorder
)

@Composable
fun JavaCompilerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}


private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40


)

@Composable
fun JavaCompilerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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