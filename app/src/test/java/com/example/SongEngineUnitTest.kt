package com.example

import com.example.domain.chord.ChordLyricParser
import com.example.domain.chord.ChordTransposer
import com.example.domain.importer.DuplicateDetector
import com.example.domain.importer.SongParserEngine
import com.example.domain.model.DuplicateReason
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import com.example.domain.model.SongSectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongEngineUnitTest {

    @Test
    fun chordTransposer_transposesSharpsFlatsAndSlashChords() {
        assertEquals("A", ChordTransposer.transposeChord("G", 2))
        assertEquals("F#m7", ChordTransposer.transposeChord("Em7", 2))
        assertEquals("D/F#", ChordTransposer.transposeChord("C/E", 2))
        assertEquals("Bb", ChordTransposer.transposeChord("C", -2, preferFlats = true))
        assertEquals("G", ChordTransposer.transposeKey("G", 12))
    }

    @Test
    fun chordLyricParser_parsesTwoLineChordsAndTeluguSections_withoutInventingChords() {
        val raw = """
            పల్లవి:
            G       C       G
            స్తోత్రార్హుడా యేసయ్యా
            D               G
            నిత్యము నిన్నే స్తుతింతును

            చరణం 1:
            నీ కృప నాకు చాలునయ్యా
        """.trimIndent()

        val sections = ChordLyricParser.parseToSections(raw)
        assertEquals(2, sections.size)
        assertEquals(SongSectionType.PALLAVI, sections[0].type)
        assertEquals("పల్లవి", sections[0].label)
        assertEquals(2, sections[0].lines.size)
        assertEquals(listOf("G", "C", "G"), sections[0].lines[0].chords.map { it.chord })
        assertEquals(SongSectionType.CHARANAM, sections[1].type)
        assertTrue(sections[1].lines[0].chords.isEmpty())
    }

    @Test
    fun songParserEngine_splitsMultiSongDocumentAndDetectsLanguages() {
        val multiSongDoc = """
            1. Amazing Grace
            Key: G
            Video: https://youtu.be/example123
            Verse 1:
            [G]Amazing grace how [C]sweet the [G]sound

            ---

            2. స్తోత్రార్హుడా యేసయ్యా
            పల్లవి:
            స్తోత్రార్హుడా యేసయ్యా నిత్యము నిన్నే స్తుతింతును

            ---

            3. स्तुति आराधना
            मुखड़ा:
            स्तुति आराधना तेरे ही लिए प्रभु
        """.trimIndent()

        val drafts = SongParserEngine.parseDocumentIntoSongs(
            fileName = "church_hymns.docx",
            rawText = multiSongDoc,
        )

        assertEquals(3, drafts.size)
        assertEquals(SongLanguage.ENGLISH, drafts[0].language)
        assertEquals("G", drafts[0].originalKey)
        assertEquals("https://youtu.be/example123", drafts[0].videoUrl)
        assertEquals(SongLanguage.TELUGU, drafts[1].language)
        assertEquals(SongLanguage.HINDI, drafts[2].language)
    }

    @Test
    fun songParserEngine_neverInventsChordsForPlainLyrics() {
        val plain = """
            Simple Prayer Song
            Chorus:
            Lord we thank You every day
            Guide our steps along Your way
        """.trimIndent()

        val drafts = SongParserEngine.parseDocumentIntoSongs("plain.pdf", plain)
        assertEquals(1, drafts.size)
        assertEquals("", drafts[0].chordsSummary)
        assertFalse(drafts[0].sections.any { s -> s.lines.any { it.chords.isNotEmpty() } })
    }

    @Test
    fun duplicateDetector_flagsExactTitleAndLyricOverlap() {
        val existing = listOf(
            Song(
                id = "1",
                title = "Amazing Grace",
                language = SongLanguage.ENGLISH,
                lyrics = "Amazing grace how sweet the sound that saved a wretch like me",
            )
        )

        val exactMatches = DuplicateDetector.findMatches(
            title = "Amazing Grace!",
            language = SongLanguage.ENGLISH,
            lyrics = "Different line",
            existingSongs = existing,
        )
        assertEquals(1, exactMatches.size)
        assertEquals(DuplicateReason.EXACT_TITLE, exactMatches[0].reason)

        val lyricMatches = DuplicateDetector.findMatches(
            title = "Completely New Title",
            language = SongLanguage.ENGLISH,
            lyrics = "Amazing grace how sweet the sound that saved a wretch like me",
            existingSongs = existing,
        )
        assertEquals(1, lyricMatches.size)
        assertEquals(DuplicateReason.HIGH_LYRIC_OVERLAP, lyricMatches[0].reason)
    }
}
