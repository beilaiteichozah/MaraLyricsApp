package com.maralyrics.laitei.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object SocialLinksSerializer : KSerializer<Map<String, String>?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SocialLinks", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Map<String, String>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        return parseSocialLinksElement(element)
    }

    private fun parseSocialLinksElement(element: JsonElement): Map<String, String>? {
        return when (element) {
            is JsonObject -> {
                element.mapValues { it.value.jsonPrimitive.content }
            }
            is JsonArray -> {
                val map = mutableMapOf<String, String>()
                element.forEach { item ->
                    if (item is JsonPrimitive && item.isString) {
                        val url = item.content
                        val platform = extractPlatform(url)
                        map[platform] = url
                    }
                }
                map.ifEmpty { null }
            }
            is JsonPrimitive -> {
                if (element.isString) {
                    val content = element.content.trim()
                    if (content.isEmpty()) return null
                    
                    // Try to parse as JSON if it looks like one
                    if (content.startsWith("[") || content.startsWith("{")) {
                        try {
                            val subElement = Json.parseToJsonElement(content)
                            return parseSocialLinksElement(subElement)
                        } catch (e: Exception) {
                            // Fallback to treating it as a single URL
                        }
                    }
                    val platform = extractPlatform(content)
                    mapOf(platform to content)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun extractPlatform(url: String): String {
        return when {
            url.contains("facebook.com", ignoreCase = true) -> "facebook"
            url.contains("instagram.com", ignoreCase = true) -> "instagram"
            url.contains("twitter.com", ignoreCase = true) || url.contains("x.com", ignoreCase = true) -> "twitter"
            url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "youtube"
            else -> "website"
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, String>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(value.mapValues { JsonPrimitive(it.value) }))
        }
    }
}

@Serializable
data class SongDto(
    @SerialName("id") val id: Long,
    @SerialName("slug") val slug: String,
    @SerialName("title") val title: String,
    @SerialName("lyrics") val lyrics: String,
    @SerialName("category") val category: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("views") val views: Int = 0,
    // Upgraded API singular objects
    @SerialName("artist") val artist: SongContributorDto? = null,
    @SerialName("composer") val composer: SongContributorDto? = null,
    @SerialName("copyright_owner") val copyrightOwner: CopyrightOwnerDto? = null,
    // Plural relationships
    @SerialName("artists") val artists: List<SongContributorDto> = emptyList(),
    @SerialName("composers") val composers: List<SongContributorDto> = emptyList(),
    // Legacy/Convenience singular fields
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("artist_slug") val artistSlug: String? = null,
    @SerialName("composer_name") val composerName: String? = null,
    @SerialName("composer_slug") val composerSlug: String? = null,
    @SerialName("copyright_owner_name") val copyrightOwnerName: String? = null,
    @SerialName("copyright") val copyright: String? = null,
    @SerialName("copyright_text") val copyrightText: String? = null,
    @SerialName("copyright_notice") val copyrightNotice: String? = null,
    // Foreign keys
    @SerialName("artist_id") val artistId: Long? = null,
    @SerialName("composer_id") val composerId: Long? = null,
    @SerialName("copyright_owner_id") val copyrightOwnerId: Long? = null
)

@Serializable
data class SongContributorDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String
)

@Serializable
data class ArtistDto(
    @SerialName("id") val id: Long,
    @SerialName("slug") val slug: String,
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @Serializable(with = SocialLinksSerializer::class)
    @SerialName("social_links") val socialLinks: Map<String, String>? = null
)

@Serializable
data class ComposerDto(
    @SerialName("id") val id: Long,
    @SerialName("slug") val slug: String,
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @Serializable(with = SocialLinksSerializer::class)
    @SerialName("social_links") val socialLinks: Map<String, String>? = null
)

@Serializable
data class CopyrightOwnerDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("full_legal_name") val fullLegalName: String? = null,
    @SerialName("organization") val organization: String? = null,
    @SerialName("territory") val territory: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("website") val website: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("ipi_number") val ipiNumber: String? = null,
    @SerialName("isrc_prefix") val isrcPrefix: String? = null,
    @SerialName("pro_affiliation") val proAffiliation: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class BootstrapResponse(
    @SerialName("version") val version: Int,
    @SerialName("since") val since: String? = null,
    @SerialName("counts") val counts: Map<String, Int> = emptyMap(),
    @SerialName("songs") val songs: List<SongDto> = emptyList(),
    @SerialName("artists") val artists: List<ArtistDto> = emptyList(),
    @SerialName("composers") val composers: List<ComposerDto> = emptyList(),
    @SerialName("copyright_owners") val copyrightOwners: List<CopyrightOwnerDto> = emptyList()
)

@Serializable
data class SongsResponse(
    @SerialName("songs") val songs: List<SongDto>,
    @SerialName("total") val total: Int,
    @SerialName("page") val page: Int = 1,
    @SerialName("totalPages") val totalPages: Int = 1,
    @SerialName("version") val version: Int = 0
)

@Serializable
data class ArtistsResponse(
    @SerialName("artists") val artists: List<ArtistDto>,
    @SerialName("total") val total: Int
)

@Serializable
data class ComposersResponse(
    @SerialName("composers") val composers: List<ComposerDto>,
    @SerialName("total") val total: Int
)

@Serializable
data class VersionResponse(
    @SerialName("version") val version: Int,
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("counts") val counts: Map<String, Int> = emptyMap()
)

@Serializable
data class IncrementalSyncResponse(
    @SerialName("songs") val songs: List<SongDto> = emptyList(),
    @SerialName("artists") val artists: List<ArtistDto> = emptyList(),
    @SerialName("composers") val composers: List<ComposerDto> = emptyList(),
    @SerialName("copyright_owners") val copyrightOwners: List<CopyrightOwnerDto> = emptyList(),
    @SerialName("deleted_ids") val deletedIds: List<Long> = emptyList(),
    @SerialName("version") val version: Int
)

@Serializable
data class FeedbackRequest(
    @SerialName("song_id") val songId: Long,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("message") val message: String
)

@Serializable
data class ReportRequest(
    @SerialName("song_slug") val songSlug: String,
    @SerialName("song_title") val songTitle: String,
    @SerialName("song_artist") val songArtist: String?,
    @SerialName("reporter_name") val reporterName: String,
    @SerialName("reporter_email") val reporterEmail: String?,
    @SerialName("body") val body: String,
    @SerialName("turnstile_token") val turnstileToken: String? = null
)

@Serializable
data class ContactRequest(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("subject") val subject: String,
    @SerialName("message") val message: String,
    @SerialName("turnstile_token") val turnstileToken: String? = null
)

@Serializable
data class SearchResponseDto(
    @SerialName("results") val results: List<SongDto> = emptyList(),
    @SerialName("suggestions") val suggestions: List<String> = emptyList()
)

@Serializable
data class StatsResponse(
    @SerialName("songs") val songs: Int,
    @SerialName("artists") val artists: Int,
    @SerialName("composers") val composers: Int,
    @SerialName("copyright_owners") val copyrightOwners: Int,
    @SerialName("categories") val categories: Int,
    @SerialName("total_views") val totalViews: Int
)

@Serializable
data class ContributorDto(
    @SerialName("login") val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("contributions") val contributions: Int
)

@Serializable
data class ContributorsResponse(
    @SerialName("contributors") val contributors: List<ContributorDto>
)

@Serializable
data class CategoriesResponse(
    @SerialName("categories") val categories: List<String>
)
