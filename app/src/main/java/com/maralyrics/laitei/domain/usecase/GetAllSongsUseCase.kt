package com.maralyrics.laitei.domain.usecase

import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.domain.model.SongSortOrder
import com.maralyrics.laitei.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(categories: List<String>?, sortOrder: SongSortOrder): Flow<List<Song>> {
        return songRepository.getAllSongs(categories, sortOrder)
    }
}
