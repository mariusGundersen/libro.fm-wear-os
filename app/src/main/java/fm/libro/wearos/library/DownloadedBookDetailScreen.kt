package fm.libro.wearos.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import fm.libro.wearos.R

@Composable
fun DownloadedBookDetailScreen(
    viewModel: DownloadedBookDetailViewModel,
    onPlay: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        val book = state.book
        if (book == null) {
            item { CircularProgressIndicator() }
        } else {
            item {
                AudiobookInfo(book, state.coverLocalPath)
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.cacheAndPlay(onPlay)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = "Play",
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.deleteBook() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Delete",
                        )
                    }
                }
            }

            item {
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
