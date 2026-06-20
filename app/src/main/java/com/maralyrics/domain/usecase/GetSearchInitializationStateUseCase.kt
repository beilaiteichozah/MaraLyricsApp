package com.maralyrics.domain.usecase

import com.maralyrics.domain.model.SearchInitializationState
import com.maralyrics.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchInitializationStateUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<SearchInitializationState> = songRepository.getSearchInitializationState()
}
