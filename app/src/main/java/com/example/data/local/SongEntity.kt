package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ImportHistoryRecord
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SongSection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val entityJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val alternateTitle: String,
    val languageCode: String,
    val lyrics: String,
    val sectionsJson: String,
    val chords: String,
    val originalKey: String,
    val capo: String,
    val category: String,
    val tagsJson: String,
    val videoUrl: String,
    val audioUrl: String,
    val otherSourceUrl: String,
    val lyricist: String,
    val composer: String,
    val translator: String,
    val copyrightOwner: String,
    val licenseInfo: String,
    val additionalCredits: String,
    val createdAt: Long,
    val updatedAt: Long,
    val createdByAdminId: String,
    val isPublished: Boolean,
    val isFavourite: Boolean,
    val lastViewedAt: Long?,
    val isLocalTestSample: Boolean,
) {
    fun toDomain(): Song {
        val decodedSections = runCatching {
            entityJson.decodeFromString<List<SongSection>>(sectionsJson)
        }.getOrDefault(emptyList())
        val decodedTags = runCatching {
            entityJson.decodeFromString<List<String>>(tagsJson)
        }.getOrDefault(emptyList())

        return Song(
            id = id,
            title = title,
            alternateTitle = alternateTitle,
            language = SongLanguage.fromCode(languageCode),
            lyrics = lyrics,
            sections = decodedSections,
            chords = chords,
            originalKey = originalKey,
            capo = capo,
            category = category,
            tags = decodedTags,
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            otherSourceUrl = otherSourceUrl,
            lyricist = lyricist,
            composer = composer,
            translator = translator,
            copyrightOwner = copyrightOwner,
            licenseInfo = licenseInfo,
            additionalCredits = additionalCredits,
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdByAdminId = createdByAdminId,
            isPublished = isPublished,
            isFavourite = isFavourite,
            lastViewedAt = lastViewedAt,
            isLocalTestSample = isLocalTestSample,
        )
    }

    companion object {
        fun fromDomain(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                alternateTitle = song.alternateTitle,
                languageCode = song.language.code,
                lyrics = song.lyrics,
                sectionsJson = entityJson.encodeToString(song.sections),
                chords = song.chords,
                originalKey = song.originalKey,
                capo = song.capo,
                category = song.category,
                tagsJson = entityJson.encodeToString(song.tags),
                videoUrl = song.videoUrl,
                audioUrl = song.audioUrl,
                otherSourceUrl = song.otherSourceUrl,
                lyricist = song.lyricist,
                composer = song.composer,
                translator = song.translator,
                copyrightOwner = song.copyrightOwner,
                licenseInfo = song.licenseInfo,
                additionalCredits = song.additionalCredits,
                createdAt = song.createdAt,
                updatedAt = song.updatedAt,
                createdByAdminId = song.createdByAdminId,
                isPublished = song.isPublished,
                isFavourite = song.isFavourite,
                lastViewedAt = song.lastViewedAt,
                isLocalTestSample = song.isLocalTestSample,
            )
        }
    }
}

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey val id: String,
    val fileNamesJson: String,
    val documentsProcessed: Int,
    val songsDetected: Int,
    val songsImported: Int,
    val songsRequiringReview: Int,
    val duplicatesSkipped: Int,
    val errorsSummary: String,
    val importedAt: Long,
    val adminDisplayName: String,
) {
    fun toDomain(): ImportHistoryRecord {
        val decodedFiles = runCatching {
            entityJson.decodeFromString<List<String>>(fileNamesJson)
        }.getOrDefault(emptyList())

        return ImportHistoryRecord(
            id = id,
            fileNames = decodedFiles,
            documentsProcessed = documentsProcessed,
            songsDetected = songsDetected,
            songsImported = songsImported,
            songsRequiringReview = songsRequiringReview,
            duplicatesSkipped = duplicatesSkipped,
            errorsSummary = errorsSummary,
            importedAt = importedAt,
            adminDisplayName = adminDisplayName,
        )
    }

    companion object {
        fun fromDomain(record: ImportHistoryRecord): ImportHistoryEntity {
            return ImportHistoryEntity(
                id = record.id,
                fileNamesJson = entityJson.encodeToString(record.fileNames),
                documentsProcessed = record.documentsProcessed,
                songsDetected = record.songsDetected,
                songsImported = record.songsImported,
                songsRequiringReview = record.songsRequiringReview,
                duplicatesSkipped = record.duplicatesSkipped,
                errorsSummary = record.errorsSummary,
                importedAt = record.importedAt,
                adminDisplayName = record.adminDisplayName,
            )
        }
    }
}
