package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.chord.ChordLyricParser
import com.example.domain.chord.ChordTransposer
import com.example.domain.model.ReadingPreferences
import com.example.domain.model.Song
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.SongRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SongReaderViewModel(
    private val songRepository: SongRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _songId = MutableStateFlow<String?>(null)

    val song: StateFlow<Song?> = _songId
        .filterNotNull()
        .flatMapLatest { id -> songRepository.observeSongById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val readingPreferences: StateFlow<ReadingPreferences> = preferencesRepository.readingPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingPreferences())

    private val _showChords = MutableStateFlow(true)
    val showChords: StateFlow<Boolean> = _showChords.asStateFlow()

    private val _semitoneOffset = MutableStateFlow(0)
    val semitoneOffset: StateFlow<Int> = _semitoneOffset.asStateFlow()

    private val _isDistractionFree = MutableStateFlow(false)
    val isDistractionFree: StateFlow<Boolean> = _isDistractionFree.asStateFlow()

    private var lastLoadedSongId: String? = null

    fun loadSong(songId: String) {
        if (lastLoadedSongId == songId) return
        lastLoadedSongId = songId
        _songId.value = songId
        _semitoneOffset.value = 0
        _isDistractionFree.value = false
        viewModelScope.launch {
            _showChords.value = preferencesRepository.readingPreferences.first().showChordsByDefault
            songRepository.markSongViewed(songId)
        }
    }

    fun toggleFavourite() {
        val current = song.value ?: return
        viewModelScope.launch {
            songRepository.toggleFavourite(current.id)
        }
    }

    fun toggleChords() {
        _showChords.value = !_showChords.value
    }

    fun transposeBy(delta: Int) {
        val next = (_semitoneOffset.value + delta).coerceIn(-11, 11)
        _semitoneOffset.value = next
    }

    fun resetTransposition() {
        _semitoneOffset.value = 0
    }

    fun toggleDistractionFree() {
        _isDistractionFree.value = !_isDistractionFree.value
    }

    fun setDistractionFree(enabled: Boolean) {
        _isDistractionFree.value = enabled
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

    fun buildPlainLyricsText(currentSong: Song): String {
        val body = if (currentSong.sections.isNotEmpty()) {
            ChordLyricParser.sectionsToPlainLyrics(currentSong.sections)
        } else {
            currentSong.lyrics
        }
        return buildString {
            appendLine(currentSong.title)
            if (currentSong.alternateTitle.isNotBlank()) {
                appendLine(currentSong.alternateTitle)
            }
            appendLine()
            append(body)
        }.trim()
    }

    fun buildShareText(currentSong: Song): String {
        val offset = _semitoneOffset.value
        val displayKey = if (currentSong.originalKey.isNotBlank()) {
            ChordTransposer.transposeKey(currentSong.originalKey, offset)
        } else {
            ""
        }
        return buildString {
            appendLine(currentSong.title)
            if (displayKey.isNotBlank()) {
                appendLine("Key: $displayKey${if (currentSong.capo.isNotBlank()) " | Capo: ${currentSong.capo}" else ""}")
            }
            appendLine()
            if (currentSong.sections.isNotEmpty()) {
                currentSong.sections.forEach { section ->
                    if (!section.label.equals("Lyrics", ignoreCase = true) || currentSong.sections.size > 1) {
                        appendLine(section.label)
                    }
                    section.lines.forEach { line ->
                        if (_showChords.value && line.chords.isNotEmpty()) {
                            val chordRow = ChordLyricParser.formatChordOverlayRow(line, offset)
                            if (chordRow.isNotBlank()) appendLine(chordRow)
                        }
                        if (line.lyrics.isNotBlank()) appendLine(line.lyrics)
                    }
                    appendLine()
                }
            } else {
                appendLine(currentSong.lyrics)
            }
            if (currentSong.videoUrl.isNotBlank()) {
                appendLine("Video: ${currentSong.videoUrl}")
            }
            append("Shared via MHOP Songs")
        }.trim()
    }
}
