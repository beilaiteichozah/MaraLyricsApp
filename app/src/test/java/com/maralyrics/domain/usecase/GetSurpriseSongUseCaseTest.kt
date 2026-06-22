package com.maralyrics.domain.usecase

import com.maralyrics.domain.model.Song
import com.maralyrics.domain.repository.SongRepository
import com.maralyrics.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetSurpriseSongUseCaseTest {

    private lateinit var songRepository: SongRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var getSurpriseSongUseCase: GetSurpriseSongUseCase

    @Before
    fun setup() {
        songRepository = mock()
        settingsRepository = mock()
        getSurpriseSongUseCase = GetSurpriseSongUseCase(songRepository, settingsRepository)
    }

    @Test
    fun `when favorites exist returns a random song and updates last id`() = runBlocking {
        val songs = listOf(
            createSong(1, "Song 1"),
            createSong(2, "Song 2"),
            createSong(3, "Song 3")
        )
        whenever(songRepository.getFavoriteSongs()).thenReturn(flowOf(songs))
        whenever(settingsRepository.getLastSurpriseSongId()).thenReturn(flowOf(null))

        val result = getSurpriseSongUseCase()

        assertNotNull(result)
        assertTrue(songs.contains(result))
        verify(settingsRepository).setLastSurpriseSongId(result!!.id)
        verify(settingsRepository).incrementSurpriseMeUses()
    }

    @Test
    fun `when multiple favorites exist avoids the last surprise song`() = runBlocking {
        val songs = listOf(
            createSong(1, "Song 1"),
            createSong(2, "Song 2")
        )
        whenever(songRepository.getFavoriteSongs()).thenReturn(flowOf(songs))
        whenever(settingsRepository.getLastSurpriseSongId()).thenReturn(flowOf(1L))

        val result = getSurpriseSongUseCase()

        assertNotNull(result)
        assertEquals(2L, result!!.id)
        verify(settingsRepository).setLastSurpriseSongId(2L)
    }

    @Test
    fun `when only one favorite exists returns that song even if it was the last one`() = runBlocking {
        val songs = listOf(createSong(1, "Song 1"))
        whenever(songRepository.getFavoriteSongs()).thenReturn(flowOf(songs))
        whenever(settingsRepository.getLastSurpriseSongId()).thenReturn(flowOf(1L))

        val result = getSurpriseSongUseCase()

        assertNotNull(result)
        assertEquals(1L, result!!.id)
        verify(settingsRepository).setLastSurpriseSongId(1L)
    }

    @Test
    fun `when no favorites exist returns null`() = runBlocking {
        whenever(songRepository.getFavoriteSongs()).thenReturn(flowOf(emptyList()))

        val result = getSurpriseSongUseCase()

        assertNull(result)
    }

    private fun createSong(id: Long, title: String) = Song(
        id = id,
        slug = "song-$id",
        title = title,
        lyrics = "Lyrics",
        category = "Gospel",
        createdAt = 0L
    )
}
