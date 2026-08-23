package fm.libro.wearos.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val prefs by viewModel.settings.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ListHeader {
                Text("Settings")
            }
        }

        val suppressSpeaker = prefs?.get(SettingsViewModel.KEY_SUPPRESS_SPEAKER) ?: true
        item {
            Text(
                text = "Speaker suppression: ${if (suppressSpeaker) "On" else "Off"}",
            )
        }

        val audioOffload = prefs?.get(SettingsViewModel.KEY_AUDIO_OFFLOAD) ?: true
        item {
            Text(
                text = "Audio offload: ${if (audioOffload) "On" else "Off"}",
            )
        }
    }
}
