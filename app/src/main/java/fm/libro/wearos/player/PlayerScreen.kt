package fm.libro.wearos.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.media3.common.util.UnstableApi
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults.ConfirmButton
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.components.PodcastControlButtons
import com.google.android.horologist.media.ui.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.components.background.ArtworkImageBackground
import com.google.android.horologist.media.ui.material3.screens.player.DefaultMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.model.MediaUiModel
import fm.libro.wearos.R

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LibroPlayerScreen(
    viewModel: LibroPlayerViewModel,
    onBack: () -> Unit) {
    val playerUiState by viewModel.playerUiState.collectAsState()
    val playerUiController = viewModel.playerUiController


    var showDialog by remember { mutableStateOf(false) }

    PlayerScreen(
        mediaDisplay = {
            DefaultMediaInfoDisplay(playerUiState = playerUiState)
        },
        controlButtons = {
            AnimatedMediaControlButtons(
                onPlayButtonClick = { playerUiController.play() },
                onPauseButtonClick = { playerUiController.pause() },
                playPauseButtonEnabled = playerUiState.playPauseEnabled,
                playing = playerUiState.playing,
                onSeekToPreviousButtonClick = { playerUiController.skipToPreviousMedia() },
                onSeekToPreviousLongRepeatableClick = { playerUiController.seekBack() },
                seekToPreviousButtonEnabled = playerUiState.seekToPreviousEnabled,
                onSeekToNextButtonClick = { playerUiController.skipToNextMedia() },
                onSeekToNextLongRepeatableClick = { playerUiController.seekForward() },
                seekToNextButtonEnabled = playerUiState.seekToNextEnabled,
                trackPositionUiModel = playerUiState.trackPositionUiModel,

                )
        },
        buttons = {
            OutlinedButton(
                onClick = {
                    showDialog = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete",
                )
            }
        },
        background = {
            ArtworkImageBackground((playerUiState.media as? MediaUiModel.Ready)?.artwork)
        }
    )

    AlertDialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
        title = { Text("Delete book") },
        text = { Text("Are you sure?") },
        icon = {Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = "Delete",
        )},
        confirmButton = {
            ConfirmButton(
                onClick = {
                    viewModel.deleteBook()
                    onBack()
                }
            )
        }

    ) {

    }
}
