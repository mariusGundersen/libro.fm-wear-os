package fm.libro.wearos.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.models.Audiobook
import fm.libro.wearos.player.AudiobookMediaMapper
import fm.libro.wearos.player.PlayerStateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class ChaptersUiState(
    val book: Audiobook? = null,
    val trackProgressMs: Map<Int, Long> = emptyMap(),
)

@HiltViewModel
class ChaptersViewModel
    @Inject
    constructor(
        private val playerRepository: PlayerRepositoryImpl,
        private val playerStateRepository: PlayerStateRepository,
        savedStateHandle: SavedStateHandle,
        private val db: AppDatabase,
    ) : ViewModel() {

    private val isbn: String = savedStateHandle["isbn"] ?: ""

    val uiState: StateFlow<ChaptersUiState> = combine(
        db.downloadedBookDao().getByIsbnWithProgress(isbn),
        db.trackProgressDao().getAllForIsbn(isbn),
    ) { entity, tracks ->
        ChaptersUiState(
            book = entity?.let { Audiobook.fromDownloaded(it) },
            trackProgressMs = tracks.associate { it.trackIndex to it.positionMs },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChaptersUiState())

    fun playTrack(index: Int) {
        viewModelScope.launch {
            val entity = db.downloadedBookDao().getByIsbn(isbn) ?: return@launch
            val mediaList = AudiobookMediaMapper.mapFromDownloadedBook(entity)

            if (mediaList.isNotEmpty()) {
                playerStateRepository.setLastPlayingIsbn(isbn)
                playerRepository.setMediaList(
                    mediaList,
                    index,
                    0.milliseconds
                )
                playerRepository.play()
            }
        }
    }
}
