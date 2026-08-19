package fm.libro.wearos.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.AudiobookInfo
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import fm.libro.wearos.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadedBookDetailUiState(
    val book: Audiobook? = null,
    val coverLocalPath: String? = null,
    val error: String? = null,
)

class DownloadedBookDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val db = AppDatabase.getInstance(application)
    private val downloadManager = DownloadManager(application)

    private val _uiState = MutableStateFlow(DownloadedBookDetailUiState())
    val uiState: StateFlow<DownloadedBookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBook()
        observeDownload()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val entity = db.downloadedBookDao().getByIsbn(isbn) ?: return@launch
            _uiState.value = _uiState.value.copy(
                book = entity.toAudiobook(),
                coverLocalPath = entity.coverLocalPath,
            )
        }
    }

    private fun observeDownload() {
        viewModelScope.launch {
            db.downloadedBookDao().getByIsbnFlow(isbn).collect { entity ->
                _uiState.value = _uiState.value.copy(
                    coverLocalPath = entity?.coverLocalPath,
                )
            }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            downloadManager.deleteBook(isbn)
        }
    }

    private fun DownloadedBookEntity.toAudiobook(): Audiobook {
        return Audiobook(
            isbn = isbn,
            title = title,
            authors = author,
            coverUrl = coverUrl,
            audiobookInfo = AudiobookInfo(
                narrators = null,
                duration = durationSeconds,
                sizeBytes = fileSizeBytes,
                trackCount = trackCount,
                partsCount = null,
                audioLanguage = null,
            ),
            series = null,
            seriesNum = null,
            publisher = null,
            publicationDate = null,
            description = null,
            userMetadata = null,
        )
    }
}
