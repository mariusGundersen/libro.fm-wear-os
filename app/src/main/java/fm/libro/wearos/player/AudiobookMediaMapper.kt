package fm.libro.wearos.player

import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.model.Media
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fm.libro.wearos.data.DownloadedBookEntity
import fm.libro.wearos.data.StoredTrack

@OptIn(ExperimentalHorologistApi::class)
object AudiobookMediaMapper {
    private val gson = Gson()

    fun mapFromDownloadedBook(
        entity: DownloadedBookEntity
    ): List<Media> {
        val tracks: List<StoredTrack> = gson.fromJson(
            entity.tracksJson,
            object : TypeToken<List<StoredTrack>>() {}.type,
        )

        return tracks.mapIndexed { index, track ->
            Media(
                id = "${entity.isbn}_$index",
                uri = track.filePath,
                title = track.chapterTitle ?: "Track ${track.number}",
                artist = entity.author,
                artworkUri = entity.coverLocalPath?.let { "file://$it" },
                extras = mapOf(
                    "isbn" to entity.isbn,
                    "trackIndex" to index,
                    "trackCount" to tracks.size,
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
                //artworkUri = coverUrl?.let { "file://$it" },
                extras = mapOf(
                    "isbn" to isbn,
                    "trackIndex" to index,
                    "trackCount" to tracks.size,
                ),
            )
        }
    }
}
