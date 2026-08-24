package fm.libro.wearos.library

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.download.AudiobookDownloadWorker
import fm.libro.wearos.download.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val book: Audiobook? = null,
    val isDownloaded: Boolean = false,
    val coverLocalPath: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class BookDetailViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val db: AppDatabase,
        private val workManager: WorkManager,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val downloadManager = DownloadManager(context.applicationContext as Application)

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(book = bookCache.remove(isbn))
        observeDownloadedStatus()
        observeWorkManager()
    }

    private fun observeDownloadedStatus() {
        viewModelScope.launch {
            db.downloadedBookDao().getByIsbnFlow(isbn).collect { entity ->
                _uiState.value = _uiState.value.copy(
                    isDownloaded = entity != null,
                    coverLocalPath = entity?.coverLocalPath,
                )
            }
        }
    }

    private fun observeWorkManager() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(workName).collect { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@collect
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = true,
                            error = null,
                        )
                    }
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(
                            AudiobookDownloadWorker.KEY_PROGRESS, 0
                        )
                        _uiState.value = _uiState.value.copy(
                            isDownloading = true,
                            downloadProgress = progress,
                            error = null,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadProgress = 100,
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(
                            AudiobookDownloadWorker.KEY_ERROR
                        )
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = error ?: "Download failed",
                        )
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = "Download cancelled",
                        )
                    }
                }
            }
        }
    }

    fun startDownload() {
        val book = _uiState.value.book ?: return
        val manifest = book.manifest ?: return

        cacheBook(book)

        val inputData = AudiobookDownloadWorker.createInputData(isbn, manifest)
        val request = OneTimeWorkRequestBuilder<AudiobookDownloadWorker>()
            .setInputData(inputData)
            .build()

        workManager.beginUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request,
        ).enqueue()

        _uiState.value = _uiState.value.copy(
            isDownloading = true,
            downloadProgress = 0,
            error = null,
        )
    }

    fun deleteBook() {
        viewModelScope.launch {
            downloadManager.deleteBook(isbn)
        }
    }

    private val workName: String
        get() = "${AudiobookDownloadWorker.WORK_NAME_PREFIX}$isbn"

    companion object {
        private val bookCache = mutableMapOf<String, Audiobook>()

        fun cacheBook(book: Audiobook) {
            bookCache[book.isbn] = book
        }

        fun getCachedBook(isbn: String): Audiobook? = bookCache[isbn]
    }
}
