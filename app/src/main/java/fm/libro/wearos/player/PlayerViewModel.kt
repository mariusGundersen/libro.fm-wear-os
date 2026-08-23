package fm.libro.wearos.player

import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.api.models.Audiobook
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibroPlayerViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepositoryImpl,
    ) : PlayerViewModel(playerRepository) {

    val playerState = playerRepository.player

    fun playAudiobook(audiobook: Audiobook, startIndex: Int = 0) {
        viewModelScope.launch {
            val mediaList = AudiobookMediaMapper.mapFromAudiobook(audiobook)
            if (mediaList.isNotEmpty()) {
                playerRepository.setMediaList(mediaList, startIndex)
                playerRepository.play()
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
            }
        }
    }
}
