package com.studyflow.ui.theme

import androidx.compose.ui.graphics.Color

// Dark-first palette (spec §8)
val Background = Color(0xFF0A0A0F)
val Surface = Color(0xFF14141C)
val SurfaceVariant = Color(0xFF1E1E2A)
val Primary = Color(0xFF7C6AF7)
val Secondary = Color(0xFF48C9B0)
val Error = Color(0xFFFF6B6B)
val OnSurface = Color(0xFFE8E8F0)
val Muted = Color(0xFF6B6B80)

// Mode badge colors
val ModeTheory = Color(0xFF5B8AF0)
val ModePractice = Color(0xFFF0955B)
val ModeRevision = Color(0xFFB05BF0)
val ModeMockTest = Color(0xFFF05B5B)

// Light variants for light theme
val LightBackground = Color(0xFFF7F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE5E5EF)
val LightPrimary = Color(0xFF5E4FE0)
val LightOnSurface = Color(0xFF1A1A24)

fun modeColor(mode: String?): Color = when (mode) {
    "theory" -> ModeTheory
    "practice" -> ModePractice
    "revision" -> ModeRevision
    "mock_test" -> ModeMockTest
    else -> Primary
}
