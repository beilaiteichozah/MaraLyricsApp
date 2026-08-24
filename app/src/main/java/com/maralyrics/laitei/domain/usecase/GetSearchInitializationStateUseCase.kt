package com.maralyrics.laitei.domain.usecase

import com.maralyrics.laitei.domain.model.SearchInitializationState
import com.maralyrics.laitei.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchInitializationStateUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<SearchInitializationState> = songRepository.getSearchInitializationState()
}
