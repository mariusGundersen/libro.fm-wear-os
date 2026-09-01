package fm.libro.wearos.data

import androidx.room.Embedded

data class DownloadedBookWithProgress(
    @Embedded val book: DownloadedBookEntity,
    @Embedded(prefix = "progress_") val progress: PlaybackProgressEntity?,
)
