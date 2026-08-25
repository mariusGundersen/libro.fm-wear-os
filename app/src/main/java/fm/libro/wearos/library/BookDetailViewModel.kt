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
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.AudiobookInfo
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import fm.libro.wearos.download.AudiobookDownloadWorker
import fm.libro.wearos.download.DownloadManager
import fm.libro.wearos.player.AudiobookMediaMapper
import fm.libro.wearos.player.PlayerStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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
        private val playerRepository: PlayerRepositoryImpl,
        private val playerStateRepository: PlayerStateRepository,
        private val bookStore: BookStore,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val downloadManager = DownloadManager(context.applicationContext as Application)

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        bookStore.get(isbn)?.let { book ->
            _uiState.value = _uiState.value.copy(book = book)
        }
        observeDownloadedStatus()
        observeWorkManager()
    }

    private fun observeDownloadedStatus() {
        viewModelScope.launch {
            db.downloadedBookDao().getByIsbnFlow(isbn).collect { entity ->
                if (entity != null) {
                    _uiState.value = _uiState.value.copy(
                        book = entity.toAudiobook(),
                        isDownloaded = true,
                        coverLocalPath = entity.coverLocalPath,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDownloaded = false,
                    )
                }
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

        val inputData = AudiobookDownloadWorker.createInputData(
            isbn = isbn,
            title = book.title,
            author = book.authorString,
            coverUrl = book.coverUrl,
            durationSeconds = book.durationSeconds,
        )
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

    fun playBook(restart: Boolean = false) {
        viewModelScope.launch {
            val mediaList = if (_uiState.value.isDownloaded) {
                val entity = db.downloadedBookDao().getByIsbn(isbn) ?: return@launch
                AudiobookMediaMapper.mapFromDownloadedBook(entity)
            } else {
                return@launch
            }

            if (mediaList.isNotEmpty()) {
                val progress = if (restart) {
                    null
                } else {
                    db.playbackProgressDao().getByIsbn(isbn)
                }

                playerRepository.setMediaList(
                    mediaList,
                    progress?.trackIndex ?: 0,
                    progress?.positionMs?.milliseconds
                )
                playerRepository.play()
                playerStateRepository.setLastPlayingIsbn(isbn)
            }
        }
    }

    private val workName: String
        get() = "${AudiobookDownloadWorker.WORK_NAME_PREFIX}$isbn"

    private suspend fun DownloadedBookEntity.toAudiobook(): Audiobook {
        val progress = db.playbackProgressDao().getByIsbn(isbn)
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
            userMetadata = if (progress != null) {
                fm.libro.wearos.api.models.UserMetadata(
                    trackIndex = progress.trackIndex,
                    trackSeconds = progress.positionMs / 1000f,
                    finished = false,
                    addedAt = null,
                )
            } else {
                null
            },
        )
    }
}
