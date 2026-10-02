package fm.libro.wearos.player

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@UnstableApi
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerProgressPersisterTest {

    private val isbn = "9781234567897"

    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var scope: CoroutineScope
    private lateinit var dao: FakePlaybackProgressDao
    private lateinit var player: FakeAudioPlayer
    private lateinit var persister: PlayerProgressPersister

    @Before
    fun setUp() {
        scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        scope = CoroutineScope(StandardTestDispatcher(scheduler))
        dao = FakePlaybackProgressDao()
        player = FakeAudioPlayer()
        player.loadMediaItems(listOf("${isbn}_0", "${isbn}_1", "${isbn}_2"))
        persister = PlayerProgressPersister(player, dao, scope) { scheduler.currentTime }
        player.addListener(persister)
        drain()
    }

    @After
    fun tearDown() {
        player.pause()
        drain()
        Dispatchers.resetMain()
    }

    @Test
    fun `resolves isbn from mediaId because horologist never sets a tag`() {
        player.play()
        player.advancePositionTo(30_000)
        player.pause()
        drain()

        assertEquals(isbn, dao.lastWriteFor(isbn)?.isbn)
    }

    @Test
    fun `persists position when paused from a headset button`() {
        player.play()
        player.advancePositionTo(42_000)
        player.pause()
        drain()

        val saved = dao.lastWriteFor(isbn)
        assertEquals(42_000L, saved?.positionMs)
        assertEquals(0, saved?.trackIndex)
    }

    @Test
    fun `persists position when paused near the end of a track`() {
        player.play()
        player.advancePositionTo(1_000)
        player.advancePositionTo(59_500)
        player.pause()
        drain()

        assertEquals(59_500L, dao.lastWriteFor(isbn)?.positionMs)
    }

    @Test
    fun `persists position when paused while the app is in the background`() {
        player.play()
        player.advancePositionTo(12_345)
        player.pause()
        drain()

        assertEquals(12_345L, dao.lastWriteFor(isbn)?.positionMs)
    }

    @Test
    fun `persists position on a seek`() {
        player.play()
        player.advancePositionTo(5_000)
        player.seekTo(75_000)
        drain()

        val saved = dao.lastWriteFor(isbn)
        assertEquals(75_000L, saved?.positionMs)
        assertEquals(0, saved?.trackIndex)
    }

    @Test
    fun `persists the track index of the current item`() {
        player.play()
        player.advancePositionTo(7_000)
        player.transitionToTrack(2)
        drain()

        assertEquals(2, player.currentMediaItemIndex)
        val saved = dao.lastWriteFor(isbn)
        assertEquals(2, saved?.trackIndex)
        assertEquals(0L, saved?.positionMs)
    }

    @Test
    fun `persists position when the track ends`() {
        player.play()
        player.advancePositionTo(60_000)
        player.setPlaybackStateForTest(Player.STATE_ENDED)
        drain()

        assertEquals(60_000L, dao.lastWriteFor(isbn)?.positionMs)
    }

    @Test
    fun `persists position on player error`() {
        player.play()
        player.advancePositionTo(9_000)
        player.raiseError()
        drain()

        assertEquals(9_000L, dao.lastWriteFor(isbn)?.positionMs)
    }

    @Test
    fun `flush persists the position captured at flush time`() {
        player.play()
        player.advancePositionTo(21_000)
        player.pause()
        persister.flush()
        drain()

        assertEquals(21_000L, dao.lastWriteFor(isbn)?.positionMs)
    }

    @Test
    fun `does not persist when no media item is loaded`() {
        player.loadMediaItems(emptyList())
        player.advancePositionTo(5_000)
        player.pause()
        persister.flush()
        drain()

        assertTrue(dao.writes.isEmpty())
    }

    @Test
    fun `writes periodically while playing`() {
        player.play()
        player.advancePositionTo(1_000)
        drain()
        scheduler.advanceTimeBy(6_000)
        drain()
        val writesAfterFirstTick = dao.writes.size

        player.advancePositionTo(20_000)
        drain()
        scheduler.advanceTimeBy(6_000)
        drain()

        assertTrue(dao.writes.size > writesAfterFirstTick)
        assertEquals(20_000L, dao.lastWriteFor(isbn)?.positionMs)
    }

    @Test
    fun `does not write again when nothing changed`() {
        player.play()
        player.advancePositionTo(15_000)
        player.pause()
        drain()
        val writesAfterPause = dao.writes.size

        persister.flush()
        persister.flush()
        drain()

        assertEquals(writesAfterPause, dao.writes.size)
    }

    @Test
    fun `keeps positions of different books separate`() {
        val other = "9780000000001"
        player.play()
        player.advancePositionTo(20_000)
        player.pause()
        drain()

        player.loadMediaItems(listOf("${other}_0"))
        player.play()
        player.advancePositionTo(3_000)
        player.pause()
        drain()

        assertEquals(20_000L, dao.lastWriteFor(isbn)?.positionMs)
        assertEquals(3_000L, dao.lastWriteFor(other)?.positionMs)
    }

    @Test
    fun `pause after a stale periodic save still writes the newer position`() {
        player.play()
        player.advancePositionTo(1_000)
        drain()
        scheduler.advanceTimeBy(6_000)
        dao.writes.clear()

        player.advancePositionTo(48_000)
        player.pause()
        drain()

        assertEquals(48_000L, dao.lastWriteFor(isbn)?.positionMs)
    }

    private fun drain() {
        shadowOf(Looper.getMainLooper()).idle()
        scheduler.runCurrent()
    }
}