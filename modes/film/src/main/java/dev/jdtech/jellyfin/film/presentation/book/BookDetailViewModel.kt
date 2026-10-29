package dev.jdtech.jellyfin.film.presentation.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidBook
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookDetailState(
    val item: FindroidBook? = null,
    val isLoading: Boolean = true,
    val error: Exception? = null,
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(BookDetailState())
    val state = _state.asStateFlow()

    private var loadedId: UUID? = null

    fun load(bookId: UUID) {
        if (loadedId == bookId) return
        loadedId = bookId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = withContext(Dispatchers.IO) { runCatching { repository.getItem(bookId) } }
            _state.value =
                result.fold(
                    onSuccess = { item ->
                        BookDetailState(
                            item = item as? FindroidBook,
                            isLoading = false,
                            error = if (item is FindroidBook) null else Exception("Not a book"),
                        )
                    },
                    onFailure = { BookDetailState(isLoading = false, error = it as? Exception) },
                )
        }
    }
}
