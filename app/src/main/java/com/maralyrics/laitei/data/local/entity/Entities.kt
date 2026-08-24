package com.maralyrics.laitei.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["slug"], unique = true),
        Index(value = ["artist_id"]),
        Index(value = ["composer_id"]),
        Index(value = ["copyright_owner_id"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "lyrics") val lyrics: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "views") val views: Int = 0,
    @ColumnInfo(name = "artist_id") val artistId: Long? = null,
    @ColumnInfo(name = "composer_id") val composerId: Long? = null,
    @ColumnInfo(name = "copyright_owner_id") val copyrightOwnerId: Long? = null,
    // Direct metadata storage for robustness
    @ColumnInfo(name = "artist_name") val artistName: String? = null,
    @ColumnInfo(name = "artist_slug") val artistSlug: String? = null,
    @ColumnInfo(name = "composer_name") val composerName: String? = null,
    @ColumnInfo(name = "composer_slug") val composerSlug: String? = null,
    @ColumnInfo(name = "copyright_owner_name") val copyrightOwnerName: String? = null
)

@Entity(
    tableName = "copyright_owners",
    indices = [Index(value = ["slug"], unique = true)]
)
data class CopyrightOwnerEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "full_legal_name") val fullLegalName: String?,
    @ColumnInfo(name = "organization") val organization: String?,
    @ColumnInfo(name = "territory") val territory: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "website") val website: String?,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "ipi_number") val ipiNumber: String?,
    @ColumnInfo(name = "isrc_prefix") val isrcPrefix: String?,
    @ColumnInfo(name = "pro_affiliation") val proAffiliation: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "artists",
    indices = [Index(value = ["slug"], unique = true)]
)
data class ArtistEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "bio") val bio: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "social_links") val socialLinks: String? // JSON string
)

@Entity(
    tableName = "composers",
    indices = [Index(value = ["slug"], unique = true)]
)
data class ComposerEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "bio") val bio: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "social_links") val socialLinks: String? // JSON string
)

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["song_id"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "favorited_at") val favoritedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recent_views",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["song_id"], unique = true)]
)
data class RecentViewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "viewed_at") val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "song_slug") val songSlug: String = "",
    @ColumnInfo(name = "song_title") val songTitle: String = "",
    @ColumnInfo(name = "song_artist") val songArtist: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "last_sync") val lastSync: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "song_artists",
    primaryKeys = ["song_id", "artist_id"],
    foreignKeys = [
        ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artist_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("artist_id")]
)
data class SongArtistEntity(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "artist_id") val artistId: Long,
    @ColumnInfo(name = "position") val position: Int = 0
)

@Entity(
    tableName = "song_composers",
    primaryKeys = ["song_id", "composer_id"],
    foreignKeys = [
        ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ComposerEntity::class, parentColumns = ["id"], childColumns = ["composer_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("composer_id")]
)
data class SongComposerEntity(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "composer_id") val composerId: Long,
    @ColumnInfo(name = "position") val position: Int = 0
)

@Fts4(contentEntity = SongEntity::class)
@Entity(tableName = "songs_search_index")
data class SongFtsEntity(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "lyrics") val lyrics: String
)
