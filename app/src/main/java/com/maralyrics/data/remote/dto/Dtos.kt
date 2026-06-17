package com.maralyrics.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("lyrics") val lyrics: String,
    @SerialName("category") val category: String,
    @SerialName("song_number") val songNumber: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class SongsResponse(
    @SerialName("songs") val songs: List<SongDto>,
    @SerialName("total") val total: Int,
    @SerialName("version") val version: Int
)

@Serializable
data class VersionResponse(
    @SerialName("version") val version: Int,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class IncrementalSyncResponse(
    @SerialName("songs") val songs: List<SongDto>,
    @SerialName("deleted_ids") val deletedIds: List<Long> = emptyList(),
    @SerialName("version") val version: Int
)