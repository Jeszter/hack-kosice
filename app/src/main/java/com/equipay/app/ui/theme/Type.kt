package com.equipay.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val sans = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.5.sp
    )
)
