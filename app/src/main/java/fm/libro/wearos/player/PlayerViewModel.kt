package fm.libro.wearos.player

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.PlaybackProgressEntity
import fm.libro.wearos.data.StoredTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlayerUiState(
    val title: String = "",
    val author: String = "",
    val coverUrl: String? = null,
    val coverLocalPath: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
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

            val tracks = loadTracks(book.filePath, book.format, book.tracksJson)

            val totalDurationMs = tracks.sumOf { it.durationMs }

            _uiState.value = PlayerUiState(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                coverLocalPath = book.coverLocalPath,
                isLoading = false,
                tracks = tracks,
                durationMs = totalDurationMs,
                currentTrackIndex = progress?.trackIndex ?: 0,
                currentPositionMs = progress?.positionMs ?: 0,
            )
        }
    }

    private fun loadTracks(filePath: String, format: String, tracksJson: String): List<TrackInfo> {
        val storedTracks: List<StoredTrack> = try {
            val type = object : TypeToken<List<StoredTrack>>() {}.type
            Gson().fromJson(tracksJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val file = File(filePath)
        return when (format) {
            "m4b" -> {
                val track = storedTracks.firstOrNull()
                listOf(TrackInfo(
                    index = 0,
                    title = track?.chapterTitle ?: file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    durationMs = (track?.lengthSec ?: 0) * 1000L,
                ))
            }
            "mp3" -> {
                file.listFiles()
                    ?.filter { it.extension == "mp3" }
                    ?.sorted()
                    ?.mapIndexed { index, f ->
                        val stored = storedTracks.getOrNull(index)
                        TrackInfo(
                            index = index,
                            title = stored?.chapterTitle ?: f.nameWithoutExtension,
                            filePath = f.absolutePath,
                            durationMs = (stored?.lengthSec ?: 0) * 1000L,
                        )
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
                    playbackSpeed = 1.0f,
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
