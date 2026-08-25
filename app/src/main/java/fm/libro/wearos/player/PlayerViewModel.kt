package fm.libro.wearos.player

import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.PlaybackProgressEntity
import fm.libro.wearos.data.StoredTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class LibroPlayerViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepositoryImpl,
        private val playerStateRepository: PlayerStateRepository,
        private val db: AppDatabase,
    ) : PlayerViewModel(playerRepository) {

    private val gson = Gson()

    val playerState = playerRepository.player

    init {
        restoreLastPlaying()
        startPeriodicProgressSave()
    }

    private fun restoreLastPlaying() {
        viewModelScope.launch {
            val isbn = playerStateRepository.lastPlayingIsbn.first() ?: return@launch
            playerStateRepository.currentIsbn = isbn
            val entity = db.downloadedBookDao().getByIsbn(isbn) ?: return@launch
            val tracks: List<StoredTrack> = gson.fromJson(
                entity.tracksJson,
                object : TypeToken<List<StoredTrack>>() {}.type,
            )
            if (tracks.isEmpty()) return@launch

            val progress = db.playbackProgressDao().getByIsbn(isbn)
            val mediaList = AudiobookMediaMapper.mapFromStoredTracks(
                isbn = isbn,
                title = entity.title,
                artist = entity.author,
                coverUrl = entity.coverUrl,
                tracks = tracks,
            )
            if (mediaList.isNotEmpty()) {
                playerRepository.setMediaList(
                    mediaList,
                    progress?.trackIndex ?: 0,
                    progress?.positionMs?.milliseconds,
                )
            }
        }
    }

    private fun startPeriodicProgressSave() {
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                saveCurrentProgress()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgressSync()
    }

    private fun saveCurrentProgress() {
        viewModelScope.launch {
            saveCurrentProgressSync()
        }
    }

    private fun saveCurrentProgressSync() {
        val player = playerState.value ?: return
        val isbn = playerStateRepository.currentIsbn ?: return
        if (player.mediaItemCount == 0) return

        val mediaItem = player.currentMediaItem ?: return
        val trackIndex = player.currentMediaItemIndex
        val positionMs = player.currentPosition

        val trackIsbn = mediaItem.localConfiguration?.tag
            ?.toString()?.substringBefore("_")
            ?: isbn

        viewModelScope.launch {
            db.playbackProgressDao().upsert(
                PlaybackProgressEntity(
                    isbn = trackIsbn,
                    trackIndex = trackIndex,
                    positionMs = positionMs,
                    playbackSpeed = 1.0f,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun playAudiobook(audiobook: Audiobook, startIndex: Int = 0) {
        viewModelScope.launch {
            val mediaList = AudiobookMediaMapper.mapFromAudiobook(audiobook)
            if (mediaList.isNotEmpty()) {
                playerRepository.setMediaList(mediaList, startIndex)
                playerRepository.play()
                playerStateRepository.setLastPlayingIsbn(audiobook.isbn)
            }
        }
    }

    fun playFromStoredTracks(
        isbn: String,
        title: String,
        artist: String,
        coverUrl: String?,
        tracks: List<fm.libro.wearos.data.StoredTrack>,
        startIndex: Int = 0,
    ) {
        viewModelScope.launch {
            val mediaList = AudiobookMediaMapper.mapFromStoredTracks(
                isbn = isbn,
                title = title,
                artist = artist,
                coverUrl = coverUrl,
                tracks = tracks,
            )
            if (mediaList.isNotEmpty()) {
                playerRepository.setMediaList(mediaList, startIndex)
                playerRepository.play()
                playerStateRepository.setLastPlayingIsbn(isbn)
            }
        }
    }
}
