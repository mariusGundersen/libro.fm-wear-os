package fm.libro.wearos.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.horologist.media.model.Media
import com.google.android.horologist.media.ui.state.PlayerUiController
import com.google.android.horologist.media.ui.state.PlayerUiState
import com.google.android.horologist.media.ui.state.PlayerUiStateProducer
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.PlaybackProgressEntity
import fm.libro.wearos.data.StoredTrack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalHorologistApi::class)
class LibroPlayerViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val isbn: String = savedStateHandle["isbn"] ?: ""
    private val db = AppDatabase.getInstance(application)

    private val exoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    val playerRepository = PlayerRepositoryImpl(exoPlayer)

    private val producer = PlayerUiStateProducer(playerRepository)
    val playerUiState: StateFlow<PlayerUiState> =
        producer.playerUiStateFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = PlayerUiState.NotConnected,
        )

    val playerUiController = PlayerUiController(playerRepository)

    private var service: PlaybackService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as PlaybackService.LocalBinder
            service = localBinder.getService()
            bound = true
            service?.attachPlayer(exoPlayer)
            service?.startForegroundNotification()
            viewModelScope.launch {
                playerRepository.connect()
                loadBook()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            bound = false
        }
    }

    init {
        val intent = Intent(application, PlaybackService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = db.downloadedBookDao().getByIsbn(isbn) ?: return@launch
            val progress = db.playbackProgressDao().getByIsbn(isbn)
            val tracks = loadTracks(book.filePath, book.format, book.tracksJson)

            if (tracks.isEmpty()) return@launch

            val mediaList = tracks.map { track ->
                Media(
                    id = "${book.isbn}_${track.number}",
                    uri = "file://${track.filePath}",
                    title = track.chapterTitle ?: "Track ${track.number}",
                    artist = book.author,
                    artworkUri = book.coverLocalPath?.let { "file://$it" },
                )
            }

            val startIndex = progress?.trackIndex?.coerceIn(0, mediaList.size - 1) ?: 0

            playerRepository.setMediaList(mediaList, startIndex, progress?.positionMs?.milliseconds)
        }
    }

    private fun loadTracks(filePath: String, format: String, tracksJson: String): List<StoredTrack> {
        val storedTracks: List<StoredTrack> = try {
            val type = object : TypeToken<List<StoredTrack>>() {}.type
            Gson().fromJson(tracksJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val file = File(filePath)
        return when (format) {
            "m4b" -> storedTracks.ifEmpty {
                listOf(StoredTrack(number = 1, lengthSec = 0, chapterTitle = file.nameWithoutExtension, filePath))
            }
            "mp3" -> {
                file.listFiles()
                    ?.filter { it.extension == "mp3" }
                    ?.sorted()
                    ?.mapIndexed { index, f ->
                        val stored = storedTracks.getOrNull(index)
                        StoredTrack(
                            number = index + 1,
                            lengthSec = stored?.lengthSec ?: 0,
                            chapterTitle = stored?.chapterTitle ?: f.nameWithoutExtension,
                            filePath = stored?.filePath ?: f.absolutePath
                        )
                    } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    fun saveProgress() {
        viewModelScope.launch {
            val mediaIndex = playerRepository.getCurrentMediaIndex()
            val positionMs = exoPlayer.currentPosition.coerceAtLeast(0)

            db.playbackProgressDao().upsert(
                PlaybackProgressEntity(
                    isbn = isbn,
                    trackIndex = mediaIndex,
                    positionMs = positionMs,
                    playbackSpeed = exoPlayer.playbackParameters.speed,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        playerRepository.disconnect()
        exoPlayer.release()
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        service?.stopForegroundNotification()
        service = null
    }
}
