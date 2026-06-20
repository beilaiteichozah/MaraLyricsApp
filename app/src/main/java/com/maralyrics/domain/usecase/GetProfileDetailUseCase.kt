package com.maralyrics.domain.usecase

import com.maralyrics.domain.model.Profile
import com.maralyrics.domain.model.ProfileType
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.repository.ArtistRepository
import com.maralyrics.domain.repository.ComposerRepository
import com.maralyrics.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfileDetailUseCase @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val composerRepository: ComposerRepository,
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(slug: String, type: ProfileType): ProfileWithSongs? {
        return when (type) {
            ProfileType.ARTIST -> {
                val artist = artistRepository.getArtistBySlug(slug) ?: return null
                ProfileWithSongs(
                    profile = Profile(
                        id = artist.id,
                        slug = artist.slug,
                        name = artist.name,
                        bio = artist.bio,
                        imageUrl = artist.imageUrl,
                        socialLinks = artist.socialLinks,
                        songCount = artist.songCount,
                        type = ProfileType.ARTIST
                    ),
                    songs = songRepository.getSongsByArtist(artist.id)
                )
            }
            ProfileType.COMPOSER -> {
                val composer = composerRepository.getComposerBySlug(slug) ?: return null
                ProfileWithSongs(
                    profile = Profile(
                        id = composer.id,
                        slug = composer.slug,
                        name = composer.name,
                        bio = composer.bio,
                        imageUrl = composer.imageUrl,
                        socialLinks = composer.socialLinks,
                        songCount = composer.songCount,
                        type = ProfileType.COMPOSER
                    ),
                    songs = songRepository.getSongsByComposer(composer.id)
                )
            }
        }
    }
}

data class ProfileWithSongs(
    val profile: Profile,
    val songs: Flow<List<Song>>
)
