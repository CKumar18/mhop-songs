package com.example.domain.repository

import com.example.domain.model.AdminSession
import com.example.domain.model.DuplicateMatch
import com.example.domain.model.ImportHistoryRecord
import com.example.domain.model.ReadingPreferences
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SongRepository {
    val syncStatus: StateFlow<SyncStatus>

    fun observePublishedSongs(): Flow<List<Song>>
    fun observeAllSongsForAdmin(): Flow<List<Song>>
    fun observeSongById(songId: String): Flow<Song?>
    fun observeFavouriteSongs(): Flow<List<Song>>
    fun observeRecentlyViewedSongs(limit: Int = 10): Flow<List<Song>>
    fun observeRecentlyAddedSongs(limit: Int = 10): Flow<List<Song>>
    fun observeCategories(): Flow<List<String>>
    fun observeImportHistory(): Flow<List<ImportHistoryRecord>>

    suspend fun getSongById(songId: String): Song?
    suspend fun upsertSong(song: Song): Result<Song>
    suspend fun upsertSongsBatch(songs: List<Song>): Result<List<Song>>
    suspend fun deleteSong(songId: String): Result<Unit>
    suspend fun setPublished(songId: String, isPublished: Boolean): Result<Unit>
    suspend fun toggleFavourite(songId: String): Result<Boolean>
    suspend fun markSongViewed(songId: String): Result<Unit>
    suspend fun findPotentialDuplicates(
        title: String,
        language: SongLanguage,
        lyrics: String,
    ): List<DuplicateMatch>

    suspend fun recordImportHistory(record: ImportHistoryRecord): Result<Unit>
    suspend fun seedLocalTestSongs(): Result<Int>
    suspend fun clearLocalTestSongs(): Result<Int>
}

interface PreferencesRepository {
    val readingPreferences: Flow<ReadingPreferences>
    suspend fun updateReadingPreferences(transform: (ReadingPreferences) -> ReadingPreferences)
    suspend fun resetReadingPreferences()
}

interface AuthRepository {
    val currentSession: StateFlow<AdminSession>
    suspend fun signInLocalAdminPreview(displayName: String, email: String): Result<AdminSession>
    suspend fun signOut(): Result<Unit>
}
