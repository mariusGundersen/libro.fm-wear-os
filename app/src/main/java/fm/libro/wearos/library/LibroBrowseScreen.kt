package fm.libro.wearos.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun LibroBrowseScreen(
    viewModel: DownloadedBooksViewModel,
    onBookClick: (String) -> Unit,
    onBrowseAllClick: () -> Unit,
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
            items(books, key = { it.isbn }) { book ->
                AudiobookCard(
                    book = book,
                    onClick = { onBookClick(book.isbn) },
                )
            }
        }

        item {
            Card(
                onClick = onBrowseAllClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = if (viewModel.isAuthenticated) { "Browse online" } else { "Log in to browse" },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

