package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isPublished = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observePublishedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY updatedAt DESC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    fun observeSongById(songId: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE isPublished = 1 AND isFavourite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavouriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isPublished = 1 AND lastViewedAt IS NOT NULL ORDER BY lastViewedAt DESC LIMIT :limit")
    fun observeRecentlyViewedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isPublished = 1 ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentlyAddedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT category FROM songs WHERE category != '' AND isPublished = 1 ORDER BY category COLLATE NOCASE ASC")
    fun observePublishedCategories(): Flow<List<String>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongByIdOnce(songId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    @Query("UPDATE songs SET isPublished = :isPublished, updatedAt = :updatedAt WHERE id = :songId")
    suspend fun updatePublishedStatus(songId: String, isPublished: Boolean, updatedAt: Long)

    @Query("UPDATE songs SET isFavourite = :isFavourite WHERE id = :songId")
    suspend fun updateFavourite(songId: String, isFavourite: Boolean)

    @Query("UPDATE songs SET lastViewedAt = :viewedAt WHERE id = :songId")
    suspend fun updateLastViewed(songId: String, viewedAt: Long)

    @Query("DELETE FROM songs WHERE isLocalTestSample = 1")
    suspend fun deleteLocalTestSamples(): Int
}

@Dao
interface ImportHistoryDao {
    @Query("SELECT * FROM import_history ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<ImportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ImportHistoryEntity)
}
