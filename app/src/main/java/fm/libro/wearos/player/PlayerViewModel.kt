package fm.libro.wearos.player

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.PlaybackProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlayerUiState(
    val title: String = "",
    val author: String = "",
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = true,
    val tracks: List<TrackInfo> = emptyList(),
    val currentTrackIndex: Int = 0,
)

data class TrackInfo(
    val index: Int,
    val title: String,
    val filePath: String,
    val durationMs: Long,
)

class PlayerViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val db = AppDatabase.getInstance(application)
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = db.downloadedBookDao().getByIsbn(isbn) ?: return@launch
            val progress = db.playbackProgressDao().getByIsbn(isbn)

            val tracks = loadTracks(book.filePath, book.format)

            _uiState.value = PlayerUiState(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                isLoading = false,
                tracks = tracks,
                currentTrackIndex = progress?.trackIndex ?: 0,
                currentPositionMs = progress?.positionMs ?: 0,
                playbackSpeed = progress?.playbackSpeed ?: 1.0f,
            )
        }
    }

    private fun loadTracks(filePath: String, format: String): List<TrackInfo> {
        val file = File(filePath)
        return when (format) {
            "m4b" -> {
                listOf(TrackInfo(0, file.nameWithoutExtension, file.absolutePath, 0))
            }
            "mp3" -> {
                file.listFiles()
                    ?.filter { it.extension == "mp3" }
                    ?.sorted()
                    ?.mapIndexed { index, f ->
                        TrackInfo(index, f.nameWithoutExtension, f.absolutePath, 0)
                    } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    fun play() {
        startPlaybackService(play = true)
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun pause() {
        startPlaybackService(play = false)
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun seekTo(positionMs: Long) {
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun skipForward() {
        val newPos = _uiState.value.currentPositionMs + 30_000
        seekTo(minOf(newPos, _uiState.value.durationMs))
    }

    fun skipBackward() {
        val newPos = _uiState.value.currentPositionMs - 30_000
        seekTo(maxOf(newPos, 0))
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun nextTrack() {
        val state = _uiState.value
        if (state.currentTrackIndex < state.tracks.size - 1) {
            _uiState.value = state.copy(
                currentTrackIndex = state.currentTrackIndex + 1,
                currentPositionMs = 0,
            )
        }
    }

    fun previousTrack() {
        val state = _uiState.value
        if (state.currentTrackIndex > 0) {
            _uiState.value = state.copy(
                currentTrackIndex = state.currentTrackIndex - 1,
                currentPositionMs = 0,
            )
        }
    }

    private fun startPlaybackService(play: Boolean) {
        val context = getApplication<Application>()
        val state = _uiState.value
        val track = state.tracks.getOrNull(state.currentTrackIndex) ?: return

        val intent = Intent(context, PlaybackService::class.java).apply {
            action = if (play) PlaybackService.ACTION_PLAY else PlaybackService.ACTION_PAUSE
            putExtra(PlaybackService.EXTRA_FILE_PATH, track.filePath)
            putExtra(PlaybackService.EXTRA_POSITION_MS, state.currentPositionMs)
            putExtra(PlaybackService.EXTRA_SPEED, state.playbackSpeed)
            putExtra(PlaybackService.EXTRA_ISBN, isbn)
        }
        context.startForegroundService(intent)
    }

    fun saveProgress() {
        viewModelScope.launch {
            val state = _uiState.value
            db.playbackProgressDao().upsert(
                PlaybackProgressEntity(
                    isbn = isbn,
                    trackIndex = state.currentTrackIndex,
                    positionMs = state.currentPositionMs,
                    playbackSpeed = state.playbackSpeed,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
    }
}
