package fm.libro.wearos.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.components.background.ArtworkImageBackground
import com.google.android.horologist.media.ui.material3.screens.player.DefaultMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.model.MediaUiModel

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LibroMediaPlayerScreen(
    playerViewModel: LibroPlayerViewModel,
    volumeViewModel: VolumeViewModel,
) {
    PlayerScreen(
        playerViewModel = playerViewModel,
        volumeViewModel = volumeViewModel,
        mediaDisplay = { playerUiState ->
            DefaultMediaInfoDisplay(playerUiState = playerUiState)
        },
        controlButtons = { playerUiController, playerUiState ->
            AnimatedMediaControlButtons(
                onPlayButtonClick = { playerUiController.play() },
                onPauseButtonClick = { playerUiController.pause() },
                playPauseButtonEnabled = playerUiState.playPauseEnabled,
                playing = playerUiState.playing,
                onSeekToPreviousButtonClick = { playerUiController.skipToPreviousMedia() },
                onSeekToPreviousRepeatableClick = { playerUiController.seekBack() },
                seekToPreviousButtonEnabled = playerUiState.seekToPreviousEnabled,
                onSeekToNextButtonClick = { playerUiController.skipToNextMedia() },
                onSeekToNextRepeatableClick = { playerUiController.seekForward() },
                seekToNextButtonEnabled = playerUiState.seekToNextEnabled,
                trackPositionUiModel = playerUiState.trackPositionUiModel,
            )
        },
        buttons = { },
        background = { playerUiState ->
            ArtworkImageBackground((playerUiState.media as? MediaUiModel.Ready)?.artwork)
        },
    )
}
