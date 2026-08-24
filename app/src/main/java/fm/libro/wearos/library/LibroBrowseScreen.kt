package fm.libro.wearos.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import fm.libro.wearos.data.DownloadedBookEntity
import java.io.File

@Composable
fun LibroBrowseScreen(
    viewModel: DownloadedBooksViewModel,
    onBookClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val books by viewModel.downloadedBooks.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 10.dp,
            vertical = 32.dp,
        ),
    ) {
        item {
            ListHeader {
                Text("My Books")
            }
        }

        if (books.isEmpty()) {
            item {
                Text(
                    text = "No downloaded books",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            items(books.size, key = { books[it].isbn }) { index ->
                DownloadedBookCard(
                    book = books[index],
                    onClick = { onBookClick(books[index].isbn) },
                )
            }
        }

        item {
            Card(
                onClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadedBookCard(
    book: DownloadedBookEntity,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        val coverModel: Any? = book.coverLocalPath?.let { File(it) }
            ?: book.coverUrl?.let { "https:$it" }
        if (coverModel != null) {
            AsyncImage(
                model = coverModel,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                contentScale = ContentScale.FillWidth,
            )
        }

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp),
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
        )
    }
}
