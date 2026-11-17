package com.example.domain.importer

import com.example.domain.chord.ChordLyricParser
import com.example.domain.model.SongLanguage
import java.util.UUID

object SongParserEngine {

    private val EXPLICIT_SEPARATOR_REGEX = Regex("""^\s*(?:[-=_*]{3,})\s*$""")

    private val NUMBERED_SONG_HEADER_REGEX = Regex(
        pattern = """^\s*(?:Song|Hymn|No\.?|పాట|గీతం|गीत|भजन)\s*#?\s*\d+\s*[:.\-–]?\s*(.*)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val NUMBERED_PREFIX_TITLE_REGEX = Regex("""^\s*\d{1,3}[.)]\s+(.+)$""")

    private val SECTION_LINE_CHECK_REGEX = Regex(
        pattern = """^\s*(?:\[|\(|【)?\s*(?:పల్లవి|చరణం\s*\d*|చరణము\s*\d*|కోరస్|అంతర|Pallavi|Charanam\s*\d*|Verse\s*\d*|V\s*\d+|Chorus\s*\d*|Pre-?Chorus|Bridge\s*\d*|Intro|Ending|Outro|Tag|Refrain|Stanza\s*\d*|कोरस|पद\s*\d*|मुखड़ा|अंतरा\s*\d*|C\s*\d+|P)\s*(?:\]|\)|】)?\s*[:\-–.]?.*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val URL_REGEX = Regex("""https?://[^\s<>)\]]+""", RegexOption.IGNORE_CASE)

    private val METADATA_LINE_REGEX = Regex(
        pattern = """^\s*(Title|Alternate\s*Title|Alt\s*Title|Language|Key|Original\s*Key|Scale|Capo|Category|Tags|Video|YouTube|Audio|Source|Link|Lyricist|Lyrics\s*By|Writer|Author|Composer|Music|Tune|Translator|Translated\s*By|Copyright|License|Credits)\s*[:\-–]\s*(.+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private const val HEADING_MARKER = "[[HEADING]]"

    fun detectLanguage(text: String): SongLanguage {
        var teluguCount = 0
        var devanagariCount = 0
        var latinCount = 0

        for (ch in text) {
            when (ch) {
                in '\u0C00'..'\u0C7F' -> teluguCount++
                in '\u0900'..'\u097F' -> devanagariCount++
                in 'A'..'Z', in 'a'..'z' -> latinCount++
            }
        }

        return when {
            teluguCount >= 6 && teluguCount >= devanagariCount -> SongLanguage.TELUGU
            devanagariCount >= 6 && devanagariCount > teluguCount -> SongLanguage.HINDI
            teluguCount > 0 && teluguCount * 3 >= latinCount -> SongLanguage.TELUGU
            devanagariCount > 0 && devanagariCount * 3 >= latinCount -> SongLanguage.HINDI
            else -> SongLanguage.ENGLISH
        }
    }

    fun parseDocumentIntoSongs(
        fileName: String,
        rawText: String,
        documentLinks: List<String> = emptyList(),
        baseWarnings: List<String> = emptyList(),
    ): List<ImportedSongDraft> {
        val cleaned = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()

        if (cleaned.isBlank()) {
            return emptyList()
        }

        val blocks = splitIntoSongBlocks(cleaned)
        if (blocks.isEmpty()) return emptyList()

        return blocks.mapIndexedNotNull { index, block ->
            parseSingleSongBlock(
                fileName = fileName,
                rawBlock = block,
                blockIndex = index,
                totalBlocks = blocks.size,
                documentLinks = if (blocks.size == 1) documentLinks else emptyList(),
                baseWarnings = baseWarnings,
            )
        }
    }

    private fun splitIntoSongBlocks(text: String): List<String> {
        val lines = text.split("\n")
        val blocks = mutableListOf<String>()
        val currentBlock = mutableListOf<String>()
        var consecutiveBlankLines = 0

        fun flushBlock() {
            val joined = currentBlock.joinToString("\n").trim()
            if (joined.isNotBlank()) {
                blocks.add(joined)
            }
            currentBlock.clear()
        }

        for (i in lines.indices) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            // Page break form-feed often separates songs in PDFs/DOCX
            if (rawLine.contains("\u000C")) {
                val cleanPart = rawLine.replace("\u000C", "").trim()
                if (currentBlock.isNotEmpty()) {
                    // Only split on page break if next content looks like a new song header/title or block already has substantial lyrics
                    val nonEmptyCount = currentBlock.count { it.isNotBlank() }
                    if (nonEmptyCount >= 4) {
                        flushBlock()
                    }
                }
                if (cleanPart.isNotBlank()) {
                    currentBlock.add(cleanPart)
                }
                consecutiveBlankLines = 0
                continue
            }

            if (EXPLICIT_SEPARATOR_REGEX.matches(trimmed)) {
                if (currentBlock.isNotEmpty()) {
                    flushBlock()
                }
                consecutiveBlankLines = 0
                continue
            }

            if (trimmed.isEmpty()) {
                consecutiveBlankLines++
                if (currentBlock.isNotEmpty()) {
                    currentBlock.add("")
                }
                continue
            }

            val isDocxHeading = trimmed.startsWith(HEADING_MARKER)
            val displayLine = trimmed.removePrefix(HEADING_MARKER).trim()

            val isExplicitSongHeader = NUMBERED_SONG_HEADER_REGEX.matches(displayLine) ||
                displayLine.startsWith("Title:", ignoreCase = true) ||
                (consecutiveBlankLines >= 1 && NUMBERED_PREFIX_TITLE_REGEX.matches(displayLine)) ||
                (isDocxHeading && !SECTION_LINE_CHECK_REGEX.matches(displayLine) && currentBlock.count { it.isNotBlank() } >= 3)

            // Also detect 2+ blank lines before a short title-like line when current block already has >= 5 lines
            val isImplicitNewSong = !isExplicitSongHeader &&
                consecutiveBlankLines >= 2 &&
                currentBlock.count { it.isNotBlank() } >= 5 &&
                displayLine.length in 3..70 &&
                !SECTION_LINE_CHECK_REGEX.matches(displayLine) &&
                !METADATA_LINE_REGEX.matches(displayLine) &&
                (lines.getOrNull(i + 1)?.trim()?.isEmpty() == true ||
                    SECTION_LINE_CHECK_REGEX.matches(lines.getOrNull(i + 1)?.trim().orEmpty()) ||
                    METADATA_LINE_REGEX.matches(lines.getOrNull(i + 1)?.trim().orEmpty()))

            if ((isExplicitSongHeader || isImplicitNewSong) && currentBlock.count { it.isNotBlank() } >= 2) {
                flushBlock()
            }

            currentBlock.add(displayLine)
            consecutiveBlankLines = 0
        }

        flushBlock()
        return blocks
    }

    private fun parseSingleSongBlock(
        fileName: String,
        rawBlock: String,
        blockIndex: Int,
        totalBlocks: Int,
        documentLinks: List<String>,
        baseWarnings: List<String>,
    ): ImportedSongDraft? {
        val cleanRawBlock = rawBlock.replace(HEADING_MARKER, "").trim()
        val lines = cleanRawBlock.split("\n")
        if (lines.all { it.isBlank() }) return null

        var title = ""
        var alternateTitle = ""
        var explicitLanguage: SongLanguage? = null
        var originalKey = ""
        var capo = ""
        var category = ""
        val tags = mutableListOf<String>()
        var videoUrl = ""
        var audioUrl = ""
        var otherSourceUrl = ""
        var lyricist = ""
        var composer = ""
        var translator = ""
        var copyrightOwner = ""
        var licenseInfo = ""
        val additionalCredits = mutableListOf<String>()

        val bodyLines = mutableListOf<String>()
        val warnings = baseWarnings.toMutableList()
        var foundFirstBodyContent = false

        fun assignUrl(url: String, hint: String = "") {
            val lower = url.lowercase()
            val hintLower = hint.lowercase()
            when {
                hintLower.contains("video") || hintLower.contains("youtube") ||
                    lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("vimeo.com") -> {
                    if (videoUrl.isBlank()) videoUrl = url
                }
                hintLower.contains("audio") || lower.endsWith(".mp3") || lower.endsWith(".wav") ||
                    lower.contains("spotify.com") || lower.contains("soundcloud.com") -> {
                    if (audioUrl.isBlank()) audioUrl = url
                }
                else -> {
                    if (otherSourceUrl.isBlank()) otherSourceUrl = url
                }
            }
        }

        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                if (foundFirstBodyContent) {
                    bodyLines.add("")
                }
                continue
            }

            // Extract metadata lines
            val metaMatch = METADATA_LINE_REGEX.find(trimmed)
            if (metaMatch != null) {
                val field = metaMatch.groupValues[1].lowercase().replace(Regex("""\s+"""), " ").trim()
                val value = metaMatch.groupValues[2].trim()
                when {
                    field == "title" -> if (title.isBlank()) title = value
                    field == "alternate title" || field == "alt title" -> alternateTitle = value
                    field == "language" -> explicitLanguage = SongLanguage.fromCode(value)
                    field == "key" || field == "original key" || field == "scale" -> originalKey = value
                    field == "capo" -> capo = value
                    field == "category" -> category = value
                    field == "tags" -> tags.addAll(value.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() })
                    field == "video" || field == "youtube" -> {
                        val url = URL_REGEX.find(value)?.value ?: value
                        assignUrl(url, "video")
                    }
                    field == "audio" -> {
                        val url = URL_REGEX.find(value)?.value ?: value
                        assignUrl(url, "audio")
                    }
                    field == "source" || field == "link" -> {
                        val url = URL_REGEX.find(value)?.value ?: value
                        assignUrl(url, "source")
                    }
                    field == "lyricist" || field == "lyrics by" || field == "writer" || field == "author" -> lyricist = value
                    field == "composer" || field == "music" || field == "tune" -> composer = value
                    field == "translator" || field == "translated by" -> translator = value
                    field == "copyright" -> copyrightOwner = value
                    field == "license" -> licenseInfo = value
                    field == "credits" -> additionalCredits.add(value)
                }
                continue
            }

            // Check bare URL lines
            val urlOnlyMatch = URL_REGEX.find(trimmed)
            if (urlOnlyMatch != null && trimmed.replace(urlOnlyMatch.value, "").trim().length <= 4) {
                assignUrl(urlOnlyMatch.value)
                continue
            }

            // First non-metadata line may be the title
            if (!foundFirstBodyContent && title.isBlank() && !SECTION_LINE_CHECK_REGEX.matches(trimmed)) {
                val numberedSongMatch = NUMBERED_SONG_HEADER_REGEX.find(trimmed)
                val numberedPrefixMatch = NUMBERED_PREFIX_TITLE_REGEX.find(trimmed)
                title = when {
                    numberedSongMatch != null && numberedSongMatch.groupValues[1].isNotBlank() ->
                        numberedSongMatch.groupValues[1].trim()
                    numberedPrefixMatch != null ->
                        numberedPrefixMatch.groupValues[1].trim()
                    else -> trimmed
                }
                // Check if next line is an alternate/transliterated title in parentheses or short subtitle before blank line
                continue
            }

            foundFirstBodyContent = true
            bodyLines.add(line)
        }

        for (docLink in documentLinks) {
            assignUrl(docLink)
        }

        val lyricsBodyText = bodyLines.joinToString("\n").trim()
        val effectiveLyricsText = lyricsBodyText.ifBlank { cleanRawBlock }
        val sections = ChordLyricParser.parseToSections(effectiveLyricsText)
        val plainLyrics = ChordLyricParser.sectionsToPlainLyrics(sections).ifBlank { effectiveLyricsText }

        if (title.isBlank()) {
            val firstLine = plainLyrics.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            title = if (firstLine.isNotBlank()) {
                firstLine.take(60)
            } else {
                val baseName = fileName.substringBeforeLast(".")
                if (totalBlocks > 1) "$baseName (Song ${blockIndex + 1})" else baseName
            }
            warnings.add("Song title was not explicitly marked and was inferred; please verify.")
        }

        val detectedLanguage = explicitLanguage ?: detectLanguage("$title\n$plainLyrics")
        val hasExplicitSections = sections.size > 1 ||
            (sections.size == 1 && !sections[0].label.equals("Lyrics", ignoreCase = true))

        if (!hasExplicitSections && plainLyrics.lines().size > 4) {
            warnings.add("No section headings (Pallavi/Charanam/Verse/Chorus) detected; verify structure.")
        }

        if (lyricsBodyText.isBlank()) {
            warnings.add("Lyrics body was short or merged with title; verify extracted lines.")
        }

        val chordSummary = ChordLyricParser.extractChordSummary(sections)
        val needsReview = warnings.isNotEmpty()

        return ImportedSongDraft(
            draftId = UUID.randomUUID().toString(),
            sourceFileName = fileName,
            title = title,
            alternateTitle = alternateTitle,
            language = detectedLanguage,
            sections = sections,
            rawLyrics = plainLyrics,
            rawExtractedText = cleanRawBlock,
            chordsSummary = chordSummary,
            originalKey = originalKey,
            capo = capo,
            category = category,
            tags = tags.distinct(),
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            otherSourceUrl = otherSourceUrl,
            lyricist = lyricist,
            composer = composer,
            translator = translator,
            copyrightOwner = copyrightOwner,
            licenseInfo = licenseInfo,
            additionalCredits = additionalCredits.joinToString("; "),
            warnings = warnings.distinct(),
            needsManualReview = needsReview,
        )
    }
}
