package fm.libro.wearos.complication

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DataUpdates(
    private val updater: ComplicationDataSourceUpdateRequester,
) {
    data class State(
        val mediaItem: MediaItem?,
    )

    val listener: Player.Listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _stateFlow.update {
                it.copy(mediaItem = mediaItem)
            }
            updater.requestUpdateAll()
        }
    }

    private val _stateFlow = MutableStateFlow(State(null))
    val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()
}
