package com.example.domain.importer

import com.example.domain.model.DuplicateMatch
import com.example.domain.model.DuplicateReason
import com.example.domain.model.Song
import com.example.domain.model.SongLanguage
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

object DuplicateDetector {

    fun findMatches(
        title: String,
        language: SongLanguage,
        lyrics: String,
        existingSongs: List<Song>,
    ): List<DuplicateMatch> {
        val normTitle = normalizeText(title)
        val normLyrics = normalizeText(lyrics)
        val lyricTokens = tokenize(normLyrics)

        val matches = mutableListOf<DuplicateMatch>()

        for (existing in existingSongs) {
            val existingNormTitle = normalizeText(existing.title)
            val existingAltTitle = normalizeText(existing.alternateTitle)
            val existingNormLyrics = normalizeText(existing.lyrics)

            // 1. Exact title match in same language
            if (normTitle.isNotEmpty() &&
                existing.language == language &&
                (normTitle == existingNormTitle || (existingAltTitle.isNotEmpty() && normTitle == existingAltTitle))
            ) {
                matches.add(
                    DuplicateMatch(
                        existingSong = existing,
                        reason = DuplicateReason.EXACT_TITLE,
                        similarityScore = 1.0f
                    )
                )
                continue
            }

            // 2. High title similarity
            val titleSim = if (normTitle.length >= 3 && existingNormTitle.length >= 3) {
                stringSimilarity(normTitle, existingNormTitle)
            } else {
                0f
            }
            if (titleSim >= 0.84f && existing.language == language) {
                matches.add(
                    DuplicateMatch(
                        existingSong = existing,
                        reason = DuplicateReason.SIMILAR_TITLE,
                        similarityScore = titleSim
                    )
                )
                continue
            }

            // 3. Lyric overlap (token Jaccard or prefix match)
            if (normLyrics.length >= 30 && existingNormLyrics.length >= 30) {
                val existingTokens = tokenize(existingNormLyrics)
                val jaccard = jaccardSimilarity(lyricTokens, existingTokens)
                val prefixLen = min(80, min(normLyrics.length, existingNormLyrics.length))
                val prefixMatch = normLyrics.take(prefixLen) == existingNormLyrics.take(prefixLen)
                if (prefixMatch || jaccard >= 0.65f) {
                    matches.add(
                        DuplicateMatch(
                            existingSong = existing,
                            reason = DuplicateReason.HIGH_LYRIC_OVERLAP,
                            similarityScore = if (prefixMatch) max(jaccard, 0.90f) else jaccard
                        )
                    )
                }
            }
        }

        return matches.sortedByDescending { it.similarityScore }
    }

    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        return nfc.lowercase()
            .replace(Regex("""\[[^\]]*\]"""), " ") // strip inline chords
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun tokenize(normalized: String): Set<String> {
        if (normalized.isEmpty()) return emptySet()
        return normalized.split(" ").filter { it.length >= 2 }.toSet()
    }

    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.intersect(b).size
        val union = a.union(b).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun stringSimilarity(a: String, b: String): Float {
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1.0f
        val dist = levenshteinDistance(a, b)
        return 1.0f - (dist.toFloat() / maxLen.toFloat())
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}
