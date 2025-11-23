package com.kjjejones42.audiobookplayer.display

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import java.util.Locale
import java.util.concurrent.TimeUnit


@Composable
@Preview
fun ListItemComposablePreview() {
    val book = AudioBook("TEST TEST", "", "", emptyList(), "Author McAuthor")
    val item = ListItem.AudioBookContainer(book)
    AudioBookContainer(item, {}, {})
}

@Composable
fun ListItemComposable(item: ListItem, onClick: (AudioBook) -> Unit, onLongClick: (AudioBook) -> Unit) {
    if (item is ListItem.AudioBookContainer) {
        AudioBookContainer(item, onClick, onLongClick)
    } else if (item is ListItem.Heading) {
        Heading(item)
    }
}
@Composable
fun AudioBookContainer(item: ListItem.AudioBookContainer, onClick: (AudioBook) -> Unit, onLongClick: (AudioBook) -> Unit) {
    val audiobook = item.book
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = { onLongClick(audiobook) },
                onClick = { onClick(audiobook) }
            ),
        elevation = elevatedCardElevation()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ){
                Text(
                    style = MaterialTheme.typography.displayLarge,
                    text = audiobook.displayName.first().toString()
                )
                AsyncImage(
                    model = audiobook.imagePath ?: audiobook.files?.get(0),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(modifier = Modifier
                .weight(1f)
                .padding(8.dp)) {
                Text(
                    text = audiobook.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!audiobook.author.isNullOrBlank()) {
                    Text(
                        text = audiobook.author ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = msToReadableDuration(audiobook.totalDuration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Heading(item: ListItem.Heading) {
    Text(
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Start,
        text = item.headingTitle ?: ""
    )
}

private fun msToReadableDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    if (hours > 0) {
        return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
    }
    return String.format(Locale.getDefault(), "%02dm", minutes)
}