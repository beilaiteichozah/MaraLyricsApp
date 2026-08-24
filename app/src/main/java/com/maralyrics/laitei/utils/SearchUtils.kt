package com.maralyrics.laitei.utils

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object SearchUtils {

    /**
     * Normalizes text by:
     * 1. Converting to lowercase.
     * 2. Decomposing diacritics (e.g., â -> a + ^).
     * 3. Removing combining marks (accents).
     * 4. Removing non-alphanumeric characters except spaces.
     * 5. Normalizing multiple spaces to a single space.
     */
    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.US)
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
        val stripped = normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val result = stripped.replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return result
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     * Uses a memory-efficient O(min(n,m)) space implementation.
     */
    fun calculateLevenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val n = s1.length
        val m = s2.length
        
        // Ensure s2 is the shorter string to save space
        if (n < m) return calculateLevenshtein(s2, s1)

        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)

        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[m]
    }

    /**
     * Returns a similarity score between 0.0 and 1.0.
     * 1.0 means identical, 0.0 means completely different.
     */
    fun getSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val maxLength = max(s1.length, s2.length)
        if (maxLength == 0) return 1.0
        val distance = calculateLevenshtein(s1, s2)
        return 1.0 - (distance.toDouble() / maxLength)
    }
}
