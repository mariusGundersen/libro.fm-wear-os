package fm.libro.wearos.player

import androidx.lifecycle.viewModelScope
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.ui.state.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibroPlayerViewModel
    @Inject
    constructor(
        playerRepository: PlayerRepositoryImpl,
    ) : PlayerViewModel(playerRepository) {

    val playerState = playerRepository.player
}
