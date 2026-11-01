package dev.jdtech.jellyfin.film.presentation.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.swr.SwrStore
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class BookDetailState(
    val item: FindroidBook? = null,
    val isLoading: Boolean = true,
    val error: Exception? = null,
    /** 0..1 progression stored by the in-app reader. */
    val readingProgress: Float = 0f,
    /** Approximate pages left, when the reader has recorded a page count. */
    val pagesLeft: Int = 0,
)

@HiltViewModel
class BookDetailViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val swrStore: SwrStore,
) : ViewModel() {
    private val _state = MutableStateFlow(BookDetailState())
    val state = _state.asStateFlow()

    private var loadedId: UUID? = null

    fun load(bookId: UUID, force: Boolean = false) {
        if (!force && loadedId == bookId) return
        loadedId = bookId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.item == null, error = null)
            val result = withContext(Dispatchers.IO) { runCatching { repository.getItem(bookId) } }
            val book = result.getOrNull() as? FindroidBook
            _state.value =
                if (book != null) {
                    val progress = readerProgress(bookId)
                    BookDetailState(
                        item = book,
                        isLoading = false,
                        readingProgress = progress,
                        pagesLeft = readerPagesLeft(bookId, progress),
                    )
                } else {
                    BookDetailState(
                        isLoading = false,
                        error = result.exceptionOrNull() as? Exception ?: Exception("Not a book"),
                    )
                }
        }
    }

    fun setPlayed(bookId: UUID, played: Boolean) {
        if (played) {
            appPreferences.sharedPreferences
                .edit()
                .remove("reader_locator_$bookId")
                .remove("reader_page_count_$bookId")
                .apply()
            _state.value = _state.value.copy(readingProgress = 0f, pagesLeft = 0)
        }
        _state.value = _state.value.copy(item = _state.value.item?.copy(played = played))
        update(bookId) {
            if (played) repository.markAsPlayed(bookId) else repository.markAsUnplayed(bookId)
        }
    }

    fun setFavorite(bookId: UUID, favorite: Boolean) {
        _state.value = _state.value.copy(item = _state.value.item?.copy(favorite = favorite))
        update(bookId) {
            if (favorite) repository.markAsFavorite(bookId) else repository.unmarkAsFavorite(bookId)
        }
    }

    private fun update(bookId: UUID, block: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { block() } }
            swrStore.invalidateMediaLists()
            load(bookId, force = true)
        }
    }

    private fun readerPagesLeft(bookId: UUID, progress: Float): Int {
        if (progress <= 0f) return 0
        val total = appPreferences.sharedPreferences.getInt("reader_page_count_$bookId", 0)
        if (total <= 0) return 0
        return ceil((1f - progress) * total).toInt().coerceAtLeast(1)
    }

    private fun readerProgress(bookId: UUID): Float {
        val json =
            appPreferences.sharedPreferences.getString("reader_locator_$bookId", null) ?: return 0f
        return runCatching {
                JSONObject(json)
                    .optJSONObject("locations")
                    ?.optDouble("totalProgression", 0.0)
                    ?.toFloat() ?: 0f
            }
            .getOrDefault(0f)
            .coerceIn(0f, 1f)
    }
}
