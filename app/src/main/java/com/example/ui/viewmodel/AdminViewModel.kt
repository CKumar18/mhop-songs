package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.chord.ChordLyricParser
import com.example.domain.importer.DocumentExtractor
import com.example.domain.importer.DuplicateResolution
import com.example.domain.importer.ImportSummary
import com.example.domain.importer.ImportedSongDraft
import com.example.domain.importer.SongParserEngine
import com.example.domain.model.ImportHistoryRecord
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SongSection
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ImportStep {
    SELECT_FILES,
    EXTRACTING,
    REVIEW_SONGS,
    SUMMARY
}

data class DocumentImportUiState(
    val step: ImportStep = ImportStep.SELECT_FILES,
    val selectedFileNames: List<String> = emptyList(),
    val drafts: List<ImportedSongDraft> = emptyList(),
    val activeDraftId: String? = null,
    val extractionErrors: List<String> = emptyList(),
    val isCommitting: Boolean = false,
    val summary: ImportSummary? = null,
)

class AdminViewModel(
    private val songRepository: SongRepository,
    private val authRepository: AuthRepository,
    private val documentExtractor: DocumentExtractor,
) : ViewModel() {

    val allSongs: StateFlow<List<Song>> = songRepository.observeAllSongsForAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importHistory: StateFlow<List<ImportHistoryRecord>> = songRepository.observeImportHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow(DocumentImportUiState())
    val importState: StateFlow<DocumentImportUiState> = _importState.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    fun clearMessage() {
        _adminMessage.value = null
    }

    suspend fun getSongById(songId: String): Song? = songRepository.getSongById(songId)

    fun saveSong(song: Song, onSaved: (Song) -> Unit = {}) {
        viewModelScope.launch {
            val adminId = authRepository.currentSession.value.uid ?: "local-preview-admin"
            val parsedSections = ChordLyricParser.parseToSections(song.lyrics)
            val plainLyrics = ChordLyricParser.sectionsToPlainLyrics(parsedSections).ifBlank { song.lyrics }
            val chordsSummary = ChordLyricParser.extractChordSummary(parsedSections)
            val normalized = song.copy(
                lyrics = plainLyrics,
                sections = parsedSections,
                chords = chordsSummary,
                createdByAdminId = song.createdByAdminId.ifBlank { adminId },
                updatedAt = System.currentTimeMillis(),
            )
            songRepository.upsertSong(normalized)
                .onSuccess { saved ->
                    _adminMessage.value = if (saved.isPublished) "Song published to library." else "Song saved as draft."
                    onSaved(saved)
                }
                .onFailure {
                    _adminMessage.value = "Failed to save song: ${it.message ?: "unknown error"}"
                }
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            songRepository.deleteSong(songId)
                .onSuccess { _adminMessage.value = "Song deleted." }
                .onFailure { _adminMessage.value = "Delete failed: ${it.message ?: "unknown error"}" }
        }
    }

    fun togglePublish(songId: String, currentlyPublished: Boolean) {
        viewModelScope.launch {
            songRepository.setPublished(songId, !currentlyPublished)
                .onSuccess {
                    _adminMessage.value = if (!currentlyPublished) "Song published." else "Song moved to drafts."
                }
        }
    }

    fun loadLocalTestSongs() {
        viewModelScope.launch {
            songRepository.seedLocalTestSongs()
                .onSuccess { count ->
                    _adminMessage.value = "Loaded $count clearly labeled local test songs."
                }
        }
    }

    fun clearLocalTestSongs() {
        viewModelScope.launch {
            songRepository.clearLocalTestSongs()
                .onSuccess { count ->
                    _adminMessage.value = "Removed $count local test songs."
                }
        }
    }

    // ---- Document Import Workflow ----

    fun resetImportWorkflow() {
        _importState.value = DocumentImportUiState()
    }

    fun processSelectedDocuments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importState.value = DocumentImportUiState(step = ImportStep.EXTRACTING)

            val fileNames = mutableListOf<String>()
            val allDrafts = mutableListOf<ImportedSongDraft>()
            val errors = mutableListOf<String>()

            for (uri in uris) {
                val extracted = documentExtractor.extractFromUri(uri)
                fileNames.add(extracted.fileName)

                if (extracted.errorMessage != null && extracted.rawText.isBlank()) {
                    errors.add("${extracted.fileName}: ${extracted.errorMessage}")
                    continue
                }

                val parsedSongs = SongParserEngine.parseDocumentIntoSongs(
                    fileName = extracted.fileName,
                    rawText = extracted.rawText,
                    documentLinks = extracted.extractedLinks,
                    baseWarnings = extracted.warnings,
                )

                if (parsedSongs.isEmpty()) {
                    errors.add("${extracted.fileName}: No songs could be detected in the extracted text.")
                } else {
                    for (draft in parsedSongs) {
                        val duplicates = songRepository.findPotentialDuplicates(
                            title = draft.title,
                            language = draft.language,
                            lyrics = draft.rawLyrics,
                        )
                        allDrafts.add(
                            draft.copy(duplicateMatches = duplicates)
                        )
                    }
                }
            }

            if (allDrafts.isEmpty()) {
                _importState.value = DocumentImportUiState(
                    step = ImportStep.SUMMARY,
                    selectedFileNames = fileNames,
                    extractionErrors = errors,
                    summary = ImportSummary(
                        documentsProcessed = uris.size,
                        songsDetected = 0,
                        songsImported = 0,
                        songsRequiringReview = 0,
                        duplicatesSkipped = 0,
                        errors = errors,
                        importedAsPublished = false,
                    )
                )
            } else {
                _importState.value = DocumentImportUiState(
                    step = ImportStep.REVIEW_SONGS,
                    selectedFileNames = fileNames,
                    drafts = allDrafts,
                    extractionErrors = errors,
                )
            }
        }
    }

    fun openDraftEditor(draftId: String?) {
        _importState.value = _importState.value.copy(activeDraftId = draftId)
    }

    fun updateDraft(updatedDraft: ImportedSongDraft) {
        viewModelScope.launch {
            val reParsedSections = ChordLyricParser.parseToSections(updatedDraft.rawLyrics)
            val chordSummary = ChordLyricParser.extractChordSummary(reParsedSections)
            val duplicates = songRepository.findPotentialDuplicates(
                title = updatedDraft.title,
                language = updatedDraft.language,
                lyrics = ChordLyricParser.sectionsToPlainLyrics(reParsedSections),
            )
            val normalized = updatedDraft.copy(
                sections = reParsedSections,
                chordsSummary = chordSummary,
                duplicateMatches = duplicates,
            )
            _importState.value = _importState.value.copy(
                drafts = _importState.value.drafts.map { if (it.draftId == normalized.draftId) normalized else it }
            )
        }
    }

    fun moveSection(draftId: String, sectionIndex: Int, direction: Int) {
        val draft = _importState.value.drafts.firstOrNull { it.draftId == draftId } ?: return
        val targetIndex = sectionIndex + direction
        if (targetIndex !in draft.sections.indices) return
        val mutable = draft.sections.toMutableList()
        val moved = mutable.removeAt(sectionIndex)
        mutable.add(targetIndex, moved)
        val reordered: List<SongSection> = mutable.mapIndexed { idx, s -> s.copy(order = idx) }
        val updatedText = ChordLyricParser.sectionsToEditableText(reordered)
        val updatedDraft = draft.copy(
            sections = reordered,
            rawLyrics = updatedText,
            chordsSummary = ChordLyricParser.extractChordSummary(reordered),
        )
        _importState.value = _importState.value.copy(
            drafts = _importState.value.drafts.map { if (it.draftId == draftId) updatedDraft else it }
        )
    }

    fun updateDraftLanguage(draftId: String, language: SongLanguage) {
        viewModelScope.launch {
            val existing = _importState.value.drafts.firstOrNull { it.draftId == draftId } ?: return@launch
            val duplicates = songRepository.findPotentialDuplicates(
                title = existing.title,
                language = language,
                lyrics = existing.rawLyrics,
            )
            val updated = existing.copy(language = language, duplicateMatches = duplicates)
            _importState.value = _importState.value.copy(
                drafts = _importState.value.drafts.map { if (it.draftId == draftId) updated else it }
            )
        }
    }

    fun setDuplicateResolution(draftId: String, resolution: DuplicateResolution) {
        _importState.value = _importState.value.copy(
            drafts = _importState.value.drafts.map { draft ->
                if (draft.draftId == draftId) {
                    draft.copy(
                        duplicateResolution = resolution,
                        isIncluded = resolution != DuplicateResolution.SKIP,
                    )
                } else {
                    draft
                }
            }
        )
    }

    fun removeDraft(draftId: String) {
        _importState.value = _importState.value.copy(
            drafts = _importState.value.drafts.filterNot { it.draftId == draftId },
            activeDraftId = if (_importState.value.activeDraftId == draftId) null else _importState.value.activeDraftId,
        )
    }

    fun commitImport(publishImmediately: Boolean) {
        val currentState = _importState.value
        if (currentState.isCommitting) return

        viewModelScope.launch {
            _importState.value = currentState.copy(isCommitting = true)

            val session = authRepository.currentSession.value
            val adminId = session.uid ?: "local-preview-admin"
            val adminName = session.displayName ?: "Local Admin Preview"

            val skippedDuplicates = currentState.drafts.count {
                !it.isIncluded || it.duplicateResolution == DuplicateResolution.SKIP
            }
            val toImport = currentState.drafts.filter {
                it.isIncluded && it.duplicateResolution != DuplicateResolution.SKIP
            }

            val songsToSave = toImport.map { draft ->
                val sections = ChordLyricParser.parseToSections(draft.rawLyrics)
                val plainLyrics = ChordLyricParser.sectionsToPlainLyrics(sections).ifBlank { draft.rawLyrics }
                Song(
                    id = UUID.randomUUID().toString(),
                    title = draft.title.trim().ifBlank { "Untitled Song" },
                    alternateTitle = draft.alternateTitle.trim(),
                    language = draft.language,
                    lyrics = plainLyrics,
                    sections = sections,
                    chords = ChordLyricParser.extractChordSummary(sections),
                    originalKey = draft.originalKey.trim(),
                    capo = draft.capo.trim(),
                    category = draft.category.trim(),
                    tags = draft.tags,
                    videoUrl = draft.videoUrl.trim(),
                    audioUrl = draft.audioUrl.trim(),
                    otherSourceUrl = draft.otherSourceUrl.trim(),
                    lyricist = draft.lyricist.trim(),
                    composer = draft.composer.trim(),
                    translator = draft.translator.trim(),
                    copyrightOwner = draft.copyrightOwner.trim(),
                    licenseInfo = draft.licenseInfo.trim(),
                    additionalCredits = draft.additionalCredits.trim(),
                    createdByAdminId = adminId,
                    isPublished = publishImmediately,
                )
            }

            val errors = currentState.extractionErrors.toMutableList()
            var importedCount = 0
            if (songsToSave.isNotEmpty()) {
                songRepository.upsertSongsBatch(songsToSave)
                    .onSuccess { importedCount = it.size }
                    .onFailure { errors.add("Database save failed: ${it.message ?: "unknown error"}") }
            }

            val reviewCount = toImport.count { it.needsManualReview }
            val summary = ImportSummary(
                documentsProcessed = currentState.selectedFileNames.size,
                songsDetected = currentState.drafts.size,
                songsImported = importedCount,
                songsRequiringReview = reviewCount,
                duplicatesSkipped = skippedDuplicates,
                errors = errors,
                importedAsPublished = publishImmediately,
            )

            songRepository.recordImportHistory(
                ImportHistoryRecord(
                    id = UUID.randomUUID().toString(),
                    fileNames = currentState.selectedFileNames,
                    documentsProcessed = summary.documentsProcessed,
                    songsDetected = summary.songsDetected,
                    songsImported = summary.songsImported,
                    songsRequiringReview = summary.songsRequiringReview,
                    duplicatesSkipped = summary.duplicatesSkipped,
                    errorsSummary = errors.joinToString("\n"),
                    adminDisplayName = adminName,
                )
            )

            _importState.value = currentState.copy(
                step = ImportStep.SUMMARY,
                isCommitting = false,
                summary = summary,
            )
        }
    }
}
