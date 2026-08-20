package fm.libro.wearos.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.components.PodcastControlButtons
import com.google.android.horologist.media.ui.material3.components.background.ArtworkImageBackground
import com.google.android.horologist.media.ui.material3.screens.player.DefaultMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.model.MediaUiModel

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LibroPlayerScreen(viewModel: LibroPlayerViewModel) {
    val playerUiState by viewModel.playerUiState.collectAsState()
    val playerUiController = viewModel.playerUiController

    PlayerScreen(
        mediaDisplay = {
            DefaultMediaInfoDisplay(playerUiState = playerUiState)
        },
        controlButtons = {
            PodcastControlButtons(
                playerController = playerUiController,
                playerUiState = playerUiState,

            )
        },
        buttons = { },
        background = {
            ArtworkImageBackground((playerUiState.media as? MediaUiModel.Ready)?.artwork)
        }
    )
}
