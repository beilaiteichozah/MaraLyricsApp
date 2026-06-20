package com.maralyrics.utils

object MaraAlphabetUtils {
    /**
     * Mara Alphabet order: Â, A, AW, Y, B, CH, D, E, F, H, I, K, L, M, N, NG, O, Ô, P, R, S, T, U, V, Z
     */
    private val MARA_ALPHABET = listOf(
        "Â", "A", "AW", "Y", "B", "CH", "D", "E", "F", "H", "I", "K", "L", "M", "N", "NG", "O", "Ô", "P", "R", "S", "T", "U", "V", "Z"
    )

    private val MARA_ORDER_MAP = MARA_ALPHABET.withIndex().associate { it.value to it.index }

    /**
     * Extracts the first Mara "letter" from a string. 
     * Handles multi-char letters like AW, CH, NG.
     */
    fun getFirstLetter(text: String): String {
        if (text.isBlank()) return "#"
        val upper = text.trim().uppercase()
        
        // Check for 2-character letters first
        if (upper.length >= 2) {
            val firstTwo = upper.substring(0, 2)
            if (firstTwo == "AW" || firstTwo == "CH" || firstTwo == "NG") {
                return firstTwo
            }
        }
        
        val firstChar = upper[0].toString()
        return if (MARA_ORDER_MAP.containsKey(firstChar)) firstChar else "#"
    }

    /**
     * Compares two strings based on the Mara alphabet.
     */
    fun compare(s1: String, s2: String): Int {
        var i = 0
        var j = 0
        
        while (i < s1.length && j < s2.length) {
            val l1 = getLetterAt(s1, i)
            val l2 = getLetterAt(s2, j)
            
            val rank1 = MARA_ORDER_MAP[l1.uppercase()] ?: (100 + l1[0].code)
            val rank2 = MARA_ORDER_MAP[l2.uppercase()] ?: (100 + l2[0].code)
            
            if (rank1 != rank2) return rank1.compareTo(rank2)
            
            i += l1.length
            j += l2.length
        }
        
        return s1.length.compareTo(s2.length)
    }

    private fun getLetterAt(s: String, index: Int): String {
        val remaining = s.substring(index).uppercase()
        if (remaining.length >= 2) {
            val firstTwo = remaining.substring(0, 2)
            if (firstTwo == "AW" || firstTwo == "CH" || firstTwo == "NG") {
                return firstTwo
            }
        }
        return s[index].toString()
    }

    fun getAlphabet(): List<String> = MARA_ALPHABET
}
