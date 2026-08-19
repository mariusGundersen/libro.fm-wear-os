package fm.libro.wearos.api.models

import com.google.gson.annotations.SerializedName

data class Audiobook(
    val isbn: String,
    val title: String,
    val authors: Any?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("audiobook_info") val audiobookInfo: AudiobookInfo?,
    val series: String?,
    @SerializedName("series_num") val seriesNum: Int?,
    val publisher: String?,
    @SerializedName("publication_date") val publicationDate: String?,
    val description: String?,
    @SerializedName("user_metadata") val userMetadata: UserMetadata?,
) {
    @Transient
    var trackLengths: List<Int>? = null

    val authorString: String
        get() = when (val a = authors) {
            is String -> a
            is List<*> -> a.filterIsInstance<String>().joinToString(", ")
            else -> "Unknown"
        }

    val durationSeconds: Int
        get() = audiobookInfo?.duration ?: 0

    val trackCount: Int
        get() = audiobookInfo?.trackCount ?: 0

    private val progressSeconds: Float
        get() = userMetadata?.trackSeconds ?: 0.0f

    val isFinished: Boolean
        get() = userMetadata?.finished == true

    val isStarted: Boolean
        get() = (userMetadata?.trackIndex ?: 0) > 0 || (userMetadata?.trackSeconds ?: 0.0f) > 0.0f

    val listeningProgressPercent: Int
        get() {
            if (durationSeconds == 0) return 0
            val trackIndex = userMetadata?.trackIndex ?: return 0
            val trackSeconds = userMetadata.trackSeconds ?: 0f
            val listenedSeconds = getListenedSeconds(trackIndex, trackSeconds)
            return ((listenedSeconds / durationSeconds) * 100).toInt().coerceIn(0, 100)
        }

    val remainingTimeString: String
        get() {
            if (durationSeconds == 0) return ""
            val trackIndex = userMetadata?.trackIndex ?: return formatDuration(durationSeconds)
            val trackSeconds = userMetadata.trackSeconds ?: 0f
            val listenedSeconds = getListenedSeconds(trackIndex, trackSeconds)
            val remaining = (durationSeconds - listenedSeconds).toInt().coerceAtLeast(0)
            return formatDuration(remaining)
        }

    val durationString: String
        get() {
            return formatDuration(durationSeconds)
        }

    private fun getListenedSeconds(trackIndex: Int, trackSeconds: Float): Float {
        val lengths = trackLengths
        return if (!lengths.isNullOrEmpty()) {
            lengths.take(trackIndex).sum().toFloat() + trackSeconds
        } else {
            if (trackCount == 0) return 0f
            val avgTrackDuration = durationSeconds.toFloat() / trackCount
            trackIndex * avgTrackDuration + trackSeconds
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

data class AudiobookInfo(
    val narrators: List<String>?,
    val duration: Int?,
    @SerializedName("size_bytes") val sizeBytes: Long?,
    @SerializedName("track_count") val trackCount: Int?,
    @SerializedName("parts_count") val partsCount: Int?,
    @SerializedName("audio_language") val audioLanguage: String?,
)

data class UserMetadata(
    @SerializedName("track_index") val trackIndex: Int?,
    @SerializedName("track_seconds") val trackSeconds: Float?,
    @SerializedName("finished") val finished: Boolean?,
    @SerializedName("added_at") val addedAt: String?,
)

data class LibraryMetadata(
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int,
    val audiobooks: List<Audiobook>,
    val tags: List<String>,
)
