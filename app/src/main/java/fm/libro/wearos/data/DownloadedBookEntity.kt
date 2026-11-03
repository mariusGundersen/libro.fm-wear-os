package fm.libro.wearos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_books")
data class DownloadedBookEntity(
    @PrimaryKey val isbn: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val coverLocalPath: String?,
    val format: String, // "m4b" or "mp3"
    val filePath: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val trackCount: Int,
    val tracksJson: String, // JSON array of track metadata
    val downloadedAt: Long,
    val narratorsJson: String, // JSON array of narrator names
)
