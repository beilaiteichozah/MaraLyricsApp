package com.maralyrics.domain.usecase

import com.maralyrics.domain.model.Song
import com.maralyrics.domain.model.SongSortOrder
import com.maralyrics.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(category: String?, sortOrder: SongSortOrder): Flow<List<Song>> {
        return songRepository.getAllSongs(category, sortOrder)
    }
}
