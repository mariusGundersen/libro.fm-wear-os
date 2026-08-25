package fm.libro.wearos.player

import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.PlaybackProgressEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

            val progress = db.playbackProgressDao().getByIsbn(isbn)
            val mediaList = AudiobookMediaMapper.mapFromDownloadedBook(entity);
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
        val progress = captureCurrentProgress() ?: return
        runBlocking {
            db.playbackProgressDao().upsert(progress)
        }
    }

    private fun saveCurrentProgress() {
        val progress = captureCurrentProgress() ?: return
        viewModelScope.launch {
            db.playbackProgressDao().upsert(progress)
        }
    }

    private fun captureCurrentProgress(): PlaybackProgressEntity? {
        val player = playerState.value ?: return null
        val isbn = playerStateRepository.currentIsbn ?: return null
        if (player.mediaItemCount == 0) return null

        return try {
            val mediaItem = player.currentMediaItem ?: return null
            val trackIndex = player.currentMediaItemIndex
            val positionMs = player.currentPosition

            val trackIsbn = mediaItem.localConfiguration?.tag
                ?.toString()?.substringBefore("_")
                ?: isbn

            PlaybackProgressEntity(
                isbn = trackIsbn,
                trackIndex = trackIndex,
                positionMs = positionMs,
                playbackSpeed = 1.0f,
                updatedAt = System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            null
        }
    }
}
