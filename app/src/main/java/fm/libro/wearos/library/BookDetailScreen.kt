package fm.libro.wearos.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import fm.libro.wearos.R
import org.intellij.lang.annotations.JdkConstants

@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onPlay: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    ScalingLazyColumn (
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                state.book?.let { book ->
                    AsyncImage(
                        model = "https:${book.coverUrl}",
                        contentDescription = book.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = book.authorString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )

                    if (book.audiobookInfo?.narrators?.isNotEmpty() == true) {
                        Text(
                            text = "Narrated by: ${book.audiobookInfo.narrators.first()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val duration = book.durationSeconds
                    val hours = duration / 3600
                    val minutes = (duration % 3600) / 60
                    Text(
                        text = "${hours}h ${minutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }
                state.isDownloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Downloading...",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            progress = { state.downloadProgress / 100f },
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                state.isDownloaded -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onPlay(state.book?.isbn ?: "") },
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
                state.manifest != null -> {
                    Button(
                        onClick = { viewModel.startDownload() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_playlist_add),
                            contentDescription = "Download",
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.fetchManifest() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Check download")
                    }
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
