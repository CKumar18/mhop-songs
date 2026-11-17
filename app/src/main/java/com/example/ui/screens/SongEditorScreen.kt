package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.chord.ChordLyricParser
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.ui.viewmodel.AdminViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    songId: String?,
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onPreviewSong: (String) -> Unit,
) {
    var existingSong by remember { mutableStateOf<Song?>(null) }
    var isLoaded by remember { mutableStateOf(songId == null) }

    var title by remember { mutableStateOf("") }
    var alternateTitle by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(SongLanguage.TELUGU) }
    var lyricsText by remember { mutableStateOf("") }
    var originalKey by remember { mutableStateOf("") }
    var capo by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    var otherSourceUrl by remember { mutableStateOf("") }
    var lyricist by remember { mutableStateOf("") }
    var composer by remember { mutableStateOf("") }
    var translator by remember { mutableStateOf("") }
    var copyrightOwner by remember { mutableStateOf("") }
    var licenseInfo by remember { mutableStateOf("") }
    var additionalCredits by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(songId) {
        if (songId != null) {
            val loaded = viewModel.getSongById(songId)
            existingSong = loaded
            if (loaded != null) {
                title = loaded.title
                alternateTitle = loaded.alternateTitle
                language = loaded.language
                lyricsText = if (loaded.sections.isNotEmpty()) {
                    ChordLyricParser.sectionsToEditableText(loaded.sections)
                } else {
                    loaded.lyrics
                }
                originalKey = loaded.originalKey
                capo = loaded.capo
                category = loaded.category
                tagsText = loaded.tags.joinToString(", ")
                videoUrl = loaded.videoUrl
                audioUrl = loaded.audioUrl
                otherSourceUrl = loaded.otherSourceUrl
                lyricist = loaded.lyricist
                composer = loaded.composer
                translator = loaded.translator
                copyrightOwner = loaded.copyrightOwner
                licenseInfo = loaded.licenseInfo
                additionalCredits = loaded.additionalCredits
                isPublished = loaded.isPublished
            }
            isLoaded = true
        }
    }

    fun buildSongRecord(publishOverride: Boolean? = null): Song? {
        if (title.isBlank()) {
            validationError = "Please enter a song title."
            return null
        }
        if (lyricsText.isBlank()) {
            validationError = "Please enter song lyrics."
            return null
        }
        validationError = null
        val targetId = existingSong?.id ?: UUID.randomUUID().toString()
        val parsedTags = tagsText.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return Song(
            id = targetId,
            title = title.trim(),
            alternateTitle = alternateTitle.trim(),
            language = language,
            lyrics = lyricsText,
            originalKey = originalKey.trim(),
            capo = capo.trim(),
            category = category.trim(),
            tags = parsedTags,
            videoUrl = videoUrl.trim(),
            audioUrl = audioUrl.trim(),
            otherSourceUrl = otherSourceUrl.trim(),
            lyricist = lyricist.trim(),
            composer = composer.trim(),
            translator = translator.trim(),
            copyrightOwner = copyrightOwner.trim(),
            licenseInfo = licenseInfo.trim(),
            additionalCredits = additionalCredits.trim(),
            createdAt = existingSong?.createdAt ?: System.currentTimeMillis(),
            createdByAdminId = existingSong?.createdByAdminId ?: "",
            isPublished = publishOverride ?: isPublished,
            isFavourite = existingSong?.isFavourite ?: false,
            lastViewedAt = existingSong?.lastViewedAt,
            isLocalTestSample = existingSong?.isLocalTestSample ?: false,
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (songId == null) "Add Song" else "Edit Song") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading song...")
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                // Language selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Language",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SongLanguage.entries.forEach { lang ->
                            val selected = language == lang
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
                                    .clickable { language = lang }
                                    .testTag("editor_lang_${lang.code}"),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                ) {
                                    Text(lang.nativeLabel, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input"),
                )

                OutlinedTextField(
                    value = alternateTitle,
                    onValueChange = { alternateTitle = it },
                    label = { Text("Alternate / Transliterated Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = lyricsText,
                    onValueChange = { lyricsText = it },
                    label = { Text("Lyrics & Chords *") },
                    placeholder = {
                        Text("Use section headers (Pallavi:, Charanam 1:, Verse 1:, Chorus:, Bridge:). Add chords inline like [G]word or on the line above.")
                    },
                    minLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_lyrics_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = originalKey,
                        onValueChange = { originalKey = it },
                        label = { Text("Original Key") },
                        placeholder = { Text("e.g., G, Dm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = capo,
                        onValueChange = { capo = it },
                        label = { Text("Capo") },
                        placeholder = { Text("e.g., 2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("e.g., Worship, Praise, Hymn") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "Media & Source Links",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video URL (YouTube / Vimeo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = audioUrl,
                    onValueChange = { audioUrl = it },
                    label = { Text("Audio URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = otherSourceUrl,
                    onValueChange = { otherSourceUrl = it },
                    label = { Text("Other Source URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    text = "Credits & Copyright",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = lyricist,
                    onValueChange = { lyricist = it },
                    label = { Text("Lyricist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it },
                    label = { Text("Composer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = translator,
                    onValueChange = { translator = it },
                    label = { Text("Translator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = copyrightOwner,
                    onValueChange = { copyrightOwner = it },
                    label = { Text("Copyright Owner / Publisher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = licenseInfo,
                    onValueChange = { licenseInfo = it },
                    label = { Text("Copyright / License Information") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = additionalCredits,
                    onValueChange = { additionalCredits = it },
                    label = { Text("Additional Credits") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { isPublished = !isPublished },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Publish to Library", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (isPublished) "Visible to all church members" else "Saved as admin-only draft",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isPublished,
                        onCheckedChange = { isPublished = it },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val record = buildSongRecord() ?: return@OutlinedButton
                            viewModel.saveSong(record) { saved ->
                                existingSong = saved
                                onPreviewSong(saved.id)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("editor_preview_button"),
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview")
                    }

                    Button(
                        onClick = {
                            val record = buildSongRecord() ?: return@Button
                            viewModel.saveSong(record) { onBack() }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("editor_save_button"),
                    ) {
                        Text(if (isPublished) "Save & Publish" else "Save Draft")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
