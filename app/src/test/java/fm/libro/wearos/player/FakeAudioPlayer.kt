package fm.libro.wearos.player

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class FakeAudioPlayer : SimpleBasePlayer(Looper.myLooper()!!) {

    private var items: List<MediaItem> = emptyList()
    private var index = 0
    private var positionMs = 0L
    private var playWhenReady = false
    private var playbackState = Player.STATE_IDLE
    private var playbackParameters = PlaybackParameters(1f)
    private var playerError: PlaybackException? = null

    fun loadMediaItems(ids: List<String>) {
        items = ids.map { id ->
            MediaItem.Builder()
                .setMediaId(id)
                .setUri(android.net.Uri.parse("file:///audio/$id.mp3"))
                .build()
        }
        index = 0
        positionMs = 0L
        playbackState = if (items.isEmpty()) Player.STATE_IDLE else Player.STATE_READY
        playWhenReady = false
        invalidateState()
    }

    fun advancePositionTo(value: Long) {
        positionMs = value
        invalidateState()
    }

    fun transitionToTrack(newIndex: Int) {
        index = newIndex
        positionMs = 0L
        invalidateState()
    }

    fun setPlaybackStateForTest(value: Int) {
        playbackState = value
        if (value == Player.STATE_IDLE || value == Player.STATE_ENDED) playWhenReady = false
        invalidateState()
    }

    fun raiseError() {
        playerError = PlaybackException(
            "test",
            null,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        )
        playbackState = Player.STATE_IDLE
        playWhenReady = false
        invalidateState()
    }

    override fun getState(): State = State.Builder()
        .setAvailableCommands(COMMANDS)
        .setPlaylist(
            items.map { item ->
                MediaItemData.Builder(Uid(item.mediaId))
                    .setMediaItem(item)
                    .build()
            },
        )
        .setCurrentMediaItemIndex(index.coerceAtMost(maxOf(0, items.size - 1)))
        .setPlaybackState(playbackState)
        .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setPlaybackParameters(playbackParameters)
        .setContentPositionMs(positionMs)
        .setContentBufferedPositionMs(PositionSupplier.getConstant(positionMs))
        .setPlayerError(playerError)
        .build()

    override fun handleSetPlayWhenReady(value: Boolean): ListenableFuture<*> {
        playWhenReady = value
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        index = mediaItemIndex
        this.positionMs = positionMs
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(
        playbackParameters: PlaybackParameters,
    ): ListenableFuture<*> {
        this.playbackParameters = playbackParameters
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playWhenReady = false
        playbackState = Player.STATE_IDLE
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleSetTrackSelectionParameters(
        trackSelectionParameters: androidx.media3.common.TrackSelectionParameters,
    ): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSetDeviceVolume(volume: Int, flags: Int): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleSetDeviceMuted(muted: Boolean, flags: Int): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    data class Uid(val id: String)

    private companion object {
        val COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SET_SPEED_AND_PITCH,
                Player.COMMAND_RELEASE,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            )
            .build()
    }
}