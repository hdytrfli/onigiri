package dev.hdytrfli.onigiri.repository

import android.content.Context
import dev.hdytrfli.onigiri.api.JellyfinApi
import dev.hdytrfli.onigiri.database.ServerDatabaseDao
import dev.hdytrfli.onigiri.models.FindroidBook
import dev.hdytrfli.onigiri.models.FindroidCollection
import dev.hdytrfli.onigiri.models.FindroidEpisode
import dev.hdytrfli.onigiri.models.FindroidGenre
import dev.hdytrfli.onigiri.models.FindroidImages
import dev.hdytrfli.onigiri.models.FindroidItem
import dev.hdytrfli.onigiri.models.FindroidMovie
import dev.hdytrfli.onigiri.models.FindroidMusicAlbum
import dev.hdytrfli.onigiri.models.FindroidMusicArtist
import dev.hdytrfli.onigiri.models.FindroidMusicSong
import dev.hdytrfli.onigiri.models.FindroidPerson
import dev.hdytrfli.onigiri.models.FindroidSeason
import dev.hdytrfli.onigiri.models.FindroidShow
import dev.hdytrfli.onigiri.models.FindroidSource
import dev.hdytrfli.onigiri.models.RecommendationCategory
import dev.hdytrfli.onigiri.models.SortBy
import dev.hdytrfli.onigiri.models.SortOrder
import dev.hdytrfli.onigiri.models.toFindroidBook
import dev.hdytrfli.onigiri.models.toFindroidEpisode
import dev.hdytrfli.onigiri.models.toFindroidMovie
import dev.hdytrfli.onigiri.models.toFindroidSeason
import dev.hdytrfli.onigiri.models.toFindroidShow
import dev.hdytrfli.onigiri.models.toFindroidSource
import dev.hdytrfli.onigiri.settings.domain.AppPreferences
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageInfo
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.RemoteImageInfo
import org.jellyfin.sdk.model.api.RemoteSearchResult
import org.jellyfin.sdk.model.api.RemoteSubtitleInfo
import org.jellyfin.sdk.model.api.SearchHint
import org.jellyfin.sdk.model.api.SessionInfoDto
import org.jellyfin.sdk.model.api.UserConfiguration
import org.jellyfin.sdk.model.api.UserPolicy

class JellyfinRepositoryOfflineImpl(
    private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {
    private val offlineGenreNames = mutableMapOf<UUID, String>()

    override suspend fun getPublicSystemInfo(): PublicSystemInfo {
        throw Exception("System info not available in offline mode")
    }

    override suspend fun getUserViews(): List<BaseItemDto> {
        return emptyList()
    }

    override suspend fun getMovie(itemId: UUID): FindroidMovie =
        withContext(Dispatchers.IO) {
            database.getMovie(itemId).toFindroidMovie(database, jellyfinApi.userId!!)
        }

    override suspend fun getShow(itemId: UUID): FindroidShow =
        withContext(Dispatchers.IO) {
            database.getShow(itemId).toFindroidShow(database, jellyfinApi.userId!!)
        }

    override suspend fun getSeason(itemId: UUID): FindroidSeason =
        withContext(Dispatchers.IO) {
            database.getSeason(itemId).toFindroidSeason(database, jellyfinApi.userId!!)
        }

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode =
        withContext(Dispatchers.IO) {
            database.getEpisode(itemId).toFindroidEpisode(database, jellyfinApi.userId!!)
        }

    override suspend fun getLibraries(): List<FindroidCollection> {
        return emptyList()
    }

    override suspend fun getItem(itemId: UUID): FindroidItem? {
        return withContext(Dispatchers.IO) {
            val userId = jellyfinApi.userId ?: return@withContext null
            runCatching {
                database.getMovie(itemId).toFindroidMovie(database, userId)
            }
                .getOrNull()
                ?: runCatching {
                    database.getShow(itemId).toFindroidShow(database, userId)
                }
                    .getOrNull()
                ?: runCatching {
                    database.getSeason(itemId).toFindroidSeason(database, userId)
                }
                    .getOrNull()
                ?: runCatching {
                    database.getEpisode(itemId).toFindroidEpisode(database, userId)
                }
                    .getOrNull()
                ?: database.getBook(itemId)?.toFindroidBook(database, userId)
        }
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
    ): List<FindroidItem> {
        val types = includeTypes?.toSet()
        val items =
            downloadedItems().filter { item ->
                if (types == null) {
                    true
                } else {
                    when (item) {
                        is FindroidMovie -> BaseItemKind.MOVIE in types
                        is FindroidShow -> BaseItemKind.SERIES in types
                        is FindroidEpisode -> BaseItemKind.EPISODE in types
                        is FindroidBook ->
                            if (item.isAudiobook) {
                                BaseItemKind.AUDIO_BOOK in types
                            } else {
                                BaseItemKind.BOOK in types
                            }
                        else -> false
                    }
                }
            }
        val genreNames = genreIds?.mapNotNull { offlineGenreNames[it] }.orEmpty()
        val filtered =
            if (genreNames.isEmpty()) {
                items
            } else {
                items.filter { item -> offlineGenres(item).any { genre -> genre in genreNames } }
            }
        val searched =
            if (searchTerm.isNullOrBlank()) {
                filtered
            } else {
                filtered.filter { it.name.contains(searchTerm, ignoreCase = true) }
            }
        val sorted =
            when (sortBy) {
                SortBy.RELEASE_DATE ->
                    searched.sortedWith(
                        compareBy(nullsLast()) { item ->
                            when (item) {
                                is FindroidMovie -> item.premiereDate
                                is FindroidEpisode -> item.premiereDate
                                is FindroidBook -> item.premiereDate
                                else -> null
                            }
                        }
                    )
                else -> searched.sortedBy { it.name.lowercase() }
            }
        val ordered = if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
        val from = startIndex ?: 0
        return ordered.drop(from).let { if (limit != null) it.take(limit) else it }
    }

    override suspend fun getGenres(includeTypes: List<BaseItemKind>): List<FindroidGenre> {
        val types = includeTypes.toSet()
        val names =
            downloadedItems()
                .filter { item ->
                    when (item) {
                        is FindroidMovie -> BaseItemKind.MOVIE in types
                        is FindroidShow -> BaseItemKind.SERIES in types
                        is FindroidBook ->
                            if (item.isAudiobook) {
                                BaseItemKind.AUDIO_BOOK in types
                            } else {
                                BaseItemKind.BOOK in types
                            }
                        else -> false
                    }
                }
                .flatMap(::offlineGenres)
                .distinct()
        return names.map { name ->
            val id = UUID.nameUUIDFromBytes(name.toByteArray())
            offlineGenreNames[id] = name
            FindroidGenre(id = id, name = name)
        }
    }

    private fun offlineGenres(item: FindroidItem): List<String> =
        when (item) {
            is FindroidMovie -> item.genres
            is FindroidShow -> item.genres
            is FindroidBook -> item.genres
            else -> emptyList()
        }

    override suspend fun getMovieRecommendations(parentId: UUID?): List<RecommendationCategory> =
        emptyList()

    override suspend fun getPerson(personId: UUID): FindroidPerson {
        // People are server metadata and are not cached locally; the person page shows the offline
        // notice instead, so return a safe placeholder rather than throwing.
        return FindroidPerson(
            id = personId,
            name = "",
            overview = "",
            images = FindroidImages(),
        )
    }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
    ): List<FindroidItem> {
        return emptyList()
    }

    override suspend fun getFavoriteItems(): List<FindroidItem> =
        downloadedItems().filter { it.favorite }

    override suspend fun getMusicArtists(): List<FindroidMusicArtist> = emptyList()

    override suspend fun getMusicAlbums(): List<FindroidMusicAlbum> = emptyList()

    override suspend fun getMusicSongs(): List<FindroidMusicSong> = emptyList()

    override suspend fun getMusicArtist(itemId: UUID): FindroidMusicArtist? = null

    override suspend fun getMusicAlbum(itemId: UUID): FindroidMusicAlbum? = null

    override suspend fun getMusicAlbums(artistId: UUID): List<FindroidMusicAlbum> = emptyList()

    override suspend fun getMusicSongs(albumId: UUID): List<FindroidMusicSong> = emptyList()

    override suspend fun getSearchItems(query: String): List<FindroidItem> =
        downloadedItems().filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun getSearchHints(query: String): List<SearchHint> = emptyList()

    override suspend fun getSuggestions(): List<FindroidItem> {
        val downloaded = downloadedItems()
        return downloaded.filter { it.favorite }.ifEmpty { downloaded }.take(18)
    }

    override suspend fun getResumeItems(): List<FindroidItem> =
        downloadedItems().filter { it.playbackPositionTicks > 0 }

    override suspend fun getLatestMedia(parentId: UUID): List<FindroidItem> {
        return emptyList()
    }

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<FindroidSeason> =
        withContext(Dispatchers.IO) {
            database.getSeasonsByShowId(seriesId).map {
                it.toFindroidSeason(database, jellyfinApi.userId!!)
            }
        }

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> {
        return withContext(Dispatchers.IO) {
            val serverId =
                appPreferences.getValue(appPreferences.currentServer)
                    ?: return@withContext emptyList()
            val userId = jellyfinApi.userId ?: return@withContext emptyList()
            val result = mutableListOf<FindroidEpisode>()
            val shows =
                database.getShowsByServerId(serverId).filter {
                    if (seriesId != null) it.id == seriesId else true
                }
            for (show in shows) {
                val episodes =
                    database.getEpisodesByShowId(show.id).map {
                        it.toFindroidEpisode(database, userId)
                    }
                val indexOfLastPlayed = episodes.indexOfLast { it.played }
                if (indexOfLastPlayed == -1) {
                    episodes.firstOrNull()?.let(result::add)
                } else {
                    episodes.getOrNull(indexOfLastPlayed + 1)?.let { result.add(it) }
                }
            }
            result.filter { it.playbackPositionTicks == 0L }
        }
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
            val items =
                database.getEpisodesBySeasonId(seasonId).map {
                    it.toFindroidEpisode(database, jellyfinApi.userId!!)
                }
            if (startItemId != null) return@withContext items.dropWhile { it.id != startItemId }
            items
        }

    override suspend fun getMediaSources(itemId: UUID, includePath: Boolean): List<FindroidSource> =
        withContext(Dispatchers.IO) {
            database.getSources(itemId).map { it.toFindroidSource(database) }
        }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String {
        // Offline playback resolves the downloaded file directly from the local source.
        return ""
    }

    override suspend fun getDownload(itemId: UUID): ByteArray = ByteArray(0)

    override suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val sources =
                    File(context.filesDir, "trickplay/$itemId").listFiles()
                        ?: return@withContext null
                File(sources.first(), index.toString()).readBytes()
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun postCapabilities() {}

    override suspend fun postPlaybackStart(itemId: UUID) {}

    override suspend fun postPlaybackStop(
        itemId: UUID,
        positionTicks: Long,
        playedPercentage: Int,
    ) {
        withContext(Dispatchers.IO) {
            when {
                playedPercentage < 10 -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
                    database.setPlayed(jellyfinApi.userId!!, itemId, false)
                }
                playedPercentage > 90 -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
                    database.setPlayed(jellyfinApi.userId!!, itemId, true)
                }
                else -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
                    database.setPlayed(jellyfinApi.userId!!, itemId, false)
                }
            }
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun postPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun getPlaybackSession(): SessionInfoDto? = null

    override suspend fun markAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setFavorite(jellyfinApi.userId!!, itemId, true)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun unmarkAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setFavorite(jellyfinApi.userId!!, itemId, false)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun markAsPlayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(jellyfinApi.userId!!, itemId, true)
            database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun markAsUnplayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(jellyfinApi.userId!!, itemId, false)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override fun getBaseUrl(): String {
        return ""
    }

    override suspend fun updateDeviceName(name: String) {
        // Nothing to report to the server while offline.
    }

    override suspend fun getUserConfiguration(): UserConfiguration? {
        return null
    }

    override suspend fun getUserPolicy(): UserPolicy? = null

    override suspend fun searchRemoteSubtitles(
        itemId: UUID,
        language: String,
    ): List<RemoteSubtitleInfo> = emptyList()

    override suspend fun downloadRemoteSubtitle(itemId: UUID, subtitleId: String) = Unit

    override suspend fun deleteSubtitle(itemId: UUID, index: Int) = Unit

    override suspend fun searchRemoteMovies(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String>,
    ): List<RemoteSearchResult> = emptyList()

    override suspend fun searchRemoteSeries(
        itemId: UUID,
        name: String,
        year: Int?,
        providerIds: Map<String, String>,
    ): List<RemoteSearchResult> = emptyList()

    override suspend fun applyRemoteSearchResult(
        itemId: UUID,
        result: RemoteSearchResult,
        replaceAllImages: Boolean,
    ) = Unit

    override suspend fun getItemImages(itemId: UUID): List<ImageInfo> = emptyList()

    override suspend fun uploadItemImage(
        itemId: UUID,
        type: ImageType,
        index: Int?,
        content: ByteArray,
        mediaType: String,
    ) = Unit

    override suspend fun deleteItemImage(itemId: UUID, type: ImageType, index: Int?) = Unit

    override suspend fun updateItemImageIndex(
        itemId: UUID,
        type: ImageType,
        oldIndex: Int,
        newIndex: Int,
    ) = Unit

    override suspend fun getRemoteImages(itemId: UUID, type: ImageType): List<RemoteImageInfo> =
        emptyList()

    override suspend fun downloadRemoteImage(itemId: UUID, type: ImageType, imageUrl: String) = Unit

    override suspend fun getDownloads(): List<FindroidItem> = downloadedItems()

    /** Every locally available item, regardless of its server-side type. */
    private suspend fun downloadedItems(): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            val serverId =
                appPreferences.getValue(appPreferences.currentServer)
                    ?: return@withContext emptyList()
            val userId = jellyfinApi.userId ?: return@withContext emptyList()
            buildList {
                addAll(
                    database.getMoviesByServerId(serverId).map {
                        it.toFindroidMovie(database, userId)
                    }
                )
                addAll(
                    database.getBooksByServerId(serverId).map {
                        it.toFindroidBook(database, userId)
                    }
                )
                addAll(
                    database.getShowsByServerId(serverId).map {
                        it.toFindroidShow(database, userId)
                    }
                )
                addAll(
                    database.getEpisodesByServerId(serverId).map {
                        it.toFindroidEpisode(database, userId)
                    }
                )
            }
        }

    override fun getUserId(): UUID {
        return jellyfinApi.userId!!
    }
}
