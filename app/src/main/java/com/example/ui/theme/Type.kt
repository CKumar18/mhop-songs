package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.FontFamilyOption
import com.example.domain.model.FontWeightOption
import com.example.domain.model.SongLanguage

val NotoSansFamily = FontFamily(
    Font(R.font.noto_sans, FontWeight.Normal),
    Font(R.font.noto_sans, FontWeight.Medium),
    Font(R.font.noto_sans, FontWeight.SemiBold),
    Font(R.font.noto_sans, FontWeight.Bold),
)

val NotoSerifFamily = FontFamily(
    Font(R.font.noto_serif, FontWeight.Normal),
    Font(R.font.noto_serif, FontWeight.Medium),
    Font(R.font.noto_serif, FontWeight.SemiBold),
    Font(R.font.noto_serif, FontWeight.Bold),
)

val NotoSansTeluguFamily = FontFamily(
    Font(R.font.noto_sans_telugu, FontWeight.Normal),
    Font(R.font.noto_sans_telugu, FontWeight.Medium),
    Font(R.font.noto_sans_telugu, FontWeight.SemiBold),
    Font(R.font.noto_sans_telugu, FontWeight.Bold),
)

val NotoSerifTeluguFamily = FontFamily(
    Font(R.font.noto_serif_telugu, FontWeight.Normal),
    Font(R.font.noto_serif_telugu, FontWeight.Medium),
    Font(R.font.noto_serif_telugu, FontWeight.SemiBold),
    Font(R.font.noto_serif_telugu, FontWeight.Bold),
)

val NotoSansDevanagariFamily = FontFamily(
    Font(R.font.noto_sans_devanagari, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari, FontWeight.Medium),
    Font(R.font.noto_sans_devanagari, FontWeight.SemiBold),
    Font(R.font.noto_sans_devanagari, FontWeight.Bold),
)

val NotoSerifDevanagariFamily = FontFamily(
    Font(R.font.noto_serif_devanagari, FontWeight.Normal),
    Font(R.font.noto_serif_devanagari, FontWeight.Medium),
    Font(R.font.noto_serif_devanagari, FontWeight.SemiBold),
    Font(R.font.noto_serif_devanagari, FontWeight.Bold),
)

/**
 * Resolves the user's chosen font family while guaranteeing full glyph support
 * for Telugu, Hindi (Devanagari), and English scripts.
 */
fun resolveSongFontFamily(
    option: FontFamilyOption,
    language: SongLanguage,
): FontFamily {
    return when (option) {
        FontFamilyOption.NOTO_SANS -> when (language) {
            SongLanguage.TELUGU -> NotoSansTeluguFamily
            SongLanguage.HINDI -> NotoSansDevanagariFamily
            SongLanguage.ENGLISH -> NotoSansFamily
        }
        FontFamilyOption.NOTO_SERIF -> when (language) {
            SongLanguage.TELUGU -> NotoSerifTeluguFamily
            SongLanguage.HINDI -> NotoSerifDevanagariFamily
            SongLanguage.ENGLISH -> NotoSerifFamily
        }
        FontFamilyOption.NOTO_SANS_TELUGU -> when (language) {
            SongLanguage.HINDI -> NotoSansDevanagariFamily
            else -> NotoSansTeluguFamily
        }
        FontFamilyOption.NOTO_SERIF_TELUGU -> when (language) {
            SongLanguage.HINDI -> NotoSerifDevanagariFamily
            else -> NotoSerifTeluguFamily
        }
        FontFamilyOption.NOTO_SANS_DEVANAGARI -> when (language) {
            SongLanguage.TELUGU -> NotoSansTeluguFamily
            else -> NotoSansDevanagariFamily
        }
        FontFamilyOption.NOTO_SERIF_DEVANAGARI -> when (language) {
            SongLanguage.TELUGU -> NotoSerifTeluguFamily
            else -> NotoSerifDevanagariFamily
        }
        FontFamilyOption.SYSTEM_SANS -> FontFamily.SansSerif
        FontFamilyOption.SYSTEM_SERIF -> FontFamily.Serif
    }
}

fun FontWeightOption.toComposeWeight(): FontWeight = when (this) {
    FontWeightOption.REGULAR -> FontWeight.Normal
    FontWeightOption.MEDIUM -> FontWeight.Medium
    FontWeightOption.SEMIBOLD -> FontWeight.SemiBold
    FontWeightOption.BOLD -> FontWeight.Bold
}

fun Boolean.toComposeFontStyle(): FontStyle = if (this) FontStyle.Italic else FontStyle.Normal

val MhopTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NotoSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)
