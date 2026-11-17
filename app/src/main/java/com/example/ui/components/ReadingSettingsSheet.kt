package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FontFamilyOption
import com.example.domain.model.FontWeightOption
import com.example.domain.model.ReadingCanvasMode
import com.example.domain.model.ReadingPreferences
import com.example.domain.model.SongLanguage
import com.example.ui.theme.MhopDarkBackground
import com.example.ui.theme.MhopDarkText
import com.example.ui.theme.MhopWhite
import com.example.ui.theme.MhopBlack
import com.example.ui.theme.resolveSongFontFamily
import com.example.ui.theme.toComposeFontStyle
import com.example.ui.theme.toComposeWeight
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsBottomSheet(
    preferences: ReadingPreferences,
    onUpdate: ((ReadingPreferences) -> ReadingPreferences) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ReadingSettingsContent(
            preferences = preferences,
            onUpdate = onUpdate,
            onReset = onReset,
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
fun ReadingSettingsContent(
    preferences: ReadingPreferences,
    onUpdate: ((ReadingPreferences) -> ReadingPreferences) -> Unit,
    onReset: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val previewFontWeight = preferences.fontWeightOption.toComposeWeight()
    val previewFontStyle = preferences.isItalic.toComposeFontStyle()
    val previewBg = when (preferences.readingCanvasMode) {
        ReadingCanvasMode.LIGHT -> MhopWhite
        ReadingCanvasMode.DARK -> MhopDarkBackground
        ReadingCanvasMode.FOLLOW_APP -> MaterialTheme.colorScheme.surfaceVariant
    }
    val previewInk = when (preferences.readingCanvasMode) {
        ReadingCanvasMode.LIGHT -> MhopBlack
        ReadingCanvasMode.DARK -> MhopDarkText
        ReadingCanvasMode.FOLLOW_APP -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reading & Typography",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (onClose != null) {
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("Done")
                }
            }
        }

        // Live multilingual preview
        Surface(
            color = previewBg,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(preferences.paragraphSpacingDp.dp / 2f),
            ) {
                Text(
                    text = "Live Multilingual Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Praise Title  |  స్తుతి గీతం  |  स्तुति गीत",
                    fontSize = preferences.titleFontSizeSp.sp,
                    lineHeight = (preferences.titleFontSizeSp * 1.3f).sp,
                    fontFamily = resolveSongFontFamily(preferences.fontFamilyOption, SongLanguage.ENGLISH),
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = previewFontStyle,
                    color = previewInk,
                )
                val lyricsSize = preferences.lyricsFontSizeSp.sp
                val lyricsLineHeight = (preferences.lyricsFontSizeSp * preferences.lineSpacingMultiplier).sp
                Text(
                    text = "తెలుగు: స్తోత్రార్హుడా యేసయ్యా నిత్యము నిన్నే స్తుతింతును",
                    fontSize = lyricsSize,
                    lineHeight = lyricsLineHeight,
                    fontFamily = resolveSongFontFamily(preferences.fontFamilyOption, SongLanguage.TELUGU),
                    fontWeight = previewFontWeight,
                    fontStyle = previewFontStyle,
                    color = previewInk,
                )
                Text(
                    text = "हिन्दी: स्तुति आराधना तेरे ही लिए प्रभु सदा",
                    fontSize = lyricsSize,
                    lineHeight = lyricsLineHeight,
                    fontFamily = resolveSongFontFamily(preferences.fontFamilyOption, SongLanguage.HINDI),
                    fontWeight = previewFontWeight,
                    fontStyle = previewFontStyle,
                    color = previewInk,
                )
                Text(
                    text = "English: Amazing grace how sweet the sound",
                    fontSize = lyricsSize,
                    lineHeight = lyricsLineHeight,
                    fontFamily = resolveSongFontFamily(preferences.fontFamilyOption, SongLanguage.ENGLISH),
                    fontWeight = previewFontWeight,
                    fontStyle = previewFontStyle,
                    color = previewInk,
                )
            }
        }

        // Title Font Size
        FontStepperSliderRow(
            label = "Song Title Size",
            valueLabel = "${preferences.titleFontSizeSp.roundToInt()} sp",
            value = preferences.titleFontSizeSp,
            range = ReadingPreferences.MIN_TITLE_SIZE_SP..ReadingPreferences.MAX_TITLE_SIZE_SP,
            onDecrement = {
                onUpdate {
                    it.copy(
                        titleFontSizeSp = (it.titleFontSizeSp - 1f)
                            .coerceIn(ReadingPreferences.MIN_TITLE_SIZE_SP, ReadingPreferences.MAX_TITLE_SIZE_SP)
                    )
                }
            },
            onIncrement = {
                onUpdate {
                    it.copy(
                        titleFontSizeSp = (it.titleFontSizeSp + 1f)
                            .coerceIn(ReadingPreferences.MIN_TITLE_SIZE_SP, ReadingPreferences.MAX_TITLE_SIZE_SP)
                    )
                }
            },
            onValueChange = { newVal -> onUpdate { it.copy(titleFontSizeSp = newVal) } },
            testTagPrefix = "title_size",
        )

        // Lyrics Font Size
        FontStepperSliderRow(
            label = "Lyrics Size",
            valueLabel = "${preferences.lyricsFontSizeSp.roundToInt()} sp",
            value = preferences.lyricsFontSizeSp,
            range = ReadingPreferences.MIN_LYRICS_SIZE_SP..ReadingPreferences.MAX_LYRICS_SIZE_SP,
            onDecrement = {
                onUpdate {
                    it.copy(
                        lyricsFontSizeSp = (it.lyricsFontSizeSp - 1f)
                            .coerceIn(ReadingPreferences.MIN_LYRICS_SIZE_SP, ReadingPreferences.MAX_LYRICS_SIZE_SP)
                    )
                }
            },
            onIncrement = {
                onUpdate {
                    it.copy(
                        lyricsFontSizeSp = (it.lyricsFontSizeSp + 1f)
                            .coerceIn(ReadingPreferences.MIN_LYRICS_SIZE_SP, ReadingPreferences.MAX_LYRICS_SIZE_SP)
                    )
                }
            },
            onValueChange = { newVal -> onUpdate { it.copy(lyricsFontSizeSp = newVal) } },
            testTagPrefix = "lyrics_size",
        )

        // Line Spacing
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Line Spacing", style = MaterialTheme.typography.titleSmall)
                Text(
                    String.format("%.2fx", preferences.lineSpacingMultiplier),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = preferences.lineSpacingMultiplier,
                onValueChange = { value -> onUpdate { it.copy(lineSpacingMultiplier = value) } },
                valueRange = ReadingPreferences.MIN_LINE_SPACING..ReadingPreferences.MAX_LINE_SPACING,
                modifier = Modifier.testTag("line_spacing_slider"),
            )
        }

        // Paragraph Spacing
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Paragraph Spacing", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${preferences.paragraphSpacingDp.roundToInt()} dp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = preferences.paragraphSpacingDp,
                onValueChange = { value -> onUpdate { it.copy(paragraphSpacingDp = value) } },
                valueRange = ReadingPreferences.MIN_PARAGRAPH_SPACING..ReadingPreferences.MAX_PARAGRAPH_SPACING,
                modifier = Modifier.testTag("paragraph_spacing_slider"),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Font Family Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Font Family", style = MaterialTheme.typography.titleSmall)
            FontFamilyOption.entries.forEach { option ->
                val selected = preferences.fontFamilyOption == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onUpdate { it.copy(fontFamilyOption = option) } }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onUpdate { it.copy(fontFamilyOption = option) } },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Font Weight & Italic
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Font Weight", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FontWeightOption.entries.forEach { weightOption ->
                    val selected = preferences.fontWeightOption == weightOption
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { onUpdate { it.copy(fontWeightOption = weightOption) } }
                            .testTag("weight_${weightOption.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = weightOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onUpdate { it.copy(isItalic = !it.isItalic) } },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Italic Style", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Applied where supported by the selected script font",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preferences.isItalic,
                    onCheckedChange = { checked -> onUpdate { it.copy(isItalic = checked) } },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Reading Canvas Light/Dark Override
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reading Mode", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReadingCanvasMode.entries.forEach { mode ->
                    val selected = preferences.readingCanvasMode == mode
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { onUpdate { it.copy(readingCanvasMode = mode) } },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onUpdate { it.copy(showChordsByDefault = !it.showChordsByDefault) } },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Show Chords by Default", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = preferences.showChordsByDefault,
                    onCheckedChange = { checked -> onUpdate { it.copy(showChordsByDefault = checked) } },
                )
            }
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("reset_reading_settings_button"),
        ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset Reading Settings to Default")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FontStepperSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueChange: (Float) -> Unit,
    testTagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}_decrease"),
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "Decrease $label")
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_slider"),
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}_increase"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Increase $label")
            }
        }
    }
}
