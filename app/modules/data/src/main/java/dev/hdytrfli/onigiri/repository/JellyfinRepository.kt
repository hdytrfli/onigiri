package dev.hdytrfli.onigiri.repository

import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidGenre
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidPerson
import dev.hdytrfli.onigiri.models.FindroidSeason
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.models.FindroidSource
import dev.hdytrfli.onigiri.models.RecommendationCategory
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.RemoteImageInfo
import org.jellyfin.sdk.model.api.RemoteSearchResult
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo
import org.jellyfin.sdk.model.api.SessionInfoDto
import org.jellyfin.sdk.model.api.UserConfiguration
import org.jellyfin.sdk.model.api.UserPolicy

interface JellyfinRepository {
    suspend fun getPublicSystemInfo(): PublicSystemInfo

    suspend fun getUserViews(): List<BaseItemDto>

    suspend fun getEpisode(itemId: UUID): FindroidEpisode

    suspend fun getMovie(itemId: UUID): FindroidMovie

    suspend fun getShow(itemId: UUID): FindroidShow

    suspend fun getSeason(itemId: UUID): FindroidSeason

    suspend fun getLibraries(): List<FindroidCollection>

    suspend fun getItem(itemId: UUID): FindroidItem?

    suspend fun getItems(
        parentId: UUID? = null,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = false,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING,
        startIndex: Int? = null,
        limit: Int? = null,
        searchTerm: String? = null,
        genreIds: List<UUID>? = null,
    ): List<FindroidItem>

    suspend fun getGenres(includeTypes: List<BaseItemKind>): List<FindroidGenre>

    suspend fun getPerson(personId: UUID): FindroidPerson

    suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = true,
    ): List<FindroidItem>

    suspend fun getFavoriteItems(): List<FindroidItem>

    suspend fun getSearchItems(query: String): List<FindroidItem>

    suspend fun getSuggestions(): List<FindroidItem>

    suspend fun getResumeItems(): List<FindroidItem>

    suspend fun getLatestMedia(parentId: UUID): List<FindroidItem>

    suspend fun getMovieRecommendations(parentId: UUID? = null): List<RecommendationCategory>

    suspend fun getSeasons(seriesId: UUID, offline: Boolean = false): List<FindroidSeason>

    suspend fun getNextUp(seriesId: UUID? = null): List<FindroidEpisode>

    suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>? = null,
        startItemId: UUID? = null,
        limit: Int? = null,
        offline: Boolean = false,
    ): List<FindroidEpisode>

    suspend fun getMediaSources(itemId: UUID, includePath: Boolean = false): List<FindroidSource>

    suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String

    /** Downloads the original file through Jellyfin's item download endpoint. */
    suspend fun getDownload(itemId: UUID): ByteArray

    suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray?

    suspend fun postCapabilities()

    suspend fun postPlaybackStart(itemId: UUID)

    suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long, playedPercentage: Int)

    suspend fun postPlaybackProgress(itemId: UUID, positionTicks: Long, isPaused: Boolean)

    /**
     * Our own playback session as the server sees it, for the player's stats overlay. Null when the
     * server reports no session for this device.
     */
    suspend fun getPlaybackSession(): SessionInfoDto?

    suspend fun markAsFavorite(itemId: UUID)

    suspend fun unmarkAsFavorite(itemId: UUID)

    suspend fun markAsPlayed(itemId: UUID)

    suspend fun markAsUnplayed(itemId: UUID)

    fun getBaseUrl(): String

    suspend fun updateDeviceName(name: String)

    suspend fun getUserConfiguration(): UserConfiguration?

    /** The current user's policy, used to gate admin-only features (e.g. subtitle search). */
    suspend fun getUserPolicy(): UserPolicy?

    /** Searches remote subtitle providers (e.g. the OpenSubtitles plugin) for [language]. */
    suspend fun searchRemoteSubtitles(itemId: UUID, language: String): List<RemoteSubtitleInfo>

    /** Asks the server to download and attach the remote subtitle [subtitleId] to [itemId]. */
    suspend fun downloadRemoteSubtitle(itemId: UUID, subtitleId: String)

    /** Deletes the external subtitle stream [index] attached to [itemId]. */
    suspend fun deleteSubtitle(itemId: UUID, index: Int)

    /** Searches remote metadata providers for movies matching [name] (optionally [year]). */
    suspend fun searchRemoteMovies(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String> = emptyMap(),
    ): List<RemoteSearchResult>

    /** Searches remote metadata providers for series matching [name] (optionally [year]). */
    suspend fun searchRemoteSeries(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String> = emptyMap(),
    ): List<RemoteSearchResult>

    /** Applies the chosen remote search [result] to [itemId], replacing its metadata. */
    suspend fun applyRemoteSearchResult(
        itemId: UUID,
        result: RemoteSearchResult,
        replaceAllImages: Boolean = true,
    )

    /** Lists the images currently attached to [itemId], as reported by the server. */
    suspend fun getItemImages(itemId: UUID): List<ImageInfo>

    /**
     * Uploads [content] as an image of [type] for [itemId]. When [index] is given the image at that
     * index is replaced (or added, for multi-image types like backdrops); otherwise the type's
     * single image is replaced.
     */
    suspend fun uploadItemImage(
        itemId: UUID,
        type: ImageType,
        index: Int?,
        content: ByteArray,
        mediaType: String,
    )

    /** Deletes the image of [type] at [index] (or the type's only image when [index] is null). */
    suspend fun deleteItemImage(itemId: UUID, type: ImageType, index: Int?)

    /**
     * Moves the image of [type] from [oldIndex] to [newIndex]. The image at index 0 is the primary
     * image, so this is how a different backdrop is promoted.
     */
    suspend fun updateItemImageIndex(
        itemId: UUID,
        type: ImageType,
        oldIndex: Int,
        newIndex: Int,
    )

    /** Searches the server's remote image providers for images of [type] to use on [itemId]. */
    suspend fun getRemoteImages(itemId: UUID, type: ImageType): List<RemoteImageInfo>

    /** Asks the server to download the remote image at [imageUrl] and attach it as [type]. */
    suspend fun downloadRemoteImage(itemId: UUID, type: ImageType, imageUrl: String)

    suspend fun getDownloads(): List<FindroidItem>

    fun getUserId(): UUID
}
