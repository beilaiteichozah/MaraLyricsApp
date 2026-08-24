package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.local.entity.ArtistEntity
import com.maralyrics.laitei.data.local.entity.ComposerEntity
import com.maralyrics.laitei.data.local.entity.CopyrightOwnerEntity
import com.maralyrics.laitei.data.local.entity.SongEntity
import com.maralyrics.laitei.data.remote.dto.ArtistDto
import com.maralyrics.laitei.data.remote.dto.ComposerDto
import com.maralyrics.laitei.data.remote.dto.CopyrightOwnerDto
import com.maralyrics.laitei.data.remote.dto.SongDto

fun SongDto.toEntity() = SongEntity(
    id = id,
    slug = slug,
    title = title,
    lyrics = lyrics,
    category = category,
    createdAt = 0L, // Should ideally parse date string if needed
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
    socialLinks = null // Handle serialization if needed
)

fun ComposerDto.toEntity() = ComposerEntity(
    id = id,
    slug = slug,
    name = name,
    bio = bio,
    imageUrl = imageUrl,
    socialLinks = null // Handle serialization if needed
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
    createdAt = 0L // Should ideally parse date string if needed
)
