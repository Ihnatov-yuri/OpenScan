package com.openscan.scanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Type is the design. Four roles, each with a single job (see design principles):
 *  - Display / brand / finality  -> condensed grotesk  (approximated: bold sans, tight tracking)
 *  - Statement / authority / big numerals -> serif      (approximated: FontFamily.Serif ≈ Fraunces)
 *  - Metadata / captions / indexing -> monospace        (FontFamily.Monospace)
 *  - Body / lists / things you read -> humanist sans     (FontFamily.SansSerif ≈ Inter)
 */
val DisplayFamily = FontFamily.SansSerif
val SerifFamily = FontFamily.Serif
val MonoFamily = FontFamily.Monospace
val BodyFamily = FontFamily.SansSerif

/** Mono caps used for eyebrows, indices and metadata (e.g. "03 / PAGES"). */
val Eyebrow = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.5.sp
)

/** A statement headline in the serif voice. */
val SerifDisplay = TextStyle(
    fontFamily = SerifFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.5).sp
)

val Typography = Typography(
    // Brand / screen titles — condensed, tight, final.
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Statements use the serif voice.
    headlineSmall = SerifDisplay,
    headlineMedium = SerifDisplay.copy(fontSize = 36.sp, lineHeight = 40.sp),
    // Body — dense, 15–16px, humanist sans.
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    // Labels are metadata — mono caps.
    labelLarge = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.0.sp
    ),
    labelMedium = Eyebrow,
    labelSmall = Eyebrow.copy(fontSize = 10.sp, letterSpacing = 1.2.sp)
)
