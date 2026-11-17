package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SongLanguage(
    val code: String,
    val nativeLabel: String,
    val englishName: String,
) {
    TELUGU(code = "te", nativeLabel = "తెలుగు", englishName = "Telugu"),
    HINDI(code = "hi", nativeLabel = "हिन्दी", englishName = "Hindi"),
    ENGLISH(code = "en", nativeLabel = "English", englishName = "English");

    companion object {
        fun fromCode(code: String?): SongLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: ENGLISH
    }
}

@Serializable
enum class SongSectionType(val defaultLabel: String) {
    PALLAVI("Pallavi"),
    CHARANAM("Charanam"),
    VERSE("Verse"),
    CHORUS("Chorus"),
    PRE_CHORUS("Pre-Chorus"),
    BRIDGE("Bridge"),
    INTRO("Intro"),
    ENDING("Ending"),
    TAG("Tag"),
    STANZA("Stanza"),
    CUSTOM("Section")
}

@Serializable
data class ChordPlacement(
    val chord: String,
    val positionIndex: Int,
)

@Serializable
data class LyricLine(
    val lyrics: String,
    val chords: List<ChordPlacement> = emptyList(),
)

@Serializable
data class SongSection(
    val id: String,
    val type: SongSectionType,
    val label: String,
    val lines: List<LyricLine>,
    val order: Int = 0,
)

@Serializable
data class Song(
    val id: String,
    val title: String,
    val alternateTitle: String = "",
    val language: SongLanguage,
    val lyrics: String,
    val sections: List<SongSection> = emptyList(),
    val chords: String = "",
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByAdminId: String = "",
    val isPublished: Boolean = true,
    val isFavourite: Boolean = false,
    val lastViewedAt: Long? = null,
    val isLocalTestSample: Boolean = false,
) {
    val hasChords: Boolean
        get() = chords.isNotBlank() || sections.any { section -> section.lines.any { it.chords.isNotEmpty() } }

    val hasAnyCredits: Boolean
        get() = lyricist.isNotBlank() ||
            composer.isNotBlank() ||
            translator.isNotBlank() ||
            copyrightOwner.isNotBlank() ||
            licenseInfo.isNotBlank() ||
            additionalCredits.isNotBlank()

    val hasSourceLinks: Boolean
        get() = videoUrl.isNotBlank() || audioUrl.isNotBlank() || otherSourceUrl.isNotBlank()
}

enum class SortOrder(val label: String) {
    ALPHABETICAL("Alphabetical"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_VIEWED("Recently Viewed")
}

enum class DuplicateReason(val description: String) {
    EXACT_TITLE("Matches existing title in the same language"),
    SIMILAR_TITLE("Very similar song title already exists"),
    HIGH_LYRIC_OVERLAP("Lyrics closely match an existing song")
}

data class DuplicateMatch(
    val existingSong: Song,
    val reason: DuplicateReason,
    val similarityScore: Float,
)

@Serializable
data class ImportHistoryRecord(
    val id: String,
    val fileNames: List<String>,
    val documentsProcessed: Int,
    val songsDetected: Int,
    val songsImported: Int,
    val songsRequiringReview: Int,
    val duplicatesSkipped: Int,
    val errorsSummary: String = "",
    val importedAt: Long = System.currentTimeMillis(),
    val adminDisplayName: String = "",
)

enum class UserRole {
    MEMBER,
    ADMIN
}

data class AdminSession(
    val isSignedIn: Boolean = false,
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val role: UserRole = UserRole.MEMBER,
    val isLocalPreviewMode: Boolean = true,
) {
    val isAdmin: Boolean
        get() = isSignedIn && role == UserRole.ADMIN
}

enum class SyncMode {
    LOCAL_OFFLINE_ONLY,
    FIREBASE_SYNCED,
    SYNC_ERROR
}

data class SyncStatus(
    val mode: SyncMode = SyncMode.LOCAL_OFFLINE_ONLY,
    val message: String = "Local Offline Storage (Firebase cloud sync not connected)",
    val lastSyncedAt: Long? = null,
    val cachedSongCount: Int = 0,
)
