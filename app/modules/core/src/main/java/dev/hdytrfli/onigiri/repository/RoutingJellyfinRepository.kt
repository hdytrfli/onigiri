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
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
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

/**
 * Routes every repository call through the current online/offline implementation at call time. This
 * is what lets the offline toggle take effect without restarting the activity.
 */
class RoutingJellyfinRepository(
    private val online: JellyfinRepositoryImpl,
    private val offline: JellyfinRepositoryOfflineImpl,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {
    private fun current(): JellyfinRepository =
        if (appPreferences.getValue(appPreferences.offlineMode)) offline else online

    override suspend fun getPublicSystemInfo(): PublicSystemInfo = current().getPublicSystemInfo()

    override suspend fun getUserViews(): List<BaseItemDto> = current().getUserViews()

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode = current().getEpisode(itemId)

    override suspend fun getMovie(itemId: UUID): FindroidMovie = current().getMovie(itemId)

    override suspend fun getShow(itemId: UUID): FindroidShow = current().getShow(itemId)

    override suspend fun getSeason(itemId: UUID): FindroidSeason = current().getSeason(itemId)

    override suspend fun getLibraries(): List<FindroidCollection> = current().getLibraries()

    override suspend fun getItem(itemId: UUID): FindroidItem? = current().getItem(itemId)

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
        startIndex: Int?,
        limit: Int?,
        searchTerm: String?,
        genreIds: List<UUID>?,
    ): List<FindroidItem> =
        current()
            .getItems(
                parentId = parentId,
                includeTypes = includeTypes,
                recursive = recursive,
                sortBy = sortBy,
                sortOrder = sortOrder,
                startIndex = startIndex,
                limit = limit,
                searchTerm = searchTerm,
                genreIds = genreIds,
            )

    override suspend fun getGenres(includeTypes: List<BaseItemKind>): List<FindroidGenre> =
        current().getGenres(includeTypes)

    override suspend fun getPerson(personId: UUID): FindroidPerson = current().getPerson(personId)

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
    ): List<FindroidItem> =
        current()
            .getPersonItems(
                personIds = personIds,
                includeTypes = includeTypes,
                recursive = recursive,
            )

    override suspend fun getFavoriteItems(): List<FindroidItem> = current().getFavoriteItems()

    override suspend fun getSearchItems(query: String): List<FindroidItem> =
        current().getSearchItems(query)

    override suspend fun getSuggestions(): List<FindroidItem> = current().getSuggestions()

    override suspend fun getResumeItems(): List<FindroidItem> = current().getResumeItems()

    override suspend fun getLatestMedia(parentId: UUID): List<FindroidItem> =
        current().getLatestMedia(parentId)

    override suspend fun getMovieRecommendations(parentId: UUID?): List<RecommendationCategory> =
        current().getMovieRecommendations(parentId)

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<FindroidSeason> =
        current().getSeasons(seriesId = seriesId, offline = offline)

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> =
        current().getNextUp(seriesId)

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?,
        offline: Boolean,
    ): List<FindroidEpisode> =
        current()
            .getEpisodes(
                seriesId = seriesId,
                seasonId = seasonId,
                fields = fields,
                startItemId = startItemId,
                limit = limit,
                offline = offline,
            )

    override suspend fun getMediaSources(
        itemId: UUID,
        includePath: Boolean,
    ): List<FindroidSource> = current().getMediaSources(itemId, includePath)

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String =
        current().getStreamUrl(itemId, mediaSourceId)

    override suspend fun getDownload(itemId: UUID): ByteArray = current().getDownload(itemId)

    override suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray? =
        current().getTrickplayData(itemId, width, index)

    override suspend fun postCapabilities() = current().postCapabilities()

    override suspend fun postPlaybackStart(itemId: UUID) = current().postPlaybackStart(itemId)

    override suspend fun postPlaybackStop(
        itemId: UUID,
        positionTicks: Long,
        playedPercentage: Int,
    ) = current().postPlaybackStop(itemId, positionTicks, playedPercentage)

    override suspend fun postPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean,
    ) = current().postPlaybackProgress(itemId, positionTicks, isPaused)

    override suspend fun getPlaybackSession(): SessionInfoDto? = current().getPlaybackSession()

    override suspend fun markAsFavorite(itemId: UUID) = current().markAsFavorite(itemId)

    override suspend fun unmarkAsFavorite(itemId: UUID) = current().unmarkAsFavorite(itemId)

    override suspend fun markAsPlayed(itemId: UUID) = current().markAsPlayed(itemId)

    override suspend fun markAsUnplayed(itemId: UUID) = current().markAsUnplayed(itemId)

    override fun getBaseUrl(): String = current().getBaseUrl()

    override suspend fun updateDeviceName(name: String) = current().updateDeviceName(name)

    override suspend fun getUserConfiguration(): UserConfiguration? =
        current().getUserConfiguration()

    override suspend fun getUserPolicy(): UserPolicy? = current().getUserPolicy()

    override suspend fun searchRemoteSubtitles(
        itemId: UUID,
        language: String,
    ): List<RemoteSubtitleInfo> = current().searchRemoteSubtitles(itemId, language)

    override suspend fun downloadRemoteSubtitle(itemId: UUID, subtitleId: String) =
        current().downloadRemoteSubtitle(itemId, subtitleId)

    override suspend fun deleteSubtitle(itemId: UUID, index: Int) =
        current().deleteSubtitle(itemId, index)

    override suspend fun searchRemoteMovies(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String>,
    ): List<RemoteSearchResult> = current().searchRemoteMovies(itemId, name, year, providerIds)

    override suspend fun searchRemoteSeries(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String>,
    ): List<RemoteSearchResult> = current().searchRemoteSeries(itemId, name, year, providerIds)

    override suspend fun applyRemoteSearchResult(
        itemId: UUID,
        result: RemoteSearchResult,
        replaceAllImages: Boolean,
    ) = current().applyRemoteSearchResult(itemId, result, replaceAllImages)

    override suspend fun getItemImages(itemId: UUID): List<ImageInfo> =
        current().getItemImages(itemId)

    override suspend fun uploadItemImage(
        itemId: UUID,
        type: ImageType,
        index: Int?,
        content: ByteArray,
        mediaType: String,
    ) = current().uploadItemImage(itemId, type, index, content, mediaType)

    override suspend fun deleteItemImage(itemId: UUID, type: ImageType, index: Int?) =
        current().deleteItemImage(itemId, type, index)

    override suspend fun updateItemImageIndex(
        itemId: UUID,
        type: ImageType,
        oldIndex: Int,
        newIndex: Int,
    ) = current().updateItemImageIndex(itemId, type, oldIndex, newIndex)

    override suspend fun getRemoteImages(
        itemId: UUID,
        type: ImageType,
    ): List<RemoteImageInfo> = current().getRemoteImages(itemId, type)

    override suspend fun downloadRemoteImage(
        itemId: UUID,
        type: ImageType,
        imageUrl: String,
    ) = current().downloadRemoteImage(itemId, type, imageUrl)

    override suspend fun getDownloads(): List<FindroidItem> = current().getDownloads()

    override fun getUserId(): UUID = current().getUserId()
}
