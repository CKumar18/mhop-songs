package com.example.domain.chord

import com.example.domain.model.ChordPlacement
import com.example.domain.model.LyricLine
import com.example.domain.model.SongSection
import com.example.domain.model.SongSectionType
import java.util.UUID

object ChordLyricParser {
    private val INLINE_CHORD_REGEX = Regex("""\[([A-G][#b]?[^\]]*)\]""")

    private val SECTION_HEADER_REGEX = Regex(
        pattern = """^\s*(?:\[|\(|【)?\s*(పల్లవి|చరణం\s*\d*|చరణము\s*\d*|కోరస్|అంతర|Pallavi|Charanam\s*\d*|Verse\s*\d*|V\s*\d+|Chorus\s*\d*|Pre-?Chorus|Bridge\s*\d*|Intro|Ending|Outro|Tag|Refrain|Stanza\s*\d*|कोरस|पद\s*\d*|मुखड़ा|अंतरा\s*\d*|C\s*\d+|P\s*:?)\s*(?:\]|\)|】)?\s*[:\-–.]?\s*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val INLINE_SECTION_PREFIX_REGEX = Regex(
        pattern = """^\s*(పల్లవి|చరణం\s*\d*|చరణము\s*\d*|Pallavi|Charanam\s*\d*|Verse\s*\d*|Chorus\s*\d*|Pre-?Chorus|Bridge|Intro|Ending|Outro|Tag|Refrain|Stanza\s*\d*|कोरस|मुखड़ा|अंतरा\s*\d*)\s*[:\-–]\s+(.+)$""",
        option = RegexOption.IGNORE_CASE
    )

    fun parseToSections(rawText: String): List<SongSection> {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        val rawLines = normalized.split("\n")
        val sections = mutableListOf<SongSection>()

        var currentType = SongSectionType.VERSE
        var currentLabel = "Lyrics"
        var hasExplicitHeader = false
        val currentLines = mutableListOf<LyricLine>()

        fun flushSection() {
            while (currentLines.isNotEmpty() && currentLines.last().lyrics.isBlank() && currentLines.last().chords.isEmpty()) {
                currentLines.removeAt(currentLines.lastIndex)
            }
            if (currentLines.isNotEmpty()) {
                val sectionLabel = if (hasExplicitHeader) {
                    currentLabel
                } else if (sections.isEmpty()) {
                    "Lyrics"
                } else {
                    "Verse ${sections.size + 1}"
                }
                sections.add(
                    SongSection(
                        id = UUID.randomUUID().toString(),
                        type = currentType,
                        label = sectionLabel,
                        lines = currentLines.toList(),
                        order = sections.size,
                    )
                )
                currentLines.clear()
            }
        }

        var i = 0
        while (i < rawLines.size) {
            val line = rawLines[i]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                if (currentLines.isNotEmpty() && !hasExplicitHeader) {
                    flushSection()
                } else if (currentLines.isNotEmpty()) {
                    // Preserve paragraph break inside explicit section if followed by more lines
                    if (currentLines.last().lyrics.isNotBlank()) {
                        currentLines.add(LyricLine(""))
                    }
                }
                i++
                continue
            }

            // Check standalone section header
            val headerMatch = SECTION_HEADER_REGEX.find(trimmed)
            if (headerMatch != null) {
                flushSection()
                val rawHeader = headerMatch.groupValues[1].trim().trimEnd(':')
                currentType = classifySectionType(rawHeader)
                currentLabel = formatSectionLabel(rawHeader, currentType)
                hasExplicitHeader = true
                i++
                continue
            }

            // Check inline section prefix like "Chorus: Amazing grace..."
            val prefixMatch = INLINE_SECTION_PREFIX_REGEX.find(trimmed)
            if (prefixMatch != null) {
                flushSection()
                val rawHeader = prefixMatch.groupValues[1].trim()
                val rest = prefixMatch.groupValues[2].trim()
                currentType = classifySectionType(rawHeader)
                currentLabel = formatSectionLabel(rawHeader, currentType)
                hasExplicitHeader = true
                currentLines.add(parseInlineChords(rest))
                i++
                continue
            }

            // Check two-line chord-over-lyric notation
            if (ChordTransposer.isChordOnlyLine(line)) {
                val nextLine = rawLines.getOrNull(i + 1)
                if (nextLine != null && nextLine.trim().isNotEmpty() &&
                    !ChordTransposer.isChordOnlyLine(nextLine) &&
                    !SECTION_HEADER_REGEX.matches(nextLine.trim())
                ) {
                    currentLines.add(mergeChordLineOverLyrics(line, nextLine))
                    i += 2
                    continue
                } else {
                    // Chord-only line (e.g., Intro chords)
                    val chords = extractChordTokensWithPositions(line)
                    currentLines.add(LyricLine(lyrics = "", chords = chords))
                    i++
                    continue
                }
            }

            currentLines.add(parseInlineChords(line.trimEnd()))
            i++
        }

        flushSection()
        return sections
    }

    fun parseInlineChords(line: String): LyricLine {
        if (!line.contains("[")) {
            return LyricLine(lyrics = line)
        }
        val placements = mutableListOf<ChordPlacement>()
        val cleanBuilder = StringBuilder()
        var lastEnd = 0
        for (match in INLINE_CHORD_REGEX.findAll(line)) {
            val chordToken = match.groupValues[1].trim()
            if (ChordTransposer.isValidChordToken(chordToken)) {
                cleanBuilder.append(line.substring(lastEnd, match.range.first))
                placements.add(ChordPlacement(chord = chordToken, positionIndex = cleanBuilder.length))
                lastEnd = match.range.last + 1
            }
        }
        if (placements.isEmpty()) {
            return LyricLine(lyrics = line)
        }
        cleanBuilder.append(line.substring(lastEnd))
        return LyricLine(lyrics = cleanBuilder.toString(), chords = placements)
    }

    private fun mergeChordLineOverLyrics(chordLine: String, lyricLine: String): LyricLine {
        val placements = extractChordTokensWithPositions(chordLine)
        return LyricLine(lyrics = lyricLine.trimEnd(), chords = placements)
    }

    private fun extractChordTokensWithPositions(chordLine: String): List<ChordPlacement> {
        val placements = mutableListOf<ChordPlacement>()
        var idx = 0
        while (idx < chordLine.length) {
            if (!chordLine[idx].isWhitespace()) {
                val start = idx
                while (idx < chordLine.length && !chordLine[idx].isWhitespace()) {
                    idx++
                }
                val token = chordLine.substring(start, idx).trim('[', ']', '(', ')')
                if (ChordTransposer.isValidChordToken(token)) {
                    placements.add(ChordPlacement(chord = token, positionIndex = start))
                }
            } else {
                idx++
            }
        }
        return placements
    }

    fun formatLineWithInlineChords(line: LyricLine): String {
        if (line.chords.isEmpty()) return line.lyrics
        if (line.lyrics.isEmpty()) {
            return line.chords.joinToString(" ") { "[${it.chord}]" }
        }
        val sorted = line.chords.sortedBy { it.positionIndex }
        val sb = StringBuilder()
        var cursor = 0
        for (placement in sorted) {
            val pos = placement.positionIndex.coerceIn(cursor, line.lyrics.length)
            sb.append(line.lyrics.substring(cursor, pos))
            sb.append("[${placement.chord}]")
            cursor = pos
        }
        if (cursor < line.lyrics.length) {
            sb.append(line.lyrics.substring(cursor))
        }
        return sb.toString()
    }

    fun formatChordOverlayRow(line: LyricLine, semitones: Int = 0): String {
        if (line.chords.isEmpty()) return ""
        val sb = StringBuilder()
        val sorted = line.chords.sortedBy { it.positionIndex }
        for (placement in sorted) {
            val transposed = ChordTransposer.transposeChord(placement.chord, semitones)
            val targetPos = placement.positionIndex.coerceAtLeast(0)
            while (sb.length < targetPos) {
                sb.append(' ')
            }
            if (sb.isNotEmpty() && sb.length == targetPos && sb.last() != ' ') {
                sb.append(' ')
            }
            sb.append(transposed)
        }
        return sb.toString()
    }

    fun sectionsToEditableText(sections: List<SongSection>): String {
        if (sections.isEmpty()) return ""
        return sections.joinToString("\n\n") { section ->
            val header = if (section.label.equals("Lyrics", ignoreCase = true) && sections.size == 1) {
                ""
            } else {
                "${section.label}:\n"
            }
            val body = section.lines.joinToString("\n") { formatLineWithInlineChords(it) }
            header + body
        }.trim()
    }

    fun sectionsToPlainLyrics(sections: List<SongSection>): String {
        return sections.joinToString("\n\n") { section ->
            val header = if (section.label.equals("Lyrics", ignoreCase = true) && sections.size == 1) {
                ""
            } else {
                "${section.label}\n"
            }
            val body = section.lines.joinToString("\n") { it.lyrics }
            header + body
        }.trim()
    }

    fun extractChordSummary(sections: List<SongSection>): String {
        val uniqueChords = sections
            .flatMap { it.lines }
            .flatMap { it.chords }
            .map { it.chord }
            .distinct()
        return uniqueChords.joinToString(", ")
    }

    fun classifySectionType(header: String): SongSectionType {
        val lower = header.lowercase().trim()
        return when {
            lower.contains("పల్లవి") || lower.contains("pallavi") || lower == "p" || lower.contains("मुखड़ा") ->
                SongSectionType.PALLAVI
            lower.contains("చరణ") || lower.contains("charanam") || lower.startsWith("c") && lower.length <= 3 && !lower.contains("ch") ->
                SongSectionType.CHARANAM
            lower.contains("pre-chorus") || lower.contains("prechorus") ->
                SongSectionType.PRE_CHORUS
            lower.contains("chorus") || lower.contains("refrain") || lower.contains("కోరస్") || lower.contains("कोरस") ->
                SongSectionType.CHORUS
            lower.contains("bridge") ->
                SongSectionType.BRIDGE
            lower.contains("intro") ->
                SongSectionType.INTRO
            lower.contains("ending") || lower.contains("outro") ->
                SongSectionType.ENDING
            lower.contains("tag") ->
                SongSectionType.TAG
            lower.contains("stanza") ->
                SongSectionType.STANZA
            lower.contains("verse") || lower.startsWith("v") || lower.contains("అంతర") || lower.contains("अंतरा") || lower.contains("पद") ->
                SongSectionType.VERSE
            else -> SongSectionType.CUSTOM
        }
    }

    private fun formatSectionLabel(rawHeader: String, type: SongSectionType): String {
        val trimmed = rawHeader.trim()
        return when {
            trimmed.equals("P", ignoreCase = true) -> "Pallavi"
            trimmed.matches(Regex("""^C\s*(\d+)$""", RegexOption.IGNORE_CASE)) -> {
                val num = Regex("""\d+""").find(trimmed)?.value ?: ""
                "Charanam $num".trim()
            }
            trimmed.matches(Regex("""^V\s*(\d+)$""", RegexOption.IGNORE_CASE)) -> {
                val num = Regex("""\d+""").find(trimmed)?.value ?: ""
                "Verse $num".trim()
            }
            trimmed.any { it in '\u0C00'..'\u0C7F' || it in '\u0900'..'\u097F' } -> trimmed
            else -> trimmed.replaceFirstChar { it.uppercaseChar() }.ifBlank { type.defaultLabel }
        }
    }
}
