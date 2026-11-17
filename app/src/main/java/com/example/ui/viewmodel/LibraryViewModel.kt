package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AdminSession
import com.example.domain.model.ReadingPreferences
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SortOrder
import com.example.domain.model.SyncStatus
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchFilterState(
    val query: String = "",
    val selectedLanguage: SongLanguage? = null,
    val selectedCategory: String? = null,
    val sortOrder: SortOrder = SortOrder.ALPHABETICAL,
)

data class LanguageCounts(
    val total: Int = 0,
    val telugu: Int = 0,
    val hindi: Int = 0,
    val english: Int = 0,
)

class LibraryViewModel(
    private val songRepository: SongRepository,
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _searchFilter = MutableStateFlow(SearchFilterState())
    val searchFilter: StateFlow<SearchFilterState> = _searchFilter

    val publishedSongs: StateFlow<List<Song>> = songRepository.observePublishedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favouriteSongs: StateFlow<List<Song>> = songRepository.observeFavouriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewedSongs: StateFlow<List<Song>> = songRepository.observeRecentlyViewedSongs(limit = 8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedSongs: StateFlow<List<Song>> = songRepository.observeRecentlyAddedSongs(limit = 8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = songRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingPreferences: StateFlow<ReadingPreferences> = preferencesRepository.readingPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingPreferences())

    val adminSession: StateFlow<AdminSession> = authRepository.currentSession

    val syncStatus: StateFlow<SyncStatus> = songRepository.syncStatus

    val languageCounts: StateFlow<LanguageCounts> = publishedSongs
        .combine(MutableStateFlow(Unit)) { songs, _ ->
            LanguageCounts(
                total = songs.size,
                telugu = songs.count { it.language == SongLanguage.TELUGU },
                hindi = songs.count { it.language == SongLanguage.HINDI },
                english = songs.count { it.language == SongLanguage.ENGLISH },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageCounts())

    val filteredSongs: StateFlow<List<Song>> = combine(publishedSongs, _searchFilter) { songs, filter ->
        val queryTrimmed = filter.query.trim()
        val matched = songs.filter { song ->
            val langMatches = filter.selectedLanguage == null || song.language == filter.selectedLanguage
            val catMatches = filter.selectedCategory == null ||
                song.category.equals(filter.selectedCategory, ignoreCase = true)
            val queryMatches = if (queryTrimmed.isEmpty()) {
                true
            } else {
                song.title.contains(queryTrimmed, ignoreCase = true) ||
                    song.alternateTitle.contains(queryTrimmed, ignoreCase = true) ||
                    song.lyrics.contains(queryTrimmed, ignoreCase = true) ||
                    song.category.contains(queryTrimmed, ignoreCase = true) ||
                    song.tags.any { it.contains(queryTrimmed, ignoreCase = true) }
            }
            langMatches && catMatches && queryMatches
        }

        when (filter.sortOrder) {
            SortOrder.ALPHABETICAL -> matched.sortedBy { it.title.lowercase() }
            SortOrder.RECENTLY_ADDED -> matched.sortedByDescending { it.createdAt }
            SortOrder.RECENTLY_VIEWED -> matched.sortedByDescending { it.lastViewedAt ?: 0L }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchFilter.value = _searchFilter.value.copy(query = query)
    }

    fun selectLanguageFilter(language: SongLanguage?) {
        _searchFilter.value = _searchFilter.value.copy(selectedLanguage = language)
    }

    fun selectCategoryFilter(category: String?) {
        _searchFilter.value = _searchFilter.value.copy(selectedCategory = category)
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _searchFilter.value = _searchFilter.value.copy(sortOrder = sortOrder)
    }

    fun clearFilters() {
        _searchFilter.value = SearchFilterState()
    }

    fun toggleFavourite(songId: String) {
        viewModelScope.launch {
            songRepository.toggleFavourite(songId)
        }
    }

    fun updateReadingPreferences(transform: (ReadingPreferences) -> ReadingPreferences) {
        viewModelScope.launch {
            preferencesRepository.updateReadingPreferences(transform)
        }
    }

    fun resetReadingPreferences() {
        viewModelScope.launch {
            preferencesRepository.resetReadingPreferences()
        }
    }

    fun signInLocalAdminPreview(displayName: String, email: String) {
        viewModelScope.launch {
            authRepository.signInLocalAdminPreview(displayName, email)
        }
    }

    fun signOutAdmin() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
