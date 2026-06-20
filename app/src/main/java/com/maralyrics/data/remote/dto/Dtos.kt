package com.maralyrics.data.remote.dto

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
    @SerialName("artist_id") val artistId: Long? = null,
    @SerialName("composer_id") val composerId: Long? = null,
    @SerialName("copyright_owner_id") val copyrightOwnerId: Long? = null
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
    @SerialName("songs") val songs: List<SongDto>,
    @SerialName("artists") val artists: List<ArtistDto>,
    @SerialName("composers") val composers: List<ComposerDto>,
    @SerialName("copyright_owners") val copyrightOwners: List<CopyrightOwnerDto>,
    @SerialName("version") val version: Int
)

@Serializable
data class SongsResponse(
    @SerialName("songs") val songs: List<SongDto>,
    @SerialName("total") val total: Int,
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
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class IncrementalSyncResponse(
    @SerialName("songs") val songs: List<SongDto>,
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
    @SerialName("body") val body: String
)
