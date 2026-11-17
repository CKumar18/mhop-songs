package com.example.domain.chord

object ChordTransposer {
    private val SHARP_SCALE = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLAT_SCALE = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val NOTE_TO_INDEX = mapOf(
        "C" to 0, "B#" to 0,
        "C#" to 1, "DB" to 1,
        "D" to 2,
        "D#" to 3, "EB" to 3,
        "E" to 4, "FB" to 4,
        "F" to 5, "E#" to 5,
        "F#" to 6, "GB" to 6,
        "G" to 7,
        "G#" to 8, "AB" to 8,
        "A" to 9,
        "A#" to 10, "BB" to 10,
        "B" to 11, "CB" to 11,
    )

    private val CHORD_TOKEN_REGEX = Regex(
        """^([A-G][#b]?)(m|maj|min|dim|aug|sus[24]?|add[0-9]+|[0-9]+|M7|m7|maj7|dim7|m7b5|7sus4)*(\/([A-G][#b]?))?$"""
    )

    private val ROOT_REGEX = Regex("""^([A-G][#b]?)(.*)$""")

    fun isValidChordToken(token: String): Boolean {
        val cleaned = token.trim().trim('(', ')', '[', ']')
        if (cleaned.isEmpty()) return false
        return CHORD_TOKEN_REGEX.matches(cleaned)
    }

    fun isChordOnlyLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        val tokens = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        val validCount = tokens.count { isValidChordToken(it) }
        // Avoid misclassifying single-letter English words like "A" or "I" (I isn't A-G anyway)
        if (tokens.size == 1 && tokens[0] == "A") return false
        return validCount == tokens.size
    }

    fun transposeChord(chord: String, semitones: Int, preferFlats: Boolean = false): String {
        if (semitones == 0 || chord.isBlank()) return chord
        val parts = chord.split("/")
        val mainTransposed = transposeRootAndSuffix(parts[0].trim(), semitones, preferFlats)
        return if (parts.size == 2) {
            val bassTransposed = transposeRootAndSuffix(parts[1].trim(), semitones, preferFlats)
            "$mainTransposed/$bassTransposed"
        } else {
            mainTransposed
        }
    }

    fun transposeKey(originalKey: String, semitones: Int): String {
        if (originalKey.isBlank() || semitones == 0) return originalKey
        val trimmed = originalKey.trim()
        val preferFlats = trimmed.contains("b") || trimmed.startsWith("F")
        return transposeRootAndSuffix(trimmed, semitones, preferFlats)
    }

    private fun transposeRootAndSuffix(part: String, semitones: Int, preferFlats: Boolean): String {
        val match = ROOT_REGEX.find(part) ?: return part
        val root = match.groupValues[1]
        val suffix = match.groupValues[2]
        val index = NOTE_TO_INDEX[root.uppercase()] ?: return part
        val targetIndex = ((index + semitones) % 12 + 12) % 12
        val scale = if (preferFlats || root.endsWith("b")) FLAT_SCALE else SHARP_SCALE
        return scale[targetIndex] + suffix
    }
}
