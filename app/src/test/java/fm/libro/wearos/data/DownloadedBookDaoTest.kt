package fm.libro.wearos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadedBookDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DownloadedBookDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.downloadedBookDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getByIsbnWithProgress_emitsNullForUnknownIsbn() =
        runBlocking {
            assertNull(dao.getByIsbnWithProgress("9780000000000").first())
        }

    @Test
    fun getByIsbnWithProgress_emitsBookWithoutProgress() =
        runBlocking {
            dao.insert(book())

            val result = dao.getByIsbnWithProgress(ISBN).first()

            assertEquals(ISBN, result?.book?.isbn)
            assertNull(result?.progress)
        }

    @Test
    fun getByIsbnWithProgress_emitsBookWithProgress() =
        runBlocking {
            dao.insert(book())
            db.playbackProgressDao()
                .upsert(
                    PlaybackProgressEntity(
                        isbn = ISBN,
                        trackIndex = 3,
                        positionMs = 1_000L,
                        playbackSpeed = 1.5f,
                        updatedAt = 10L,
                    )
                )

            val result = dao.getByIsbnWithProgress(ISBN).first()

            assertEquals(3, result?.progress?.trackIndex)
            assertEquals(1_000L, result?.progress?.positionMs)
        }

    private fun book() =
        DownloadedBookEntity(
            isbn = ISBN,
            title = "Reentry",
            author = "Eric Berger",
            coverUrl = null,
            coverLocalPath = null,
            format = "m4b",
            filePath = "/tmp/reentry.m4b",
            fileSizeBytes = 1L,
            durationSeconds = 60,
            trackCount = 1,
            tracksJson = "[]",
            downloadedAt = 1L,
            narratorsJson = "[]",
        )

    companion object {
        private const val ISBN = "9781039489479"
    }
}
