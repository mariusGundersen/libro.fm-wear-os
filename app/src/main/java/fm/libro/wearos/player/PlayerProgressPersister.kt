package fm.libro.wearos.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import fm.libro.wearos.data.PlaybackProgressDao
import fm.libro.wearos.data.PlaybackProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerProgressPersister(
    private val player: Player,
    private val playbackProgressDao: PlaybackProgressDao,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : Player.Listener {

    private var lastSavedAt = 0L
    private var lastSavedIsbn: String? = null
    private var lastSavedTrackIndex = -1
    private var lastSavedPositionMs = -1L
    private var periodicJob: Job? = null

    override fun onEvents(player: Player, events: Player.Events) {
        val isPlaying = player.isPlaying
        val paused = !isPlaying && !player.playWhenReady

        when {
            paused ||
                player.playbackState == Player.STATE_ENDED ||
                player.playbackState == Player.STATE_IDLE ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ->
                persist(force = true)
            else ->
                persist()
        }

        if (isPlaying && periodicJob?.isActive != true) {
            periodicJob?.cancel()
            periodicJob = scope.launch(Dispatchers.Main.immediate) {
                while (isActive) {
                    delay(SAVE_INTERVAL_MS)
                    persist()
                }
            }
        } else if (!isPlaying) {
            periodicJob?.cancel()
            periodicJob = null
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK) return
        val mediaItem = player.currentMediaItem ?: return
        val isbn = isbnFrom(mediaItem) ?: return

        write(
            isbn = isbn,
            trackIndex = player.currentMediaItemIndex,
            positionMs = newPosition.positionMs.coerceAtLeast(0L),
            speed = player.playbackParameters.speed,
            force = true,
        )
    }

    override fun onPlayerError(error: PlaybackException) {
        persist(force = true)
    }

    fun flush() {
        persist(force = true)
    }

    private fun persist(force: Boolean = false) {
        val mediaItem = player.currentMediaItem ?: return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val isbn = isbnFrom(mediaItem) ?: return

        write(
            isbn = isbn,
            trackIndex = player.currentMediaItemIndex,
            positionMs = positionMs,
            speed = player.playbackParameters.speed,
            force = force,
        )
    }

    private fun write(
        isbn: String,
        trackIndex: Int,
        positionMs: Long,
        speed: Float,
        force: Boolean,
    ) {
        val now = nowMs()

        if (isbn == lastSavedIsbn &&
            trackIndex == lastSavedTrackIndex &&
            positionMs == lastSavedPositionMs
        ) {
            lastSavedAt = now
            return
        }
        if (!force && now - lastSavedAt < SAVE_INTERVAL_MS) return

        lastSavedAt = now
        lastSavedIsbn = isbn
        lastSavedTrackIndex = trackIndex
        lastSavedPositionMs = positionMs

        val entity = PlaybackProgressEntity(
            isbn = isbn,
            trackIndex = trackIndex,
            positionMs = positionMs,
            playbackSpeed = speed,
            updatedAt = now,
        )
        scope.launch {
            playbackProgressDao.upsert(entity)
        }
    }

    private fun isbnFrom(mediaItem: MediaItem): String? =
        mediaItem.mediaId.takeIf { it.isNotBlank() }
            ?.substringBeforeLast(SEP)
            ?.takeIf { it.isNotBlank() }

    private companion object {
        const val SEP = "_"
        const val SAVE_INTERVAL_MS = 5_000L
    }
}