package dev.hdytrfli.onigiri.repository

import android.content.Context
import dev.hdytrfli.onigiri.api.JellyfinApi
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
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
import dev.hdytrfli.onigiri.models.RecommendationKind
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.models.toFindroidBook
import dev.hdytrfli.onigiri.models.toFindroidCollection
import dev.hdytrfli.onigiri.models.toFindroidEpisode
import dev.hdytrfli.onigiri.models.toFindroidItem
import dev.hdytrfli.onigiri.models.toFindroidMovie
import dev.hdytrfli.onigiri.models.toFindroidPerson
import dev.hdytrfli.onigiri.models.toFindroidSeason
import dev.hdytrfli.onigiri.models.toFindroidShow
import dev.hdytrfli.onigiri.models.toFindroidSource
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.FileInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceOptionsDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.MovieInfo
import org.jellyfin.sdk.model.api.MovieInfoRemoteSearchQuery
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.RecommendationType
import org.jellyfin.sdk.model.api.RemoteImageInfo
import org.jellyfin.sdk.model.api.RemoteSearchResult
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo
import org.jellyfin.sdk.model.api.RepeatMode
import org.jellyfin.sdk.model.api.SeriesInfo
import org.jellyfin.sdk.model.api.SeriesInfoRemoteSearchQuery
import org.jellyfin.sdk.model.api.SessionInfoDto
import org.jellyfin.sdk.model.api.SortOrder as ItemSortOrder
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.SubtitleProfile
import org.jellyfin.sdk.model.api.UserConfiguration
import org.jellyfin.sdk.model.api.UserPolicy
import org.jellyfin.sdk.model.api.request.GetGenresRequest
import timber.log.Timber

class JellyfinRepositoryImpl(
    private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {
    // Cached so opening a detail screen doesn't re-fetch the policy every time. Invalidated when
    // the signed-in user changes.
    private var cachedUserPolicy: UserPolicy? = null
    private var cachedUserPolicyUserId: UUID? = null

    override suspend fun getPublicSystemInfo(): PublicSystemInfo =
        withContext(Dispatchers.IO) { jellyfinApi.systemApi.getPublicSystemInfo().content }

    override suspend fun getUserViews(): List<BaseItemDto> =
        withContext(Dispatchers.IO) {
            jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!).content.items
        }

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi
                .getItem(itemId, jellyfinApi.userId!!)
                .content
                .toFindroidEpisode(this@JellyfinRepositoryImpl, database)
                ?: error("Episode $itemId could not be mapped from the server response")
        }

    override suspend fun getMovie(itemId: UUID): FindroidMovie =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi
                .getItem(itemId, jellyfinApi.userId!!)
                .content
                .toFindroidMovie(this@JellyfinRepositoryImpl, database)
        }

    override suspend fun getShow(itemId: UUID): FindroidShow =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi
                .getItem(itemId, jellyfinApi.userId!!)
                .content
                .toFindroidShow(this@JellyfinRepositoryImpl)
        }

    override suspend fun getSeason(itemId: UUID): FindroidSeason =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi
                .getItem(itemId, jellyfinApi.userId!!)
                .content
                .toFindroidSeason(this@JellyfinRepositoryImpl)
        }

    override suspend fun getLibraries(): List<FindroidCollection> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(jellyfinApi.userId!!).content.items.mapNotNull {
                it.toFindroidCollection(this@JellyfinRepositoryImpl)
            }
        }

    override suspend fun getItem(itemId: UUID): FindroidItem? =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi
                .getItems(
                    jellyfinApi.userId!!,
                    ids = listOf(itemId),
                    fields =
                        listOf(
                            ItemFields.OVERVIEW,
                            ItemFields.PEOPLE,
                            ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                            ItemFields.STUDIOS,
                        ),
                )
                .content
                .items
                .firstOrNull()
                ?.toFindroidItem(this@JellyfinRepositoryImpl, database)
        }

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
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi
                .getItems(
                    jellyfinApi.userId!!,
                    parentId = parentId,
                    includeItemTypes = includeTypes,
                    recursive = recursive,
                    sortBy = listOf(ItemSortBy.fromName(sortBy.sortString)),
                    sortOrder = listOf(ItemSortOrder.fromName(sortOrder.sortString)),
                    startIndex = startIndex,
                    limit = limit,
                    searchTerm = searchTerm,
                    genreIds = genreIds,
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getGenres(includeTypes: List<BaseItemKind>): List<FindroidGenre> =
        withContext(Dispatchers.IO) {
            jellyfinApi.genresApi
                .getGenres(
                    GetGenresRequest(
                        userId = jellyfinApi.userId,
                        includeItemTypes = includeTypes,
                    )
                )
                .content
                .items
                .map { FindroidGenre(id = it.id, name = it.name.orEmpty()) }
        }

    override suspend fun getPerson(personId: UUID): FindroidPerson =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi
                .getItem(personId, jellyfinApi.userId!!)
                .content
                .toFindroidPerson(this@JellyfinRepositoryImpl)
        }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
    ): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi
                .getItems(
                    jellyfinApi.userId!!,
                    personIds = personIds,
                    includeItemTypes = includeTypes,
                    recursive = recursive,
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getFavoriteItems(): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi
                .getItems(
                    jellyfinApi.userId!!,
                    filters = listOf(ItemFilter.IS_FAVORITE),
                    includeItemTypes =
                        listOf(
                            BaseItemKind.MOVIE,
                            BaseItemKind.SERIES,
                            BaseItemKind.SEASON,
                            BaseItemKind.EPISODE,
                            BaseItemKind.BOOK,
                            BaseItemKind.AUDIO_BOOK,
                        ),
                    recursive = true,
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getSearchItems(query: String): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi
                .getItems(
                    jellyfinApi.userId!!,
                    searchTerm = query,
                    includeItemTypes =
                        listOf(
                            BaseItemKind.MOVIE,
                            BaseItemKind.SERIES,
                            BaseItemKind.BOOK,
                            BaseItemKind.AUDIO_BOOK,
                        ),
                    recursive = true,
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getSuggestions(): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.suggestionsApi
                .getSuggestions(
                    jellyfinApi.userId!!,
                    limit = 12,
                    type = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getResumeItems(): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi
                .getResumeItems(
                    jellyfinApi.userId!!,
                    limit = 12,
                    includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
                )
                .content
                .items
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getLatestMedia(parentId: UUID): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi
                .getLatestMedia(jellyfinApi.userId!!, parentId = parentId, limit = 16)
                .content
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getMovieRecommendations(parentId: UUID?): List<RecommendationCategory> =
        withContext(Dispatchers.IO) {
            jellyfinApi.moviesApi
                .getMovieRecommendations(
                    jellyfinApi.userId!!,
                    parentId = parentId,
                    categoryLimit = 6,
                    itemLimit = 20,
                )
                .content
                .mapNotNull { dto ->
                    val items =
                        dto.items
                            ?.mapNotNull {
                                it.toFindroidItem(this@JellyfinRepositoryImpl, database)
                            }
                            .orEmpty()
                    if (items.isEmpty()) {
                        null
                    } else {
                        RecommendationCategory(
                            kind =
                                when (dto.recommendationType) {
                                    RecommendationType.SIMILAR_TO_RECENTLY_PLAYED ->
                                        RecommendationKind.BecauseYouWatched
                                    RecommendationType.SIMILAR_TO_LIKED_ITEM ->
                                        RecommendationKind.BecauseYouLike
                                    RecommendationType.HAS_DIRECTOR_FROM_RECENTLY_PLAYED,
                                    RecommendationType.HAS_LIKED_DIRECTOR ->
                                        RecommendationKind.DirectedBy
                                    RecommendationType.HAS_ACTOR_FROM_RECENTLY_PLAYED,
                                    RecommendationType.HAS_LIKED_ACTOR ->
                                        RecommendationKind.Starring
                                },
                            baselineName = dto.baselineItemName,
                            categoryId = dto.categoryId,
                            items = items,
                        )
                    }
                }
        }

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<FindroidSeason> =
        withContext(Dispatchers.IO) {
            if (!offline) {
                jellyfinApi.showsApi.getSeasons(seriesId, jellyfinApi.userId!!).content.items.map {
                    it.toFindroidSeason(this@JellyfinRepositoryImpl)
                }
            } else {
                database.getSeasonsByShowId(seriesId).map {
                    it.toFindroidSeason(database, jellyfinApi.userId!!)
                }
            }
        }

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi
                .getNextUp(
                    jellyfinApi.userId!!,
                    limit = 24,
                    seriesId = seriesId,
                    enableResumable = false,
                )
                .content
                .items
                .mapNotNull { it.toFindroidEpisode(this@JellyfinRepositoryImpl) }
        }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?,
        offline: Boolean,
    ): List<FindroidEpisode> =
        withContext(Dispatchers.IO) {
            if (!offline) {
                jellyfinApi.showsApi
                    .getEpisodes(
                        seriesId,
                        jellyfinApi.userId!!,
                        seasonId = seasonId,
                        fields = fields,
                        startItemId = startItemId,
                        limit = limit,
                    )
                    .content
                    .items
                    .mapNotNull { it.toFindroidEpisode(this@JellyfinRepositoryImpl, database) }
            } else {
                database.getEpisodesBySeasonId(seasonId).map {
                    it.toFindroidEpisode(database, jellyfinApi.userId!!)
                }
            }
        }

    override suspend fun getMediaSources(itemId: UUID, includePath: Boolean): List<FindroidSource> =
        withContext(Dispatchers.IO) {
            val sources = mutableListOf<FindroidSource>()
            sources.addAll(
                jellyfinApi.mediaInfoApi
                    .getPostedPlaybackInfo(
                        itemId,
                        PlaybackInfoDto(
                            userId = jellyfinApi.userId!!,
                            deviceProfile =
                                DeviceProfile(
                                    name = "Direct play all",
                                    maxStaticBitrate = 1_000_000_000,
                                    maxStreamingBitrate = 1_000_000_000,
                                    codecProfiles = emptyList(),
                                    containerProfiles = emptyList(),
                                    directPlayProfiles = emptyList(),
                                    transcodingProfiles = emptyList(),
                                    subtitleProfiles =
                                        listOf(
                                            SubtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL),
                                            SubtitleProfile("ass", SubtitleDeliveryMethod.EXTERNAL),
                                        ),
                                ),
                            maxStreamingBitrate = 1_000_000_000,
                        ),
                    )
                    .content
                    .mediaSources
                    .map { it.toFindroidSource(this@JellyfinRepositoryImpl, itemId, includePath) }
            )
            sources.addAll(database.getSources(itemId).map { it.toFindroidSource(database) })
            sources
        }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String =
        withContext(Dispatchers.IO) {
            try {
                jellyfinApi.videosApi.getVideoStreamUrl(
                    itemId,
                    static = true,
                    mediaSourceId = mediaSourceId,
                )
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
        }

    override suspend fun getDownload(itemId: UUID): ByteArray =
        withContext(Dispatchers.IO) { jellyfinApi.libraryApi.getDownload(itemId).content }

    override suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                try {
                    val sources = File(context.filesDir, "trickplay/$itemId").listFiles()
                    if (sources != null) {
                        return@withContext File(sources.first(), index.toString()).readBytes()
                    }
                } catch (_: Exception) {}

                return@withContext jellyfinApi.trickplayApi
                    .getTrickplayTileImage(itemId, width, index)
                    .content
            } catch (_: Exception) {
                return@withContext null
            }
        }

    override suspend fun postCapabilities() {
        Timber.d("Sending capabilities")
        withContext(Dispatchers.IO) {
            jellyfinApi.sessionApi.postCapabilities(
                playableMediaTypes = listOf(MediaType.VIDEO),
                supportedCommands =
                    listOf(
                        GeneralCommandType.VOLUME_UP,
                        GeneralCommandType.VOLUME_DOWN,
                        GeneralCommandType.TOGGLE_MUTE,
                        GeneralCommandType.SET_AUDIO_STREAM_INDEX,
                        GeneralCommandType.SET_SUBTITLE_STREAM_INDEX,
                        GeneralCommandType.MUTE,
                        GeneralCommandType.UNMUTE,
                        GeneralCommandType.SET_VOLUME,
                        GeneralCommandType.DISPLAY_MESSAGE,
                        GeneralCommandType.PLAY,
                        GeneralCommandType.PLAY_STATE,
                        GeneralCommandType.PLAY_NEXT,
                        GeneralCommandType.PLAY_MEDIA_SOURCE,
                    ),
                supportsMediaControl = true,
            )
        }
    }

    override suspend fun postPlaybackStart(itemId: UUID) {
        Timber.d("Sending start $itemId")
        withContext(Dispatchers.IO) {
            jellyfinApi.playStateApi.reportPlaybackStart(
                PlaybackStartInfo(
                    itemId = itemId,
                    canSeek = true,
                    isPaused = false,
                    isMuted = false,
                    playMethod = PlayMethod.DIRECT_PLAY,
                    repeatMode = RepeatMode.REPEAT_NONE,
                    playbackOrder = PlaybackOrder.DEFAULT,
                )
            )
        }
    }

    override suspend fun postPlaybackStop(
        itemId: UUID,
        positionTicks: Long,
        playedPercentage: Int,
    ) {
        Timber.d("Sending stop $itemId")
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId!!
            try {
                jellyfinApi.playStateApi.reportPlaybackStopped(
                    PlaybackStopInfo(itemId = itemId, positionTicks = positionTicks, failed = false)
                )
                updateLocalPlaybackStop(userId, itemId, positionTicks, playedPercentage, false)
            } catch (e: Exception) {
                Timber.e(e, "Failed to report playback stop for $itemId")
                updateLocalPlaybackStop(userId, itemId, positionTicks, playedPercentage, true)
            }
        }
    }

    private suspend fun updateLocalPlaybackStop(
        userId: UUID,
        itemId: UUID,
        positionTicks: Long,
        playedPercentage: Int,
        pendingSync: Boolean,
    ) {
        database.getUserDataOrCreateNew(itemId, userId)
        when {
            playedPercentage < 10 -> {
                database.setPlaybackPositionTicks(itemId, userId, 0)
                database.setPlayed(userId, itemId, false)
            }
            playedPercentage > 90 -> {
                database.setPlaybackPositionTicks(itemId, userId, 0)
                database.setPlayed(userId, itemId, true)
            }
            else -> {
                database.setPlaybackPositionTicks(itemId, userId, positionTicks)
                database.setPlayed(userId, itemId, false)
            }
        }
        if (pendingSync) {
            database.setUserDataToBeSynced(userId, itemId, true)
        }
    }

    override suspend fun postPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean,
    ) {
        Timber.d("Posting progress of $itemId, position: $positionTicks")
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId!!
            try {
                jellyfinApi.playStateApi.reportPlaybackProgress(
                    PlaybackProgressInfo(
                        itemId = itemId,
                        canSeek = true,
                        isPaused = isPaused,
                        isMuted = false,
                        playMethod = PlayMethod.DIRECT_PLAY,
                        repeatMode = RepeatMode.REPEAT_NONE,
                        playbackOrder = PlaybackOrder.DEFAULT,
                        positionTicks = positionTicks,
                    )
                )
                updateLocalPlaybackProgress(userId, itemId, positionTicks, false)
            } catch (e: Exception) {
                Timber.e(e, "Failed to report playback progress for $itemId")
                updateLocalPlaybackProgress(userId, itemId, positionTicks, true)
            }
        }
    }

    private suspend fun updateLocalPlaybackProgress(
        userId: UUID,
        itemId: UUID,
        positionTicks: Long,
        pendingSync: Boolean,
    ) {
        database.getUserDataOrCreateNew(itemId, userId)
        database.setPlaybackPositionTicks(itemId, userId, positionTicks)
        if (pendingSync) {
            database.setUserDataToBeSynced(userId, itemId, true)
        }
    }

    override suspend fun getPlaybackSession(): SessionInfoDto? =
        withContext(Dispatchers.IO) {
            // Without our own device id the query would answer with every session on the server,
            // including other users', so don't ask at all.
            val deviceId = jellyfinApi.jellyfin.deviceInfo?.id ?: return@withContext null
            jellyfinApi.sessionApi.getSessions(deviceId = deviceId).content.firstOrNull()
        }

    override suspend fun markAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId!!
            jellyfinApi.userLibraryApi.markFavoriteItem(itemId)
            database.getUserDataOrCreateNew(itemId, userId)
            database.setFavorite(userId, itemId, true)
        }
    }

    override suspend fun unmarkAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId!!
            jellyfinApi.userLibraryApi.unmarkFavoriteItem(itemId)
            database.getUserDataOrCreateNew(itemId, userId)
            database.setFavorite(userId, itemId, false)
        }
    }

    override suspend fun markAsPlayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId!!
            jellyfinApi.playStateApi.markPlayedItem(itemId)
            database.getUserDataOrCreateNew(itemId, userId)
            database.setPlayed(userId, itemId, true)
            database.setPlaybackPositionTicks(itemId, userId, 0)
        }
    }

    override suspend fun markAsUnplayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId!!
            jellyfinApi.playStateApi.markUnplayedItem(itemId)
            database.getUserDataOrCreateNew(itemId, userId)
            database.setPlayed(userId, itemId, false)
        }
    }

    override fun getBaseUrl() = jellyfinApi.api.baseUrl.orEmpty()

    override suspend fun updateDeviceName(name: String) {
        withContext(Dispatchers.IO) {
            jellyfinApi.jellyfin.deviceInfo?.id?.let { id ->
                jellyfinApi.devicesApi.updateDeviceOptions(
                    id,
                    DeviceOptionsDto(0, customName = name),
                )
            }
        }
    }

    override suspend fun getUserConfiguration(): UserConfiguration =
        withContext(Dispatchers.IO) { jellyfinApi.userApi.getCurrentUser().content.configuration!! }

    override suspend fun getUserPolicy(): UserPolicy? =
        withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId
            val cached = cachedUserPolicy
            if (cached != null && cachedUserPolicyUserId == userId) {
                return@withContext cached
            }
            jellyfinApi.userApi.getCurrentUser().content.policy?.also { policy ->
                cachedUserPolicy = policy
                cachedUserPolicyUserId = userId
            }
        }

    override suspend fun searchRemoteSubtitles(
        itemId: UUID,
        language: String,
    ): List<RemoteSubtitleInfo> =
        withContext(Dispatchers.IO) {
            jellyfinApi.subtitleApi
                .searchRemoteSubtitles(itemId = itemId, language = language)
                .content
        }

    override suspend fun downloadRemoteSubtitle(itemId: UUID, subtitleId: String) {
        withContext(Dispatchers.IO) {
            jellyfinApi.subtitleApi.downloadRemoteSubtitles(
                itemId = itemId,
                subtitleId = subtitleId,
            )
        }
    }

    override suspend fun deleteSubtitle(itemId: UUID, index: Int) {
        withContext(Dispatchers.IO) { jellyfinApi.subtitleApi.deleteSubtitle(itemId, index) }
    }

    override suspend fun searchRemoteMovies(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String>,
    ): List<RemoteSearchResult> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemLookupApi
                .getMovieRemoteSearchResults(
                    MovieInfoRemoteSearchQuery(
                        MovieInfo(
                            name,
                            null,
                            null,
                            null,
                            null,
                            providerIds,
                            year,
                            null,
                            null,
                            null,
                            false,
                        ),
                        itemId,
                        null,
                        false,
                    )
                )
                .content
        }

    override suspend fun searchRemoteSeries(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String>,
    ): List<RemoteSearchResult> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemLookupApi
                .getSeriesRemoteSearchResults(
                    SeriesInfoRemoteSearchQuery(
                        SeriesInfo(
                            name,
                            null,
                            null,
                            null,
                            null,
                            providerIds,
                            year,
                            null,
                            null,
                            null,
                            false,
                        ),
                        itemId,
                        null,
                        false,
                    )
                )
                .content
        }

    override suspend fun applyRemoteSearchResult(
        itemId: UUID,
        result: RemoteSearchResult,
        replaceAllImages: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            jellyfinApi.itemLookupApi.applySearchCriteria(itemId, replaceAllImages, result)
        }
    }

    override suspend fun getItemImages(itemId: UUID): List<ImageInfo> =
        withContext(Dispatchers.IO) { jellyfinApi.imageApi.getItemImageInfos(itemId).content }

    override suspend fun uploadItemImage(
        itemId: UUID,
        type: ImageType,
        index: Int?,
        content: ByteArray,
        mediaType: String,
    ) {
        withContext(Dispatchers.IO) {
            val fileInfo = FileInfo(content, mediaType)
            if (index != null) {
                jellyfinApi.imageApi.setItemImageByIndex(itemId, type, index, fileInfo)
            } else {
                jellyfinApi.imageApi.setItemImage(itemId, type, fileInfo)
            }
        }
    }

    override suspend fun deleteItemImage(itemId: UUID, type: ImageType, index: Int?) {
        withContext(Dispatchers.IO) { jellyfinApi.imageApi.deleteItemImage(itemId, type, index) }
    }

    override suspend fun updateItemImageIndex(
        itemId: UUID,
        type: ImageType,
        oldIndex: Int,
        newIndex: Int,
    ) {
        withContext(Dispatchers.IO) {
            jellyfinApi.imageApi.updateItemImageIndex(itemId, type, oldIndex, newIndex)
        }
    }

    override suspend fun getRemoteImages(itemId: UUID, type: ImageType): List<RemoteImageInfo> =
        withContext(Dispatchers.IO) {
            jellyfinApi.remoteImageApi
                .getRemoteImages(itemId = itemId, type = type)
                .content
                .images
                .orEmpty()
        }

    override suspend fun downloadRemoteImage(itemId: UUID, type: ImageType, imageUrl: String) {
        withContext(Dispatchers.IO) {
            jellyfinApi.remoteImageApi.downloadRemoteImage(
                itemId = itemId,
                type = type,
                imageUrl = imageUrl,
            )
        }
    }

    override suspend fun getDownloads(): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<FindroidItem>()
            items.addAll(
                database
                    .getMoviesByServerId(appPreferences.getValue(appPreferences.currentServer)!!)
                    .map { it.toFindroidMovie(database, jellyfinApi.userId!!) }
            )
            items.addAll(
                database
                    .getBooksByServerId(appPreferences.getValue(appPreferences.currentServer)!!)
                    .map { it.toFindroidBook(database, jellyfinApi.userId!!) }
            )
            items.addAll(
                database
                    .getShowsByServerId(appPreferences.getValue(appPreferences.currentServer)!!)
                    .map { it.toFindroidShow(database, jellyfinApi.userId!!) }
            )
            items
        }

    override fun getUserId(): UUID {
        return jellyfinApi.userId!!
    }
}
