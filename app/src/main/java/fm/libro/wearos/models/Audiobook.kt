package fm.libro.wearos.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fm.libro.wearos.api.models.Audiobook as ApiAudiobook
import fm.libro.wearos.data.DownloadedBookWithProgress
import fm.libro.wearos.data.StoredTrack

data class Track(
    val number: Int,
    val title: String?,
    val lengthSec: Int,
    val filePath: String? = null,
)

data class Audiobook(
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val narrators: List<String>,
    val coverUrl: String?,
    val coverLocalPath: String? = null,
    val durationSeconds: Int,
    val trackCount: Int,
    val tracks: List<Track>,
    val isFinished: Boolean,
    val trackIndex: Int,
    val positionMs: Long,
) {
    val authorString: String
        get() = authors.joinToString(", ").ifEmpty { "Unknown" }

    val narratorString: String
        get() = narrators.joinToString(", ")

    val durationString: String
        get() = formatDuration(durationSeconds)

    val listenedSeconds: Float
        get() {
            if (tracks.isEmpty()) {
                if (durationSeconds == 0 || trackCount == 0) return positionMs / 1000f
                val avgTrackDuration = durationSeconds.toFloat() / trackCount
                return trackIndex * avgTrackDuration + positionMs / 1000f
            }
            val currentIndex = trackIndex.coerceIn(0, tracks.size - 1)
            val prior = tracks.take(currentIndex).sumOf { it.lengthSec }.toFloat()
            return prior + positionMs / 1000f
        }

    val listenedTimeString: String
        get() = formatDuration(listenedSeconds.toInt())

    val remainingTimeString: String
        get() {
            if (durationSeconds == 0) return ""
            val remaining = (durationSeconds - listenedSeconds.toInt()).coerceAtLeast(0)
            return formatDuration(remaining)
        }

    val isStarted: Boolean
        get() = trackIndex > 0 || positionMs > 0L

    val progressPercent: Int
        get() {
            if (durationSeconds == 0) return 0
            return ((listenedSeconds / durationSeconds) * 100).toInt().coerceIn(0, 100)
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

    companion object {
        private val gson = Gson()

        fun fromApi(api: ApiAudiobook): Audiobook {
            val authors = when (val a = api.authors) {
                is String -> listOf(a)
                is List<*> -> a.filterIsInstance<String>()
                else -> emptyList()
            }
            val narrators = api.audiobookInfo?.narrators ?: emptyList()
            val tracks = api.manifest?.tracks?.map {
                Track(
                    number = it.number,
                    title = it.chapterTitle,
                    lengthSec = it.lengthSec,
                )
            } ?: emptyList()
            val metadata = api.userMetadata
            return Audiobook(
                isbn = api.isbn,
                title = api.title,
                authors = authors,
                narrators = narrators,
                coverUrl = api.coverUrl,
                durationSeconds = api.durationSeconds,                trackCount = api.trackCount,
                tracks = tracks,
                isFinished = metadata?.finished == true,
                trackIndex = metadata?.trackIndex ?: 0,
                positionMs = ((metadata?.trackSeconds ?: 0f) * 1000).toLong(),
            )
        }

        fun fromDownloaded(item: DownloadedBookWithProgress): Audiobook {
            val book = item.book
            val authors = book.author.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val narrators = parseStringList(book.narratorsJson)
            val tracks = parseStoredTracks(book.tracksJson).map {
                Track(
                    number = it.number,
                    title = it.chapterTitle,
                    lengthSec = it.lengthSec,
                    filePath = it.filePath,
                )
            }
            val progress = item.progress
            return Audiobook(
                isbn = book.isbn,
                title = book.title,
                authors = authors,
                narrators = narrators,
                coverUrl = book.coverUrl,
                coverLocalPath = book.coverLocalPath,
                durationSeconds = book.durationSeconds,
                trackCount = book.trackCount,
                tracks = tracks,
                isFinished = false,
                trackIndex = progress?.trackIndex ?: 0,
                positionMs = progress?.positionMs ?: 0L,
            )
        }

        private fun parseStoredTracks(json: String): List<StoredTrack> {
            if (json.isBlank()) return emptyList()
            return try {
                gson.fromJson<List<StoredTrack>>(
                    json,
                    object : TypeToken<List<StoredTrack>>() {}.type,
                )
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun parseStringList(json: String): List<String> {
            if (json.isBlank()) return emptyList()
            return try {
                gson.fromJson<List<String>>(
                    json,
                    object : TypeToken<List<String>>() {}.type,
                )
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
