package com.example.data.repository

import com.example.data.local.ImportHistoryDao
import com.example.data.local.ImportHistoryEntity
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import com.example.domain.chord.ChordLyricParser
import com.example.domain.importer.DuplicateDetector
import com.example.domain.model.DuplicateMatch
import com.example.domain.model.ImportHistoryRecord
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SyncMode
import com.example.domain.model.SyncStatus
import com.example.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class LocalSongRepository(
    private val songDao: SongDao,
    private val importHistoryDao: ImportHistoryDao,
) : SongRepository {

    private val _syncStatus = MutableStateFlow(
        SyncStatus(
            mode = SyncMode.LOCAL_OFFLINE_ONLY,
            message = "Offline local database active. Firebase Cloud Sync is not connected in Phase 1.",
            lastSyncedAt = null,
            cachedSongCount = 0
        )
    )
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override fun observePublishedSongs(): Flow<List<Song>> =
        songDao.observePublishedSongs().map { entities ->
            val mapped = entities.map { it.toDomain() }
            _syncStatus.value = _syncStatus.value.copy(cachedSongCount = mapped.size)
            mapped
        }

    override fun observeAllSongsForAdmin(): Flow<List<Song>> =
        songDao.observeAllSongs().map { entities -> entities.map { it.toDomain() } }

    override fun observeSongById(songId: String): Flow<Song?> =
        songDao.observeSongById(songId).map { it?.toDomain() }

    override fun observeFavouriteSongs(): Flow<List<Song>> =
        songDao.observeFavouriteSongs().map { entities -> entities.map { it.toDomain() } }

    override fun observeRecentlyViewedSongs(limit: Int): Flow<List<Song>> =
        songDao.observeRecentlyViewedSongs(limit).map { entities -> entities.map { it.toDomain() } }

    override fun observeRecentlyAddedSongs(limit: Int): Flow<List<Song>> =
        songDao.observeRecentlyAddedSongs(limit).map { entities -> entities.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<String>> =
        songDao.observePublishedCategories()

    override fun observeImportHistory(): Flow<List<ImportHistoryRecord>> =
        importHistoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSongById(songId: String): Song? =
        songDao.getSongByIdOnce(songId)?.toDomain()

    override suspend fun upsertSong(song: Song): Result<Song> = runCatching {
        val updated = song.copy(updatedAt = System.currentTimeMillis())
        songDao.upsertSong(SongEntity.fromDomain(updated))
        updated
    }

    override suspend fun upsertSongsBatch(songs: List<Song>): Result<List<Song>> = runCatching {
        val now = System.currentTimeMillis()
        val updated = songs.mapIndexed { idx, song ->
            song.copy(
                createdAt = if (song.createdAt <= 0L) now + idx else song.createdAt,
                updatedAt = now + idx
            )
        }
        songDao.upsertSongs(updated.map { SongEntity.fromDomain(it) })
        updated
    }

    override suspend fun deleteSong(songId: String): Result<Unit> = runCatching {
        songDao.deleteSongById(songId)
    }

    override suspend fun setPublished(songId: String, isPublished: Boolean): Result<Unit> = runCatching {
        songDao.updatePublishedStatus(songId, isPublished, System.currentTimeMillis())
    }

    override suspend fun toggleFavourite(songId: String): Result<Boolean> = runCatching {
        val existing = songDao.getSongByIdOnce(songId)
            ?: throw IllegalStateException("Song not found")
        val nextValue = !existing.isFavourite
        songDao.updateFavourite(songId, nextValue)
        nextValue
    }

    override suspend fun markSongViewed(songId: String): Result<Unit> = runCatching {
        songDao.updateLastViewed(songId, System.currentTimeMillis())
    }

    override suspend fun findPotentialDuplicates(
        title: String,
        language: SongLanguage,
        lyrics: String,
    ): List<DuplicateMatch> {
        val existingSongs = songDao.getAllSongsOnce().map { it.toDomain() }
        return DuplicateDetector.findMatches(title, language, lyrics, existingSongs)
    }

    override suspend fun recordImportHistory(record: ImportHistoryRecord): Result<Unit> = runCatching {
        importHistoryDao.insert(ImportHistoryEntity.fromDomain(record))
    }

    override suspend fun seedLocalTestSongs(): Result<Int> = runCatching {
        val samples = createClearlyLabeledLocalTestSongs()
        songDao.upsertSongs(samples.map { SongEntity.fromDomain(it) })
        samples.size
    }

    override suspend fun clearLocalTestSongs(): Result<Int> = runCatching {
        songDao.deleteLocalTestSamples()
    }

    private fun createClearlyLabeledLocalTestSongs(): List<Song> {
        val now = System.currentTimeMillis()

        // 1. Public-domain hymn with chords (English)
        val englishRaw = """
            Verse 1:
            [G]Amazing [G7]grace how [C]sweet the [G]sound
            That saved a [Em]wretch like [D]me
            [G]I once was [G7]lost, but [C]now am [G]found
            Was blind, but [D]now I [G]see

            Verse 2:
            [G]'Twas grace that [G7]taught my [C]heart to [G]fear
            And grace my [Em]fears re[D]lieved
            [G]How precious [G7]did that [C]grace ap[G]pear
            The hour I [D]first be[G]lieved
        """.trimIndent()
        val englishSections = ChordLyricParser.parseToSections(englishRaw)

        // 2. Public-domain doxology / format test (Telugu script)
        val teluguRaw = """
            పల్లవి:
            [C]స్తోత్రార్హుడా యే[F]సయ్యా - ని[G]త్యము నిన్నే స్తు[C]తింతును
            కృపగల దేవా కరుణించుమా - నీ సన్నిధిలో నను నిలుపుమా

            చరణం 1:
            [C]నీ కృప నాకు [Am]చాలునయ్యా - నీ [F]ప్రేమ ఎంతో [G]మధురమయ్యా
            ఆకాశమంత ఎత్తయినది - నీ విశ్వాస్యత నిరంతరము
        """.trimIndent()
        val teluguSections = ChordLyricParser.parseToSections(teluguRaw)

        // 3. Public-domain praise chorus / format test (Hindi script)
        val hindiRaw = """
            मुखड़ा:
            [D]स्तुति आराधना [G]तेरे ही [A]लिए प्रभु
            [D]महिमा और सम्मान [A]सदा तेरे [D]लिए

            अंतरा 1:
            [D]तू ही मेरा शरणस्थान, [G]तू ही मेरी चट्टान
            [Em]हर दिन तेरे नाम की [A]जय मैं गाऊँगा
        """.trimIndent()
        val hindiSections = ChordLyricParser.parseToSections(hindiRaw)

        return listOf(
            Song(
                id = "local-test-en-amazing-grace",
                title = "Amazing Grace [Local Test Data]",
                alternateTitle = "Public Domain Hymn Test",
                language = SongLanguage.ENGLISH,
                lyrics = ChordLyricParser.sectionsToPlainLyrics(englishSections),
                sections = englishSections,
                chords = ChordLyricParser.extractChordSummary(englishSections),
                originalKey = "G",
                capo = "0",
                category = "Hymn",
                tags = listOf("Public Domain", "Grace", "Local Test"),
                lyricist = "John Newton (1779)",
                composer = "Traditional (New Britain)",
                copyrightOwner = "Public Domain",
                licenseInfo = "Local test sample for reader and chord verification",
                createdAt = now - 3000,
                updatedAt = now - 3000,
                createdByAdminId = "local-preview-admin",
                isPublished = true,
                isLocalTestSample = true,
            ),
            Song(
                id = "local-test-te-stotrarhuda",
                title = "స్తోత్రార్హుడా యేసయ్యా [Local Test Data]",
                alternateTitle = "Stotrarhuda Yesayya (Test Sample)",
                language = SongLanguage.TELUGU,
                lyrics = ChordLyricParser.sectionsToPlainLyrics(teluguSections),
                sections = teluguSections,
                chords = ChordLyricParser.extractChordSummary(teluguSections),
                originalKey = "C",
                category = "Worship",
                tags = listOf("Telugu", "Local Test"),
                licenseInfo = "Local test sample for Telugu script rendering verification",
                createdAt = now - 2000,
                updatedAt = now - 2000,
                createdByAdminId = "local-preview-admin",
                isPublished = true,
                isLocalTestSample = true,
            ),
            Song(
                id = "local-test-hi-stuti-aradhana",
                title = "स्तुति आराधना तेरे ही लिए [Local Test Data]",
                alternateTitle = "Stuti Aradhana (Test Sample)",
                language = SongLanguage.HINDI,
                lyrics = ChordLyricParser.sectionsToPlainLyrics(hindiSections),
                sections = hindiSections,
                chords = ChordLyricParser.extractChordSummary(hindiSections),
                originalKey = "D",
                category = "Praise",
                tags = listOf("Hindi", "Local Test"),
                licenseInfo = "Local test sample for Devanagari script rendering verification",
                createdAt = now - 1000,
                updatedAt = now - 1000,
                createdByAdminId = "local-preview-admin",
                isPublished = true,
                isLocalTestSample = true,
            )
        )
    }
}
