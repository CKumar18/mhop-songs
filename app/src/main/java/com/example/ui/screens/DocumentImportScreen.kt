package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.chord.ChordLyricParser
import com.example.domain.importer.DuplicateResolution
import com.example.domain.importer.ImportSummary
import com.example.domain.importer.ImportedSongDraft
import com.example.domain.model.SongLanguage
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.DocumentImportUiState
import com.example.ui.viewmodel.ImportStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentImportScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onReviewExistingSong: (String) -> Unit,
) {
    val state by viewModel.importState.collectAsStateWithLifecycle()

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.processSelectedDocuments(uris)
        }
    }

    val activeDraft = state.drafts.firstOrNull { it.draftId == state.activeDraftId }

    BackHandler(enabled = activeDraft != null) {
        viewModel.openDraftEditor(null)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            activeDraft != null -> "Edit Extracted Song"
                            state.step == ImportStep.REVIEW_SONGS -> "Review Imported Songs"
                            state.step == ImportStep.SUMMARY -> "Import Summary"
                            else -> "Import DOCX & PDF"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeDraft != null) {
                                viewModel.openDraftEditor(null)
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (activeDraft == null && state.step == ImportStep.REVIEW_SONGS && state.drafts.isNotEmpty()) {
                val includedCount = state.drafts.count {
                    it.isIncluded && it.duplicateResolution != DuplicateResolution.SKIP
                }
                val unresolvedDuplicates = state.drafts.count { it.hasUnresolvedDuplicate }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (unresolvedDuplicates > 0) {
                            Text(
                                text = "$unresolvedDuplicates song(s) match existing records. Choose Skip or Import as Version before saving.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.commitImport(publishImmediately = false) },
                                enabled = includedCount > 0 && unresolvedDuplicates == 0 && !state.isCommitting,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("import_save_drafts_button"),
                            ) {
                                Text("Save Drafts ($includedCount)")
                            }
                            Button(
                                onClick = { viewModel.commitImport(publishImmediately = true) },
                                enabled = includedCount > 0 && unresolvedDuplicates == 0 && !state.isCommitting,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("import_publish_button"),
                            ) {
                                Text("Publish ($includedCount)")
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (activeDraft != null) {
                DraftCorrectionEditor(
                    draft = activeDraft,
                    onSaveChanges = { updated ->
                        viewModel.updateDraft(updated)
                        viewModel.openDraftEditor(null)
                    },
                    onMoveSection = { idx, dir -> viewModel.moveSection(activeDraft.draftId, idx, dir) },
                    onRemoveDraft = {
                        viewModel.removeDraft(activeDraft.draftId)
                    },
                    onCancel = { viewModel.openDraftEditor(null) },
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 780.dp)
                        .align(Alignment.TopCenter),
                )
            } else {
                when (state.step) {
                    ImportStep.SELECT_FILES -> {
                        SelectFilesStep(
                            onSelectFiles = {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/pdf",
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                                .align(Alignment.TopCenter),
                        )
                    }

                    ImportStep.EXTRACTING -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Extracting text, detecting songs, and checking duplicates...",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    ImportStep.REVIEW_SONGS -> {
                        ReviewSongsStep(
                            state = state,
                            onEditDraft = { viewModel.openDraftEditor(it) },
                            onChangeLanguage = { id, lang -> viewModel.updateDraftLanguage(id, lang) },
                            onResolveDuplicate = { id, res -> viewModel.setDuplicateResolution(id, res) },
                            onReviewExisting = onReviewExistingSong,
                            onRemoveDraft = { viewModel.removeDraft(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 780.dp)
                                .align(Alignment.TopCenter),
                        )
                    }

                    ImportStep.SUMMARY -> {
                        state.summary?.let { summary ->
                            ImportSummaryStep(
                                summary = summary,
                                onDone = {
                                    viewModel.resetImportWorkflow()
                                    onBack()
                                },
                                onImportMore = {
                                    viewModel.resetImportWorkflow()
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 720.dp)
                                    .align(Alignment.TopCenter),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectFilesStep(
    onSelectFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FileUpload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Import Songs from DOCX or PDF",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "Select one or multiple Microsoft Word (.docx) or PDF (.pdf) songbook files from your device. Multiple songs inside a single document are automatically detected and separated for review before saving.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "What the Importer Preserves",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "- Telugu, Hindi, and English scripts with automatic language detection",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "- Section headings (Pallavi / పల్లవి, Charanam / చరణం, Verse, Chorus, Bridge)",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "- Chords, keys, credits, and embedded or listed source links",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "- Original extracted text retained side-by-side for manual correction",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "- Duplicate checking against existing library songs",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "- Scanned PDF detection (runs on-device OCR for Latin & Devanagari; marks Telugu scanned images for manual review)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Button(
            onClick = onSelectFiles,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("select_documents_button"),
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose DOCX or PDF Files")
        }
    }
}

@Composable
private fun ReviewSongsStep(
    state: DocumentImportUiState,
    onEditDraft: (String) -> Unit,
    onChangeLanguage: (String, SongLanguage) -> Unit,
    onResolveDuplicate: (String, DuplicateResolution) -> Unit,
    onReviewExisting: (String) -> Unit,
    onRemoveDraft: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${state.selectedFileNames.size} file(s) processed  |  ${state.drafts.size} song(s) detected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Tap any song below to edit title, language, lyrics, chords, section order, or view original extracted text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.extractionErrors.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "File Warnings / Errors",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        state.extractionErrors.forEach { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }

        items(state.drafts, key = { it.draftId }) { draft ->
            DraftReviewCard(
                draft = draft,
                onEdit = { onEditDraft(draft.draftId) },
                onChangeLanguage = { lang -> onChangeLanguage(draft.draftId, lang) },
                onResolveDuplicate = { res -> onResolveDuplicate(draft.draftId, res) },
                onReviewExisting = onReviewExisting,
                onRemove = { onRemoveDraft(draft.draftId) },
            )
        }
    }
}

@Composable
private fun DraftReviewCard(
    draft: ImportedSongDraft,
    onEdit: () -> Unit,
    onChangeLanguage: (SongLanguage) -> Unit,
    onResolveDuplicate: (DuplicateResolution) -> Unit,
    onReviewExisting: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val isSkipped = !draft.isIncluded || draft.duplicateResolution == DuplicateResolution.SKIP

    Surface(
        color = if (isSkipped) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (draft.hasUnresolvedDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "From: ${draft.sourceFileName}  |  ${draft.sections.size} section(s)" +
                            if (draft.originalKey.isNotBlank()) "  |  Key ${draft.originalKey}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit ${draft.title}")
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove ${draft.title}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Quick language override
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SongLanguage.entries.forEach { lang ->
                    val selected = draft.language == lang
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clickable { onChangeLanguage(lang) },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(lang.nativeLabel, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (draft.warnings.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        draft.warnings.forEach { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Duplicate warning & resolution actions
            if (draft.duplicateMatches.isNotEmpty()) {
                val topMatch = draft.duplicateMatches.first()
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Possible Duplicate: \"${topMatch.existingSong.title}\"",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = topMatch.reason.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onResolveDuplicate(DuplicateResolution.SKIP) },
                                modifier = Modifier.heightIn(min = 44.dp),
                            ) {
                                Text(if (draft.duplicateResolution == DuplicateResolution.SKIP) "Skipped" else "Skip")
                            }
                            TextButton(
                                onClick = { onReviewExisting(topMatch.existingSong.id) },
                                modifier = Modifier.heightIn(min = 44.dp),
                            ) {
                                Text("Review Existing")
                            }
                            Button(
                                onClick = { onResolveDuplicate(DuplicateResolution.IMPORT_AS_VERSION) },
                                modifier = Modifier.heightIn(min = 44.dp),
                            ) {
                                Text(
                                    if (draft.duplicateResolution == DuplicateResolution.IMPORT_AS_VERSION) {
                                        "Keeping Version"
                                    } else {
                                        "Import Version"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCorrectionEditor(
    draft: ImportedSongDraft,
    onSaveChanges: (ImportedSongDraft) -> Unit,
    onMoveSection: (Int, Int) -> Unit,
    onRemoveDraft: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(draft.draftId) { mutableStateOf(draft.title) }
    var alternateTitle by remember(draft.draftId) { mutableStateOf(draft.alternateTitle) }
    var language by remember(draft.draftId) { mutableStateOf(draft.language) }
    var lyrics by remember(draft.draftId, draft.rawLyrics) {
        mutableStateOf(
            if (draft.sections.isNotEmpty()) {
                ChordLyricParser.sectionsToEditableText(draft.sections)
            } else {
                draft.rawLyrics
            }
        )
    }
    var originalKey by remember(draft.draftId) { mutableStateOf(draft.originalKey) }
    var capo by remember(draft.draftId) { mutableStateOf(draft.capo) }
    var category by remember(draft.draftId) { mutableStateOf(draft.category) }
    var videoUrl by remember(draft.draftId) { mutableStateOf(draft.videoUrl) }
    var audioUrl by remember(draft.draftId) { mutableStateOf(draft.audioUrl) }
    var lyricist by remember(draft.draftId) { mutableStateOf(draft.lyricist) }
    var composer by remember(draft.draftId) { mutableStateOf(draft.composer) }
    var showRawText by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Song Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = alternateTitle,
            onValueChange = { alternateTitle = it },
            label = { Text("Alternate Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { language = lang },
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(lang.nativeLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Section reordering controls
        if (draft.sections.size > 1) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Reorder Detected Sections",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    draft.sections.forEachIndexed { idx, section ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${idx + 1}. ${section.label} (${section.lines.size} lines)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onMoveSection(idx, -1) },
                                enabled = idx > 0,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move section up")
                            }
                            IconButton(
                                onClick = { onMoveSection(idx, 1) },
                                enabled = idx < draft.sections.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move section down")
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = lyrics,
            onValueChange = { lyrics = it },
            label = { Text("Structured Lyrics & Chords") },
            minLines = 9,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = originalKey,
                onValueChange = { originalKey = it },
                label = { Text("Key") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = capo,
                onValueChange = { capo = it },
                label = { Text("Capo") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = videoUrl,
            onValueChange = { videoUrl = it },
            label = { Text("Video URL") },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = lyricist,
                onValueChange = { lyricist = it },
                label = { Text("Lyricist") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = composer,
                onValueChange = { composer = it },
                label = { Text("Composer") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        // Original extracted text comparison
        TextButton(
            onClick = { showRawText = !showRawText },
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text(if (showRawText) "Hide Original Extracted Text" else "Compare Original Extracted Text")
        }

        if (showRawText) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = draft.rawExtractedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onRemoveDraft,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Text("Remove Song")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSaveChanges(
                        draft.copy(
                            title = title,
                            alternateTitle = alternateTitle,
                            language = language,
                            rawLyrics = lyrics,
                            originalKey = originalKey,
                            capo = capo,
                            category = category,
                            videoUrl = videoUrl,
                            audioUrl = audioUrl,
                            lyricist = lyricist,
                            composer = composer,
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun ImportSummaryStep(
    summary: ImportSummary,
    onDone: () -> Unit,
    onImportMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Import Complete",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryLine("Documents processed", summary.documentsProcessed.toString())
                SummaryLine("Songs detected", summary.songsDetected.toString())
                SummaryLine(
                    if (summary.importedAsPublished) "Published to library" else "Saved as drafts",
                    summary.songsImported.toString(),
                )
                SummaryLine("Flagged for review", summary.songsRequiringReview.toString())
                SummaryLine("Duplicates skipped", summary.duplicatesSkipped.toString())
            }
        }

        if (summary.errors.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Extraction Errors",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    summary.errors.forEach { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onImportMore,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                Text("Import More")
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("import_summary_done_button"),
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
