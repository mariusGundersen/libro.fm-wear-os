package fm.libro.wearos.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.material3.screens.player.DefaultMediaInfoDisplay
import com.google.android.horologist.media.ui.material3.screens.player.DefaultPlayerScreenControlButtons

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
            DefaultPlayerScreenControlButtons(
                playerController = playerUiController,
                playerUiState = playerUiState,
            )
        },
        buttons = { },
    )
}
