package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.local.dao.SongWithArtistAndComposer
import com.maralyrics.laitei.data.local.entity.*
import com.maralyrics.laitei.data.remote.dto.*
import com.maralyrics.laitei.domain.model.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// A List, not a Map — the same platform can appear more than once (e.g. two Facebook links).
private val socialLinksListSerializer = ListSerializer(PairSerializer(String.serializer(), String.serializer()))

// Room stores social links as a single JSON TEXT column rather than a related table.
private fun List<Pair<String, String>>?.toSocialLinksJson(): String? =
    this?.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(socialLinksListSerializer, it) }

private fun String?.toSocialLinks(): List<SocialLink> =
    this?.let {
        try {
            Json.decodeFromString(socialLinksListSerializer, it).map { (platform, url) -> SocialLink(platform, url) }
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()

// The API sends "yyyy-MM-dd HH:mm:ss" (server/UTC time, no offset). Only relative ordering
// (most-recent song, New-Old sort) depends on this value, so the exact zone doesn't matter.
private fun String.toEpochMillisOrZero(): Long =
    try {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(this)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }

fun SongDto.toEntity() = SongEntity(
    id = id,
    slug = slug,
    title = title,
    lyrics = lyrics,
    category = category,
    createdAt = createdAt.toEpochMillisOrZero(),
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
    socialLinks = socialLinks.toSocialLinksJson()
)

fun ComposerDto.toEntity() = ComposerEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = socialLinks.toSocialLinksJson()
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

// Skips the @Relation artist/composer list lookup entirely — for callers (widgets,
// notifications) that only need the song's own denormalized fields, since Room's
// relation-population code throws for a plain SELECT * FROM songs ... LIMIT 1 query
// when the picked song has zero rows in the artist/composer junction tables.
fun SongEntity.toDomain(isFavorite: Boolean = false) = Song(
    id = id,
    slug = slug,
    title = title,
    lyrics = lyrics,
    category = category ?: "",
    createdAt = createdAt,
    artistId = artistId,
    artistName = artistName,
    artistSlug = artistSlug,
    composerId = composerId,
    composerName = composerName,
    composerSlug = composerSlug,
    copyrightOwnerId = copyrightOwnerId,
    copyrightOwnerName = copyrightOwnerName,
    isFavorite = isFavorite,
    views = views,
    artists = emptyList(),
    composers = emptyList()
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
    socialLinks = socialLinks.toSocialLinks(),
    songCount = songCount
)

fun ComposerEntity.toDomain(songCount: Int = 0) = Composer(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = socialLinks.toSocialLinks(),
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
    socialLinks = socialLinks.map { it.platform to it.url }.toSocialLinksJson()
)

fun Composer.toEntity() = ComposerEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = socialLinks.map { it.platform to it.url }.toSocialLinksJson()
)
