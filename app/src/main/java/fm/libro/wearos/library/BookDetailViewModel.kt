package fm.libro.wearos.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fm.libro.wearos.api.LibroFmClient
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.DownloadManifest
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: Audiobook? = null,
    val manifest: DownloadManifest? = null,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class BookDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val authManager = AuthManager(application)
    private val db = AppDatabase.getInstance(application)
    private val downloadManager = DownloadManager(application)

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBook()
        observeDownload()
    }

    private fun observeDownload() {
        viewModelScope.launch {
            db.downloadedBookDao().getByIsbnFlow(isbn).collect { entity ->
                _uiState.value = _uiState.value.copy(isDownloaded = entity != null)
            }
        }
    }

    private fun loadBook() {
        val token = authManager.token ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                var page = 1
                var totalPages = 1
                var found: fm.libro.wearos.api.models.Audiobook? = null
                while (page <= totalPages) {
                    val response = LibroFmClient.getLibrary(token, page)
                    found = response.audiobooks.find { it.isbn == isbn }
                    if (found != null) break
                    totalPages = response.totalPages
                    page++
                }
                _uiState.value = _uiState.value.copy(book = found, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun fetchManifest() {
        val token = authManager.token ?: return
        viewModelScope.launch {
            try {
                val manifest = LibroFmClient.getDownloadManifest(token, isbn)
                _uiState.value = _uiState.value.copy(manifest = manifest)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun startDownload() {
        val book = _uiState.value.book ?: return
        val manifest = _uiState.value.manifest ?: return

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
}
