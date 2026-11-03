package fm.libro.wearos.data

import androidx.room.Entity

@Entity(tableName = "track_progress", primaryKeys = ["isbn", "trackIndex"])
data class TrackProgressEntity(
    val isbn: String,
    val trackIndex: Int,
    val positionMs: Long,
    val updatedAt: Long,
)
