package fm.libro.wearos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val isbn: String,
    val trackIndex: Int,
    val positionMs: Long,
    val playbackSpeed: Float,
    val updatedAt: Long,
)
