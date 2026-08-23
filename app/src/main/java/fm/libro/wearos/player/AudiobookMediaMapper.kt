package fm.libro.wearos.player

import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.model.Media
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.data.StoredTrack

@OptIn(ExperimentalHorologistApi::class)
object AudiobookMediaMapper {

    fun mapFromAudiobook(audiobook: Audiobook, trackBasePath: String? = null): List<Media> {
        val manifest = audiobook.manifest
        val trackCount = audiobook.trackCount

        return (0 until trackCount).map { index ->
            val track = manifest?.tracks?.getOrNull(index)
            val trackNumber = track?.number ?: (index + 1)
            val chapterTitle = track?.chapterTitle ?: "Track $trackNumber"

            val uri = if (trackBasePath != null) {
                "$trackBasePath/track_$trackNumber"
            } else {
                audiobook.manifest?.parts?.firstOrNull()?.url ?: ""
            }

            Media(
                id = "${audiobook.isbn}_$index",
                uri = uri,
                title = chapterTitle,
                artist = audiobook.authorString,
                artworkUri = audiobook.coverUrl?.let { "https:$it" },
                extras = mapOf(
                    "isbn" to audiobook.isbn,
                    "trackIndex" to index,
                    "trackCount" to trackCount,
                ),
            )
        }
    }

    fun mapFromStoredTracks(
        isbn: String,
        title: String,
        artist: String,
        coverUrl: String?,
        tracks: List<StoredTrack>,
    ): List<Media> {
        return tracks.mapIndexed { index, track ->
            Media(
                id = "${isbn}_$index",
                uri = track.filePath,
                title = track.chapterTitle ?: "Track ${track.number}",
                artist = artist,
                artworkUri = coverUrl?.let { "https:$it" },
                extras = mapOf(
                    "isbn" to isbn,
                    "trackIndex" to index,
                    "trackCount" to tracks.size,
                ),
            )
        }
    }
}
