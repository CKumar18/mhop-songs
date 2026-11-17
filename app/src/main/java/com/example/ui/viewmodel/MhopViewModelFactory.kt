package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.AppContainer

class MhopViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(
                songRepository = container.songRepository,
                preferencesRepository = container.preferencesRepository,
                authRepository = container.authRepository,
            ) as T

            modelClass.isAssignableFrom(SongReaderViewModel::class.java) -> SongReaderViewModel(
                songRepository = container.songRepository,
                preferencesRepository = container.preferencesRepository,
            ) as T

            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(
                songRepository = container.songRepository,
                authRepository = container.authRepository,
                documentExtractor = container.documentExtractor,
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
