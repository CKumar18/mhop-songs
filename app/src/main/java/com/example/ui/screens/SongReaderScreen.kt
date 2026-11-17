package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.chord.ChordLyricParser
import com.example.domain.chord.ChordTransposer
import com.example.domain.model.AppThemeMode
import com.example.domain.model.LyricLine
import com.example.domain.model.ReadingCanvasMode
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.ui.components.ReadingSettingsBottomSheet
import com.example.ui.theme.MhopBlack
import com.example.ui.theme.MhopBlue
import com.example.ui.theme.MhopDarkBackground
import com.example.ui.theme.MhopDarkOutline
import com.example.ui.theme.MhopDarkPrimary
import com.example.ui.theme.MhopDarkSurface
import com.example.ui.theme.MhopDarkText
import com.example.ui.theme.MhopDarkTextSecondary
import com.example.ui.theme.MhopDividerLight
import com.example.ui.theme.MhopInkSecondary
import com.example.ui.theme.MhopOffWhite
import com.example.ui.theme.MhopWhite
import com.example.ui.theme.resolveSongFontFamily
import com.example.ui.theme.toComposeFontStyle
import com.example.ui.theme.toComposeWeight
import com.example.ui.viewmodel.SongReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongReaderScreen(
    songId: String,
    isAdmin: Boolean,
    viewModel: SongReaderViewModel,
    onBack: () -> Unit,
    onEditSong: (String) -> Unit,
) {
    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    val song by viewModel.song.collectAsStateWithLifecycle()
    val prefs by viewModel.readingPreferences.collectAsStateWithLifecycle()
    val showChords by viewModel.showChords.collectAsStateWithLifecycle()
    val semitoneOffset by viewModel.semitoneOffset.collectAsStateWithLifecycle()
    val isDistractionFree by viewModel.isDistractionFree.collectAsStateWithLifecycle()

    var showReadingSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val systemDark = isSystemInDarkTheme()
    val isDarkCanvas = when (prefs.readingCanvasMode) {
        ReadingCanvasMode.LIGHT -> false
        ReadingCanvasMode.DARK -> true
        ReadingCanvasMode.FOLLOW_APP -> when (prefs.appThemeMode) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
            AppThemeMode.SYSTEM -> systemDark
        }
    }

    val canvasBg = if (isDarkCanvas) MhopDarkBackground else MhopWhite
    val canvasSurface = if (isDarkCanvas) MhopDarkSurface else MhopOffWhite
    val canvasInk = if (isDarkCanvas) MhopDarkText else MhopBlack
    val canvasSecondary = if (isDarkCanvas) MhopDarkTextSecondary else MhopInkSecondary
    val canvasAccent = if (isDarkCanvas) MhopDarkPrimary else MhopBlue
    val canvasDivider = if (isDarkCanvas) MhopDarkOutline else MhopDividerLight

    Scaffold(
        containerColor = canvasBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AnimatedVisibility(visible = !isDistractionFree) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = canvasBg,
                        titleContentColor = canvasInk,
                        navigationIconContentColor = canvasInk,
                        actionIconContentColor = canvasInk,
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reader_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = {
                        Text(
                            text = song?.language?.nativeLabel ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = canvasAccent,
                        )
                    },
                    actions = {
                        val currentSong = song
                        if (currentSong != null) {
                            IconButton(
                                onClick = { viewModel.toggleFavourite() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reader_favourite_button"),
                            ) {
                                Icon(
                                    imageVector = if (currentSong.isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (currentSong.isFavourite) "Remove favourite" else "Add favourite",
                                    tint = if (currentSong.isFavourite) canvasAccent else canvasInk,
                                )
                            }
                        }
                        IconButton(
                            onClick = { showReadingSheet = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reader_typography_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TextFields,
                                contentDescription = "Reading settings",
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleDistractionFree() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reader_focus_mode_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fullscreen,
                                contentDescription = "Distraction-free reading mode",
                            )
                        }
                        if (isAdmin && currentSong != null) {
                            IconButton(
                                onClick = { onEditSong(currentSong.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reader_edit_song_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit song",
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            val currentSong = song
            if (!isDistractionFree && currentSong != null) {
                Surface(
                    color = canvasSurface,
                    border = BorderStroke(0.75.dp, canvasDivider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReaderBottomAction(
                            icon = Icons.Outlined.ContentCopy,
                            label = "Copy",
                            tint = canvasInk,
                            onClick = {
                                clipboard.setText(AnnotatedString(viewModel.buildPlainLyricsText(currentSong)))
                                Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            testTag = "reader_copy_button",
                        )
                        ReaderBottomAction(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            tint = canvasInk,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, currentSong.title)
                                    putExtra(Intent.EXTRA_TEXT, viewModel.buildShareText(currentSong))
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Song"))
                            },
                            testTag = "reader_share_button",
                        )
                        if (currentSong.hasChords) {
                            ReaderBottomAction(
                                icon = Icons.Outlined.MusicNote,
                                label = if (showChords) "Hide Chords" else "Show Chords",
                                tint = if (showChords) canvasAccent else canvasInk,
                                onClick = { viewModel.toggleChords() },
                                testTag = "reader_toggle_chords_button",
                            )
                        }
                        ReaderBottomAction(
                            icon = Icons.Outlined.TextFields,
                            label = "Text",
                            tint = canvasInk,
                            onClick = { showReadingSheet = true },
                            testTag = "reader_text_settings_bottom_button",
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val currentSong = song
        if (currentSong == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Song not found.", color = canvasSecondary)
            }
            return@Scaffold
        }

        val scriptFont = resolveSongFontFamily(prefs.fontFamilyOption, currentSong.language)
        val bodyWeight = prefs.fontWeightOption.toComposeWeight()
        val bodyStyle = prefs.isItalic.toComposeFontStyle()
        val displayKey = if (currentSong.originalKey.isNotBlank()) {
            ChordTransposer.transposeKey(currentSong.originalKey, semitoneOffset)
        } else {
            ""
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(canvasBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .then(
                        if (isDistractionFree) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                viewModel.setDistractionFree(false)
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                if (!currentSong.isPublished) {
                    Text(
                        text = "Draft Preview (Not yet published to community library)",
                        style = MaterialTheme.typography.labelMedium,
                        color = canvasAccent,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                // Song Title
                Text(
                    text = currentSong.title,
                    fontSize = prefs.titleFontSizeSp.sp,
                    lineHeight = (prefs.titleFontSizeSp * 1.28f).sp,
                    fontFamily = scriptFont,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = bodyStyle,
                    color = canvasInk,
                    modifier = Modifier.testTag("reader_song_title"),
                )

                if (currentSong.alternateTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong.alternateTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = canvasSecondary,
                    )
                }

                // Clean metadata line
                val metaParts = buildList {
                    add(currentSong.language.nativeLabel)
                    if (currentSong.category.isNotBlank()) add(currentSong.category)
                    if (displayKey.isNotBlank()) {
                        val suffix = if (semitoneOffset != 0) " (${if (semitoneOffset > 0) "+$semitoneOffset" else semitoneOffset})" else ""
                        add("Key $displayKey$suffix")
                    }
                    if (currentSong.capo.isNotBlank() && currentSong.capo != "0") {
                        add("Capo ${currentSong.capo}")
                    }
                }
                if (metaParts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = metaParts.joinToString("  |  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = canvasAccent,
                    )
                }

                // Chord Transposition Bar (only when song actually has chords)
                if (currentSong.hasChords && !isDistractionFree) {
                    Spacer(modifier = Modifier.height(14.dp))
                    ChordControlBar(
                        showChords = showChords,
                        semitoneOffset = semitoneOffset,
                        originalKey = currentSong.originalKey,
                        displayKey = displayKey,
                        surfaceColor = canvasSurface,
                        dividerColor = canvasDivider,
                        inkColor = canvasInk,
                        accentColor = canvasAccent,
                        onToggleChords = { viewModel.toggleChords() },
                        onTransposeDown = { viewModel.transposeBy(-1) },
                        onTransposeUp = { viewModel.transposeBy(1) },
                        onResetTranspose = { viewModel.resetTransposition() },
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(color = canvasDivider, thickness = 0.75.dp)
                Spacer(modifier = Modifier.height(22.dp))

                // Song Lyrics & Sections
                val sections = currentSong.sections.ifEmpty {
                    ChordLyricParser.parseToSections(currentSong.lyrics)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(prefs.paragraphSpacingDp.dp),
                ) {
                    sections.forEach { section ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val showSectionHeading = !section.label.equals("Lyrics", ignoreCase = true) || sections.size > 1
                            if (showSectionHeading) {
                                Text(
                                    text = section.label,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = scriptFont,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = canvasAccent,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }

                            section.lines.forEach { line ->
                                LyricChordLineView(
                                    line = line,
                                    showChords = showChords,
                                    semitoneOffset = semitoneOffset,
                                    fontSizeSp = prefs.lyricsFontSizeSp,
                                    lineSpacingMultiplier = prefs.lineSpacingMultiplier,
                                    fontFamily = scriptFont,
                                    fontWeight = bodyWeight,
                                    fontStyle = bodyStyle,
                                    inkColor = canvasInk,
                                    chordColor = canvasAccent,
                                )
                            }
                        }
                    }
                }

                // Source links & Credits (hidden in distraction-free mode to keep lyrics pure)
                if (!isDistractionFree && (currentSong.hasSourceLinks || currentSong.hasAnyCredits || currentSong.tags.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = canvasDivider, thickness = 0.75.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (currentSong.hasSourceLinks) {
                        Text(
                            text = "Media & Source Links",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = canvasInk,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (currentSong.videoUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { openExternalUrl(context, currentSong.videoUrl) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("open_video_link_button"),
                                ) {
                                    Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Video")
                                }
                            }
                            if (currentSong.audioUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { openExternalUrl(context, currentSong.audioUrl) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("open_audio_link_button"),
                                ) {
                                    Icon(Icons.Outlined.Audiotrack, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Audio")
                                }
                            }
                            if (currentSong.otherSourceUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { openExternalUrl(context, currentSong.otherSourceUrl) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("open_other_link_button"),
                                ) {
                                    Icon(Icons.Outlined.Link, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Source")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (currentSong.hasAnyCredits) {
                        Text(
                            text = "Credits & Information",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = canvasInk,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CreditRow("Lyricist", currentSong.lyricist, canvasSecondary, canvasInk)
                            CreditRow("Composer", currentSong.composer, canvasSecondary, canvasInk)
                            CreditRow("Translator", currentSong.translator, canvasSecondary, canvasInk)
                            CreditRow("Copyright", currentSong.copyrightOwner, canvasSecondary, canvasInk)
                            CreditRow("License", currentSong.licenseInfo, canvasSecondary, canvasInk)
                            CreditRow("Notes", currentSong.additionalCredits, canvasSecondary, canvasInk)
                        }
                    }

                    if (currentSong.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tags: ${currentSong.tags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = canvasSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }

            // Subtle floating exit button in distraction-free mode
            if (isDistractionFree) {
                Surface(
                    color = canvasSurface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, canvasDivider),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    IconButton(
                        onClick = { viewModel.setDistractionFree(false) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("exit_focus_mode_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FullscreenExit,
                            contentDescription = "Exit distraction-free mode",
                            tint = canvasInk,
                        )
                    }
                }
            }
        }
    }

    if (showReadingSheet) {
        ReadingSettingsBottomSheet(
            preferences = prefs,
            onUpdate = { viewModel.updateReadingPreferences(it) },
            onReset = { viewModel.resetReadingPreferences() },
            onDismiss = { showReadingSheet = false },
        )
    }
}

@Composable
private fun LyricChordLineView(
    line: LyricLine,
    showChords: Boolean,
    semitoneOffset: Int,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontStyle: androidx.compose.ui.text.font.FontStyle,
    inkColor: androidx.compose.ui.graphics.Color,
    chordColor: androidx.compose.ui.graphics.Color,
) {
    if (line.lyrics.isBlank() && line.chords.isEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showChords && line.chords.isNotEmpty()) {
            val chordRowText = ChordLyricParser.formatChordOverlayRow(line, semitoneOffset)
            Text(
                text = chordRowText,
                fontSize = (fontSizeSp * 0.82f).sp,
                lineHeight = (fontSizeSp * 1.05f).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = chordColor,
            )
        }
        if (line.lyrics.isNotBlank()) {
            Text(
                text = line.lyrics,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = inkColor,
            )
        }
    }
}

@Composable
private fun ChordControlBar(
    showChords: Boolean,
    semitoneOffset: Int,
    originalKey: String,
    displayKey: String,
    surfaceColor: androidx.compose.ui.graphics.Color,
    dividerColor: androidx.compose.ui.graphics.Color,
    inkColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onToggleChords: () -> Unit,
    onTransposeDown: () -> Unit,
    onTransposeUp: () -> Unit,
    onResetTranspose: () -> Unit,
) {
    Surface(
        color = surfaceColor,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, dividerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onToggleChords,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Text(
                    text = if (showChords) "Hide Chords" else "Show Chords",
                    color = accentColor,
                )
            }

            if (showChords) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTransposeDown,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("transpose_down_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = "Transpose down one semitone",
                            tint = inkColor,
                        )
                    }
                    val transposeLabel = when {
                        displayKey.isNotBlank() -> displayKey
                        semitoneOffset == 0 -> "Original"
                        semitoneOffset > 0 -> "+$semitoneOffset"
                        else -> "$semitoneOffset"
                    }
                    Text(
                        text = transposeLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = inkColor,
                        modifier = Modifier
                            .clickable(enabled = semitoneOffset != 0, onClick = onResetTranspose)
                            .padding(horizontal = 6.dp),
                    )
                    IconButton(
                        onClick = onTransposeUp,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("transpose_up_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Transpose up one semitone",
                            tint = inkColor,
                        )
                    }
                    if (semitoneOffset != 0 && originalKey.isNotBlank()) {
                        TextButton(onClick = onResetTranspose) {
                            Text("Reset", color = accentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun CreditRow(
    label: String,
    value: String,
    labelColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = labelColor,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun openExternalUrl(context: android.content.Context, rawUrl: String) {
    val trimmed = rawUrl.trim()
    val normalized = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open link: $normalized", Toast.LENGTH_SHORT).show()
    }
}
