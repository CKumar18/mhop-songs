package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.MhopDatabase
import com.example.data.local.ReadingPreferencesDataStore
import com.example.data.repository.LocalAuthRepository
import com.example.data.repository.LocalSongRepository
import com.example.domain.importer.DocumentExtractor
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.SongRepository

class AppContainer(context: Context) {
    private val database: MhopDatabase = MhopDatabase.getInstance(context)

    val songRepository: SongRepository = LocalSongRepository(
        songDao = database.songDao(),
        importHistoryDao = database.importHistoryDao(),
    )

    val preferencesRepository: PreferencesRepository = ReadingPreferencesDataStore(context)

    val authRepository: AuthRepository = LocalAuthRepository(context)

    val documentExtractor: DocumentExtractor = DocumentExtractor(context)
}

class MhopSongsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
