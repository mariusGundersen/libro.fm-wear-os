package fm.libro.wearos.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: Audiobook? = null,
    val isDownloaded: Boolean = false,
    val coverLocalPath: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val error: String? = null,
)

class BookDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val db = AppDatabase.getInstance(application)
    private val downloadManager = DownloadManager(application)

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(book = bookCache.remove(isbn))
        observeDownload()
    }

    private fun observeDownload() {
        viewModelScope.launch {
            db.downloadedBookDao().getByIsbnFlow(isbn).collect { entity ->
                _uiState.value = _uiState.value.copy(
                    isDownloaded = entity != null,
                    coverLocalPath = entity?.coverLocalPath,
                    isDownloading = false,
                )
            }
        }
    }

    fun startDownload() {
        val book = _uiState.value.book ?: return
        val manifest = book.manifest ?: return

        _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = 0)

        viewModelScope.launch {
            try {
                downloadManager.downloadAudiobook(book, manifest) { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 100,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "Download failed: ${e.message}",
                )
            }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            downloadManager.deleteBook(isbn)
        }
    }

    companion object {
        private val bookCache = mutableMapOf<String, Audiobook>()

        fun cacheBook(book: Audiobook) {
            bookCache[book.isbn] = book
        }
    }
}
