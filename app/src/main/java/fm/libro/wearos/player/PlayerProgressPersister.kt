package fm.libro.wearos.player

import androidx.media3.common.Player
import fm.libro.wearos.data.PlaybackProgressDao
import fm.libro.wearos.data.PlaybackProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerProgressPersister(
    private val player: Player,
    private val playbackProgressDao: PlaybackProgressDao,
    private val playerStateRepository: PlayerStateRepository,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var lastSavedAt = 0L
    private var wasPlaying = false
    private var periodicJob: Job? = null

    override fun onEvents(player: Player, events: Player.Events) {
        val isPlaying = player.isPlaying
        val justPaused = wasPlaying && !isPlaying
        wasPlaying = isPlaying

        when {
            justPaused || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ->
                persist(force = true)
            events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED) ->
                persist(force = false)
        }

        if (player.playbackState == Player.STATE_ENDED) {
            persist(force = true)
        }

        if (isPlaying && periodicJob?.isActive != true) {
            periodicJob?.cancel()
            periodicJob = scope.launch {
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

    private fun persist(force: Boolean = false) {
        val mediaItem = player.currentMediaItem ?: return
        val playbackState = player.playbackState
        val positionMs = player.currentPosition
        if (positionMs <= 0 && playbackState != Player.STATE_ENDED) return

        val isbn = isbnFromTag(mediaItem) ?: playerStateRepository.currentIsbn ?: return

        val now = System.currentTimeMillis()
        if (!force && now - lastSavedAt < SAVE_INTERVAL_MS) return
        lastSavedAt = now

        scope.launch {
            playbackProgressDao.upsert(
                PlaybackProgressEntity(
                    isbn = isbn,
                    trackIndex = player.currentMediaItemIndex,
                    positionMs = positionMs,
                    playbackSpeed = player.playbackParameters.speed,
                    updatedAt = now,
                )
            )
        }
    }

    private fun isbnFromTag(mediaItem: androidx.media3.common.MediaItem): String? =
        mediaItem.localConfiguration?.tag?.toString()?.substringBefore("_")

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L
    }
}