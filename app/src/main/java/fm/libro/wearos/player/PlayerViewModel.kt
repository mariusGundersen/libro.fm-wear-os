package fm.libro.wearos.player

import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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

    init {
        restoreLastPlaying()
    }

    private fun restoreLastPlaying() {
        viewModelScope.launch {
            val isbn = playerStateRepository.lastPlayingIsbn.first() ?: return@launch
            val entity = db.downloadedBookDao().getByIsbnWithProgress(isbn).firstOrNull() ?: return@launch

            val mediaList = AudiobookMediaMapper.mapFromDownloadedBook(entity.book)
            if (mediaList.isEmpty()) return@launch

            playerRepository.connected.first { it }

            if (playerRepository.getMediaCount() > 0) return@launch

            playerRepository.setMediaList(
                mediaList,
                entity.progress?.trackIndex ?: 0,
                entity.progress?.positionMs?.milliseconds,
            )
        }
    }
}