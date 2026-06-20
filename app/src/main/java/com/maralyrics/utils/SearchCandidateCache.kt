package com.maralyrics.utils

import com.maralyrics.domain.model.SuggestionType
import javax.inject.Inject
import javax.inject.Singleton

data class Candidate(
    val id: Long,
    val text: String,
    val normalizedText: String,
    val type: SuggestionType,
    val views: Int = 0,
    val slug: String = ""
)

@Singleton
class SearchCandidateCache @Inject constructor() {
    private var candidates: List<Candidate> = emptyList()
    private var lastUpdated: Long = 0

    fun update(newCandidates: List<Candidate>) {
        candidates = newCandidates
        lastUpdated = System.currentTimeMillis()
    }

    fun getCandidates(): List<Candidate> = candidates

    fun isEmpty(): Boolean = candidates.isEmpty()

    fun getCandidateCount(): Int = candidates.size
}
