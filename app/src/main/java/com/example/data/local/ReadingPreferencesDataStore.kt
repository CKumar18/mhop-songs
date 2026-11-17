package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeMode
import com.example.domain.model.FontFamilyOption
import com.example.domain.model.FontWeightOption
import com.example.domain.model.ReadingCanvasMode
import com.example.domain.model.ReadingPreferences
import com.example.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readingPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mhop_reading_prefs")

class ReadingPreferencesDataStore(
    private val context: Context,
) : PreferencesRepository {

    private object Keys {
        val TITLE_FONT_SIZE = floatPreferencesKey("title_font_size")
        val LYRICS_FONT_SIZE = floatPreferencesKey("lyrics_font_size")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_WEIGHT = stringPreferencesKey("font_weight")
        val IS_ITALIC = booleanPreferencesKey("is_italic")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val READING_CANVAS_MODE = stringPreferencesKey("reading_canvas_mode")
        val SHOW_CHORDS_BY_DEFAULT = booleanPreferencesKey("show_chords_by_default")
    }

    override val readingPreferences: Flow<ReadingPreferences> = context.readingPrefsDataStore.data.map { prefs ->
        ReadingPreferences(
            titleFontSizeSp = prefs[Keys.TITLE_FONT_SIZE] ?: 24f,
            lyricsFontSizeSp = prefs[Keys.LYRICS_FONT_SIZE] ?: 18f,
            fontFamilyOption = prefs[Keys.FONT_FAMILY]?.let { name ->
                FontFamilyOption.entries.firstOrNull { it.name == name }
            } ?: FontFamilyOption.NOTO_SANS,
            fontWeightOption = prefs[Keys.FONT_WEIGHT]?.let { name ->
                FontWeightOption.entries.firstOrNull { it.name == name }
            } ?: FontWeightOption.REGULAR,
            isItalic = prefs[Keys.IS_ITALIC] ?: false,
            lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: 1.45f,
            paragraphSpacingDp = prefs[Keys.PARAGRAPH_SPACING] ?: 16f,
            appThemeMode = prefs[Keys.APP_THEME_MODE]?.let { name ->
                AppThemeMode.entries.firstOrNull { it.name == name }
            } ?: AppThemeMode.SYSTEM,
            readingCanvasMode = prefs[Keys.READING_CANVAS_MODE]?.let { name ->
                ReadingCanvasMode.entries.firstOrNull { it.name == name }
            } ?: ReadingCanvasMode.FOLLOW_APP,
            showChordsByDefault = prefs[Keys.SHOW_CHORDS_BY_DEFAULT] ?: true,
        )
    }

    override suspend fun updateReadingPreferences(transform: (ReadingPreferences) -> ReadingPreferences) {
        context.readingPrefsDataStore.edit { prefs ->
            val current = ReadingPreferences(
                titleFontSizeSp = prefs[Keys.TITLE_FONT_SIZE] ?: 24f,
                lyricsFontSizeSp = prefs[Keys.LYRICS_FONT_SIZE] ?: 18f,
                fontFamilyOption = prefs[Keys.FONT_FAMILY]?.let { n -> FontFamilyOption.entries.firstOrNull { it.name == n } }
                    ?: FontFamilyOption.NOTO_SANS,
                fontWeightOption = prefs[Keys.FONT_WEIGHT]?.let { n -> FontWeightOption.entries.firstOrNull { it.name == n } }
                    ?: FontWeightOption.REGULAR,
                isItalic = prefs[Keys.IS_ITALIC] ?: false,
                lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: 1.45f,
                paragraphSpacingDp = prefs[Keys.PARAGRAPH_SPACING] ?: 16f,
                appThemeMode = prefs[Keys.APP_THEME_MODE]?.let { n -> AppThemeMode.entries.firstOrNull { it.name == n } }
                    ?: AppThemeMode.SYSTEM,
                readingCanvasMode = prefs[Keys.READING_CANVAS_MODE]?.let { n -> ReadingCanvasMode.entries.firstOrNull { it.name == n } }
                    ?: ReadingCanvasMode.FOLLOW_APP,
                showChordsByDefault = prefs[Keys.SHOW_CHORDS_BY_DEFAULT] ?: true,
            )
            val updated = transform(current)
            prefs[Keys.TITLE_FONT_SIZE] = updated.titleFontSizeSp
            prefs[Keys.LYRICS_FONT_SIZE] = updated.lyricsFontSizeSp
            prefs[Keys.FONT_FAMILY] = updated.fontFamilyOption.name
            prefs[Keys.FONT_WEIGHT] = updated.fontWeightOption.name
            prefs[Keys.IS_ITALIC] = updated.isItalic
            prefs[Keys.LINE_SPACING] = updated.lineSpacingMultiplier
            prefs[Keys.PARAGRAPH_SPACING] = updated.paragraphSpacingDp
            prefs[Keys.APP_THEME_MODE] = updated.appThemeMode.name
            prefs[Keys.READING_CANVAS_MODE] = updated.readingCanvasMode.name
            prefs[Keys.SHOW_CHORDS_BY_DEFAULT] = updated.showChordsByDefault
        }
    }

    override suspend fun resetReadingPreferences() {
        context.readingPrefsDataStore.edit { prefs ->
            val currentAppTheme = prefs[Keys.APP_THEME_MODE]
            prefs.clear()
            if (currentAppTheme != null) {
                prefs[Keys.APP_THEME_MODE] = currentAppTheme
            }
        }
    }
}
