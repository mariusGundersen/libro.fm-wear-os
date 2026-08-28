package fm.libro.wearos.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import fm.libro.wearos.R
import fm.libro.wearos.api.models.Audiobook
import java.io.File

@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()


    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item {
            state.book?.let { book ->
                AudiobookInfo(book, state.coverLocalPath)
            }

        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            when {
                state.isDownloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { state.downloadProgress / 100f },
                            modifier = Modifier.size(48.dp),
                            strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloading... ${state.downloadProgress}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                state.isDownloaded -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.playBook()
                                onPlay()
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

                else -> {
                    Button(
                        onClick = { viewModel.startDownload() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = "Download",
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
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

@Composable
fun AudiobookInfo(book: Audiobook, coverLocalPath: String? = null) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coverModel: Any? = coverLocalPath?.let { File(it) }
            ?: book.coverUrl?.let { "https:$it" }
        AsyncImage(
            model = coverModel,
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
            textAlign = TextAlign.Center
        )

        Text(
            text = "Author: ${book.authorString}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        if (book.audiobookInfo?.narrators?.isNotEmpty() == true) {
            Text(
                text = "Narrator: ${book.audiobookInfo.narrators.first()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = book.durationString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (book.userMetadata != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    book.isFinished -> "Finished"
                    book.isStarted -> "${book.listeningProgressPercent}% · ${book.remainingTimeString} remaining"
                    else -> "Not started"
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (book.isFinished) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

    }
}
