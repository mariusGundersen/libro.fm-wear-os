package fm.libro.wearos.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PlaylistTrack(
    val index: Int,
    val title: String,
    val durationMs: Long,
    val isCurrent: Boolean,
)

@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepositoryImpl,
    ) : ViewModel() {

    val tracks: StateFlow<List<PlaylistTrack>> =
        playerRepository.player.map { player ->
            if (player == null) return@map emptyList()
            val currentIndex = player.currentMediaItemIndex
            val currentDurationMs = if (player.duration > 0) player.duration else 0L
            (0 until player.mediaItemCount).map { index ->
                val item = player.getMediaItemAt(index)
                PlaylistTrack(
                    index = index,
                    title = item.mediaMetadata.title?.toString() ?: "Track ${index + 1}",
                    durationMs = if (index == currentIndex) currentDurationMs else 0,
                    isCurrent = index == currentIndex,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playTrack(index: Int) {
        val player = playerRepository.player.value ?: return
        player.pause()
        player.seekTo(index, 0L)
        player.prepare()
        player.play()
    }
}
