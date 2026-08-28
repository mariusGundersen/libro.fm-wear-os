package fm.libro.wearos.player

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.components.background.ArtworkImageBackground
import com.google.android.horologist.media.ui.material3.screens.player.DefaultMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import fm.libro.wearos.R
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LibroMediaPlayerScreen(
    playerViewModel: LibroPlayerViewModel,
    volumeViewModel: VolumeViewModel,
    onPlaylistClick: () -> Unit,
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
        buttons = {
            Button(
                onClick = onPlaylistClick,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_queue_music_24),
                    contentDescription = "Playlist",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        background = { playerUiState ->
            ArtworkImageBackground((playerUiState.media as? MediaUiModel.Ready)?.artwork)
        },
    )
}
