package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Warm Neutral Palette (Matching Reference)
val MainBackground = Color(0xFFF7F5F1)   // Warm off-white linen canvas
val CardBackground = Color(0xFFFFFFFF)   // Pure white card surfaces
val SurfaceNeutral = Color(0xFFF0EEEA)   // Soft neutral container/inputs
val BorderSubtle = Color(0xFFE8E5DF)     // Delicate soft border (0.8.dp)

val TextPrimary = Color(0xFF191919)      // Primary dark heading/text
val TextSecondary = Color(0xFF777777)    // Muted secondary label grey
val TextTertiary = Color(0xFFA0A0A0)     // Subtle tertiary caption

// Actions
val PrimaryAction = Color(0xFF191919)    // Graphite/black action buttons
val PrimaryActionPressed = Color(0xFF333333)
val AccentSage = Color(0xFF5F7161)       // Subtle sage green accent

// Accents & Financial Status
val StatusGreen = Color(0xFF2E7D32)      // Approved / Paid / Positive
val StatusGreenBg = Color(0xFFE8F5E9)
val StatusGreenBorder = Color(0xFFC8E6C9)

val StatusAmber = Color(0xFFB45309)      // Pending approval / warning
val StatusAmberBg = Color(0xFFFEF3C7)
val StatusAmberBorder = Color(0xFFFDE68A)

val StatusRed = Color(0xFFC62828)        // You owe / Over budget / Rejected
val StatusRedBg = Color(0xFFFFEBEE)
val StatusRedBorder = Color(0xFFFFCDD2)

// Pure tones
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF090A0C)

// Backward-compatibility aliases for existing components
val Graphite900 = TextPrimary
val Graphite800 = Color(0xFF2A2A2A)
val Graphite700 = Color(0xFF444444)
val Graphite600 = Color(0xFF666666)
val Graphite500 = TextSecondary
val Graphite400 = TextTertiary
val Graphite300 = Color(0xFFD4D0C8)
val Graphite200 = BorderSubtle
val Graphite100 = SurfaceNeutral
val Graphite50 = MainBackground
