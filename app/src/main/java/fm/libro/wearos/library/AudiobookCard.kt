package fm.libro.wearos.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import coil.compose.AsyncImage
import fm.libro.wearos.models.Audiobook
import java.io.File


@Composable
fun AudiobookCard(
    book: Audiobook,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val coverModel: Any? = book.coverLocalPath?.let { File(it) }
            ?: book.coverUrl?.let { "https:$it" }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 3.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp)
            ) {
                if (coverModel != null) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = book.title,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.FillWidth,
                    )
                }
                CircularProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 2.dp,
                    gapSize = 0.dp,
                    colors = ProgressIndicatorDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ){
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (book.narratorString.isNotEmpty()) {
                        "by ${book.authorString}, read by ${book.narratorString}"
                    } else {
                        "by ${book.authorString}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = when {
                book.isFinished -> "Finished"
                book.isStarted -> "${book.remainingTimeString} remaining"
                else -> book.durationString
            },
            style = MaterialTheme.typography.bodyExtraSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.align(Alignment.End),
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