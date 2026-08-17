package fm.libro.wearos.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import fm.libro.wearos.R

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = state.coverUrl,
            contentDescription = state.title,
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = state.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = state.author,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.durationMs > 0) {
            val progress = state.currentPositionMs.toFloat() / state.durationMs.toFloat()
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(48.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(state.currentPositionMs),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.tracks.size > 1) {
                Button(
                    onClick = { viewModel.previousTrack() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Button(
                onClick = { viewModel.skipBackward() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_replay),
                    contentDescription = "Rewind 30s",
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = {
                    if (state.isPlaying) viewModel.pause() else viewModel.play()
                },
            ) {
                Icon(
                    painter = painterResource(
                        if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    ),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = { viewModel.skipForward() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward_30),
                    contentDescription = "Forward 30s",
                )
            }

            if (state.tracks.size > 1) {
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { viewModel.nextTrack() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                Button(
                    onClick = { viewModel.setPlaybackSpeed(speed) },
                    colors = if (state.playbackSpeed == speed) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
