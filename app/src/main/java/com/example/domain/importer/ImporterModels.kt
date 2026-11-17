package com.example.domain.importer

import com.example.domain.model.DuplicateMatch
import com.example.domain.model.SongLanguage
import com.example.domain.model.SongSection

enum class ImportedFileType {
    DOCX,
    PDF,
    UNKNOWN
}

data class ExtractedDocumentResult(
    val fileName: String,
    val fileType: ImportedFileType,
    val rawText: String,
    val extractedLinks: List<String> = emptyList(),
    val isScannedPdf: Boolean = false,
    val ocrAttempted: Boolean = false,
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null,
)

enum class DuplicateResolution {
    UNDECIDED,
    SKIP,
    IMPORT_AS_VERSION
}

data class ImportedSongDraft(
    val draftId: String,
    val sourceFileName: String,
    val title: String,
    val alternateTitle: String = "",
    val language: SongLanguage,
    val sections: List<SongSection>,
    val rawLyrics: String,
    val rawExtractedText: String,
    val chordsSummary: String = "",
    val originalKey: String = "",
    val capo: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val videoUrl: String = "",
    val audioUrl: String = "",
    val otherSourceUrl: String = "",
    val lyricist: String = "",
    val composer: String = "",
    val translator: String = "",
    val copyrightOwner: String = "",
    val licenseInfo: String = "",
    val additionalCredits: String = "",
    val warnings: List<String> = emptyList(),
    val duplicateMatches: List<DuplicateMatch> = emptyList(),
    val duplicateResolution: DuplicateResolution = DuplicateResolution.UNDECIDED,
    val isIncluded: Boolean = true,
    val needsManualReview: Boolean = false,
) {
    val hasUnresolvedDuplicate: Boolean
        get() = duplicateMatches.isNotEmpty() && duplicateResolution == DuplicateResolution.UNDECIDED && isIncluded
}

data class ImportSummary(
    val documentsProcessed: Int,
    val songsDetected: Int,
    val songsImported: Int,
    val songsRequiringReview: Int,
    val duplicatesSkipped: Int,
    val errors: List<String> = emptyList(),
    val importedAsPublished: Boolean,
)
