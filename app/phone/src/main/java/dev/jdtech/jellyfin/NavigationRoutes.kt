package dev.jdtech.jellyfin

import androidx.navigation3.runtime.NavKey
import dev.jdtech.jellyfin.models.CollectionType
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute : NavKey

@Serializable data object WelcomeRoute : NavKey

@Serializable data object ServersRoute : NavKey

@Serializable data object AddServerRoute : NavKey

@Serializable data class ServerAddressesRoute(val serverId: String) : NavKey

@Serializable data object UsersRoute : NavKey

@Serializable data class LoginRoute(val username: String? = null) : NavKey

@Serializable data object HomeRoute : NavKey

@Serializable data object MoviesRoute : NavKey

@Serializable data object ShowsRoute : NavKey

@Serializable data object BooksRoute : NavKey

@Serializable
data class GenreListRoute(
    val genreId: String,
    val genreName: String,
    val movies: Boolean,
) : NavKey

@Serializable data object MediaRoute : NavKey

@Serializable data object DownloadsRoute : NavKey

@Serializable
data class LibraryRoute(
    val libraryId: String,
    val libraryName: String,
    val libraryType: CollectionType,
) : NavKey

@Serializable
data class CollectionRoute(val collectionId: String, val collectionName: String) : NavKey

@Serializable data object FavoritesRoute : NavKey

@Serializable data class MovieRoute(val movieId: String) : NavKey

@Serializable data class ShowRoute(val showId: String) : NavKey

@Serializable data class EpisodeRoute(val episodeId: String) : NavKey

@Serializable data class SeasonRoute(val seasonId: String) : NavKey

@Serializable data class PersonRoute(val personId: String, val isAuthor: Boolean = false) : NavKey

@Serializable
data class BookDetailRoute(val bookId: String, val audiobook: Boolean = false) : NavKey

@Serializable data class SubtitleSearchRoute(val itemId: String) : NavKey

@Serializable data class IdentifyRoute(val itemId: String, val series: Boolean = false) : NavKey

@Serializable data class EditImagesRoute(val itemId: String) : NavKey

@Serializable
data class EditImageSearchRoute(
    val itemId: String,
    val type: String,
    val isEpisode: Boolean = false,
) : NavKey

@Serializable
data class IdentifyResultsRoute(
    val itemId: String,
    val query: String,
    val year: String,
    val imdbId: String,
    val tmdbId: String,
    val tvdbId: String,
    val series: Boolean = false,
) : NavKey

@Serializable data class SettingsRoute(val indexes: List<Int>) : NavKey

@Serializable data class ProfilePickerRoute(val canGoBack: Boolean = true) : NavKey

@Serializable data object AddProfileRoute : NavKey

@Serializable data class SettingsFileEditRoute(val filePath: String) : NavKey

@Serializable data object AboutRoute : NavKey

@Serializable data object SubtitleFontRoute : NavKey

@Serializable data object ReaderFontRoute : NavKey
