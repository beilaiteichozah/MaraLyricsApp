package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.local.dao.SongWithArtistAndComposer
import com.maralyrics.laitei.data.local.entity.*
import com.maralyrics.laitei.data.remote.dto.*
import com.maralyrics.laitei.domain.model.*

fun SongDto.toEntity() = SongEntity(
    id = id,
    slug = slug,
    title = title,
    lyrics = lyrics,
    category = category,
    createdAt = 0L,
    views = views,
    artistId = artistId,
    composerId = composerId,
    copyrightOwnerId = copyrightOwnerId,
    artistName = artistName,
    artistSlug = artistSlug,
    composerName = composerName,
    composerSlug = composerSlug,
    copyrightOwnerName = copyrightOwnerName
)

fun ArtistDto.toEntity() = ArtistEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = null 
)

fun ComposerDto.toEntity() = ComposerEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = null
)

fun CopyrightOwnerDto.toEntity() = CopyrightOwnerEntity(
    id = id,
    name = name,
    slug = slug,
    fullLegalName = fullLegalName,
    organization = organization,
    territory = territory,
    email = email,
    website = website,
    address = address,
    ipiNumber = ipiNumber,
    isrcPrefix = isrcPrefix,
    proAffiliation = proAffiliation,
    notes = notes,
    createdAt = 0L
)

fun SongWithArtistAndComposer.toDomain(isFavorite: Boolean = false) = Song(
    id = song.id,
    slug = song.slug,
    title = song.title,
    lyrics = song.lyrics,
    category = song.category ?: "",
    createdAt = song.createdAt,
    artistId = song.artistId,
    artistName = song.artistName,
    artistSlug = song.artistSlug,
    composerId = song.composerId,
    composerName = song.composerName,
    composerSlug = song.composerSlug,
    copyrightOwnerId = song.copyrightOwnerId,
    copyrightOwnerName = song.copyrightOwnerName,
    isFavorite = isFavorite,
    views = song.views,
    artists = artists.map { it.toSongContributor() },
    composers = composers.map { it.toSongContributor() }
)

fun ArtistEntity.toSongContributor() = SongContributor(
    id = id,
    name = name,
    slug = slug
)

fun ComposerEntity.toSongContributor() = SongContributor(
    id = id,
    name = name,
    slug = slug
)

fun ArtistEntity.toDomain(songCount: Int = 0) = Artist(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = emptyList(), // Needs parsing if present
    songCount = songCount
)

fun ComposerEntity.toDomain(songCount: Int = 0) = Composer(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = emptyList(), // Needs parsing if present
    songCount = songCount
)

fun Feedback.toEntity() = FeedbackEntity(
    id = id,
    songId = songId,
    songSlug = songSlug,
    songTitle = songTitle,
    songArtist = artistName,
    name = name,
    email = email,
    message = message,
    isSynced = isSynced
)

fun FeedbackEntity.toDomain() = Feedback(
    id = id,
    songId = songId,
    songSlug = songSlug,
    songTitle = songTitle,
    artistName = songArtist,
    name = name,
    email = email,
    message = message,
    isSynced = isSynced
)

fun Song.toEntity() = SongEntity(
    id = id,
    slug = slug,
    title = title,
    lyrics = lyrics,
    category = category,
    createdAt = createdAt,
    views = views,
    artistId = artistId,
    composerId = composerId,
    copyrightOwnerId = copyrightOwnerId,
    artistName = artistName,
    artistSlug = artistSlug,
    composerName = composerName,
    composerSlug = composerSlug,
    copyrightOwnerName = copyrightOwnerName
)

fun Artist.toEntity() = ArtistEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = null 
)

fun Composer.toEntity() = ComposerEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = null
)
