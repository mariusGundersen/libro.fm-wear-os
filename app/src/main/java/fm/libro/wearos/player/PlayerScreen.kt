package fm.libro.wearos.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.media.ui.components.controls.SeekBackButton
import com.google.android.horologist.media.ui.components.controls.SeekForwardButton
import com.google.android.horologist.media.ui.components.controls.SeekToNextButton
import com.google.android.horologist.media.ui.components.controls.SeekToPreviousButton
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.components.background.ArtworkImageBackground
import com.google.android.horologist.media.ui.material3.screens.player.DefaultMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import fm.libro.wearos.R

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LibroMediaPlayerScreen(
    playerViewModel: LibroPlayerViewModel,
    volumeViewModel: VolumeViewModel,
    onChaptersClick: () -> Unit,
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
                trackPositionUiModel = playerUiState.trackPositionUiModel,
                leftButton = {
                    SeekBackButton(
                        modifier = Modifier.fillMaxSize(),
                        onClick = { playerUiController.seekBack() },
                        seekButtonIncrement = playerUiState.seekBackButtonIncrement,
                        enabled = playerUiState.seekBackEnabled,
                    )
                },
                rightButton = {
                    SeekForwardButton(
                        modifier = Modifier.fillMaxSize(),
                        onClick = { playerUiController.seekForward() },
                        seekButtonIncrement = playerUiState.seekForwardButtonIncrement,
                        enabled = playerUiState.seekForwardEnabled,
                    )
                }
            )
        },
        buttons = { playerUiState ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            )
                 {
                     SeekToPreviousButton(
                         onClick = { playerViewModel.playerUiController.skipToPreviousMedia() },
                         enabled = playerUiState.seekToPreviousEnabled,
                         modifier = Modifier.size(44.dp),
                         iconSize = 24.dp
                     )

                    Button(
                        onClick = onChaptersClick,
                        enabled = playerUiState.playPauseEnabled,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chapters),
                            contentDescription = "Chapters",
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    SeekToNextButton(
                        onClick = { playerViewModel.playerUiController.skipToNextMedia() },
                        enabled = playerUiState.seekToNextEnabled,
                        modifier = Modifier.size(44.dp),
                        iconSize = 24.dp
                    )
                }

        },
        background = { playerUiState ->
            ArtworkImageBackground((playerUiState.media as? MediaUiModel.Ready)?.artwork)
        },
    )
}
