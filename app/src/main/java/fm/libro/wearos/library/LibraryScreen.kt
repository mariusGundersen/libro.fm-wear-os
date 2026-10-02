package fm.libro.wearos.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import fm.libro.wearos.models.Audiobook

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (Audiobook) -> Unit,
) {
    val books = viewModel.books.collectAsLazyPagingItems()

    when (books.loadState.refresh) {
        is LoadState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading library",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        is LoadState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = (books.loadState.refresh as LoadState.Error).error.message ?: "Unknown error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        else -> {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 32.dp,
                ),
            ) {
                item {
                    Text(
                        text = "Available to download",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(books.itemCount, key = books.itemKey { it.isbn }) { index ->
                    val book = books[index]
                    if (book != null) {
                        AudiobookCard(
                            book = book,
                            onClick = { onBookClick(book) },
                        )
                    } else {
                        BookCardPlaceholder()
                    }
                }
                if (books.loadState.append is LoadState.Loading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCardPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        CircularProgressIndicator()
    }
}
