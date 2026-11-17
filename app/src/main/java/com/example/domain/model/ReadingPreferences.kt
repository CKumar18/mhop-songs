package com.example.domain.model

enum class FontFamilyOption(val label: String, val description: String) {
    NOTO_SANS("Noto Sans", "Clean modern sans-serif with Telugu and Hindi fallbacks"),
    NOTO_SERIF("Noto Serif", "Classic hymnbook serif with Telugu and Hindi fallbacks"),
    NOTO_SANS_TELUGU("Noto Sans Telugu", "Optimized for Telugu worship lyrics"),
    NOTO_SERIF_TELUGU("Noto Serif Telugu", "Traditional serif tuned for Telugu script"),
    NOTO_SANS_DEVANAGARI("Noto Sans Devanagari", "Optimized for Hindi Devanagari lyrics"),
    NOTO_SERIF_DEVANAGARI("Noto Serif Devanagari", "Traditional serif tuned for Hindi script"),
    SYSTEM_SANS("System Sans", "Device default sans-serif"),
    SYSTEM_SERIF("System Serif", "Device default serif"),
}

enum class FontWeightOption(val label: String, val weightValue: Int) {
    REGULAR("Regular", 400),
    MEDIUM("Medium", 500),
    SEMIBOLD("Semibold", 600),
    BOLD("Bold", 700),
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class ReadingCanvasMode(val label: String) {
    FOLLOW_APP("Match App Theme"),
    LIGHT("Light Paper"),
    DARK("Dark Sanctuary"),
}

data class ReadingPreferences(
    val titleFontSizeSp: Float = 24f,
    val lyricsFontSizeSp: Float = 18f,
    val fontFamilyOption: FontFamilyOption = FontFamilyOption.NOTO_SANS,
    val fontWeightOption: FontWeightOption = FontWeightOption.REGULAR,
    val isItalic: Boolean = false,
    val lineSpacingMultiplier: Float = 1.45f,
    val paragraphSpacingDp: Float = 16f,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val readingCanvasMode: ReadingCanvasMode = ReadingCanvasMode.FOLLOW_APP,
    val showChordsByDefault: Boolean = true,
) {
    companion object {
        const val MIN_TITLE_SIZE_SP = 18f
        const val MAX_TITLE_SIZE_SP = 36f
        const val MIN_LYRICS_SIZE_SP = 13f
        const val MAX_LYRICS_SIZE_SP = 30f
        const val MIN_LINE_SPACING = 1.15f
        const val MAX_LINE_SPACING = 2.0f
        const val MIN_PARAGRAPH_SPACING = 8f
        const val MAX_PARAGRAPH_SPACING = 32f
    }
}
