package fm.libro.wearos.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackProgressBackfillTest {

    @Test
    fun `chapters below the current track are complete and the current track keeps its position`() {
        val rows = AppDatabase.trackProgressBackfill(
            isbn = ISBN,
            trackIndex = 2,
            positionMs = 12_000L,
            updatedAt = 5L,
            tracksJson = tracksJson(600, 700, 800, 900),
        )

        assertEquals(
            listOf(
                TrackProgressEntity(ISBN, 0, 600_000L, 5L),
                TrackProgressEntity(ISBN, 1, 700_000L, 5L),
                TrackProgressEntity(ISBN, 2, 12_000L, 5L),
            ),
            rows,
        )
    }

    @Test
    fun `an unstarted current track only backfills the chapters before it`() {
        val rows = AppDatabase.trackProgressBackfill(
            isbn = ISBN,
            trackIndex = 1,
            positionMs = 0L,
            updatedAt = 5L,
            tracksJson = tracksJson(600, 700),
        )

        assertEquals(listOf(TrackProgressEntity(ISBN, 0, 600_000L, 5L)), rows)
    }

    @Test
    fun `tracks without a known length are skipped`() {
        val rows = AppDatabase.trackProgressBackfill(
            isbn = ISBN,
            trackIndex = 2,
            positionMs = 1_000L,
            updatedAt = 5L,
            tracksJson = tracksJson(0, 700, 800),
        )

        assertEquals(
            listOf(
                TrackProgressEntity(ISBN, 1, 700_000L, 5L),
                TrackProgressEntity(ISBN, 2, 1_000L, 5L),
            ),
            rows,
        )
    }

    @Test
    fun `a current track beyond the track list backfills every chapter`() {
        val rows = AppDatabase.trackProgressBackfill(
            isbn = ISBN,
            trackIndex = 9,
            positionMs = 1_000L,
            updatedAt = 5L,
            tracksJson = tracksJson(600, 700),
        )

        assertEquals(
            listOf(
                TrackProgressEntity(ISBN, 0, 600_000L, 5L),
                TrackProgressEntity(ISBN, 1, 700_000L, 5L),
            ),
            rows,
        )
    }

    @Test
    fun `malformed track metadata leaves only the current track`() {
        val rows = AppDatabase.trackProgressBackfill(
            isbn = ISBN,
            trackIndex = 3,
            positionMs = 4_000L,
            updatedAt = 5L,
            tracksJson = "not json",
        )

        assertEquals(listOf(TrackProgressEntity(ISBN, 3, 4_000L, 5L)), rows)
    }

    private fun tracksJson(vararg lengths: Int): String =
        Gson().toJson(
            lengths.mapIndexed { index, length ->
                StoredTrack(
                    number = index + 1,
                    lengthSec = length,
                    chapterTitle = "Chapter ${index + 1}",
                    filePath = "/tmp/$ISBN/$index.mp3",
                )
            }
        )

    companion object {
        private const val ISBN = "9781039489479"
    }
}
