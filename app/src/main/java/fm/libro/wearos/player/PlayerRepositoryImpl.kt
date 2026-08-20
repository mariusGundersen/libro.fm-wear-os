package fm.libro.wearos.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_BACK
import androidx.media3.common.Player.COMMAND_SEEK_FORWARD
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.model.Command
import com.google.android.horologist.media.model.Media
import com.google.android.horologist.media.model.PlaybackState
import com.google.android.horologist.media.model.PlaybackStateEvent
import com.google.android.horologist.media.model.PlayerState
import com.google.android.horologist.media.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalHorologistApi::class)
class PlayerRepositoryImpl(
    val player: Player,
) : PlayerRepository {

    private val _currentMedia = MutableStateFlow<Media?>(null)
    override val currentMedia: StateFlow<Media?> = _currentMedia.asStateFlow()

    private val _latestPlaybackState = MutableStateFlow(
        PlaybackStateEvent(
            playbackState = PlaybackState(
                playerState = PlayerState.Idle,
                isLive = false,
                currentPosition = null,
                duration = null,
                playbackSpeed = 1.0f,
            ),
            cause = PlaybackStateEvent.Cause.Initial,
        )
    )
    override val latestPlaybackState: StateFlow<PlaybackStateEvent> = _latestPlaybackState.asStateFlow()

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _availableCommands = MutableStateFlow<Set<Command>>(emptySet())
    override val availableCommands: StateFlow<Set<Command>> = _availableCommands.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    override val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    override val seekBackIncrement: StateFlow<Duration?> = MutableStateFlow(30.seconds)
    override val seekForwardIncrement: StateFlow<Duration?> = MutableStateFlow(30.seconds)

    private var mediaList: MutableList<Media> = mutableListOf()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
            updateAvailableCommands()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
            updateAvailableCommands()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentMedia()
            updatePlaybackState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            updatePlaybackState()
        }
    }

    init {
        player.addListener(playerListener)
    }

    fun connect() {
        _connected.value = true
        updateCurrentMedia()
        updatePlaybackState()
        updateAvailableCommands()
    }

    fun disconnect() {
        _connected.value = false
        player.removeListener(playerListener)
    }

    private fun updateCurrentMedia() {
        val index = player.currentMediaItemIndex
        _currentMedia.value = mediaList.getOrNull(index)
    }

    private fun updatePlaybackState() {
        val state = when (player.playbackState) {
            Player.STATE_IDLE -> PlayerState.Idle
            Player.STATE_BUFFERING -> PlayerState.Loading
            Player.STATE_READY -> {
                if (player.isPlaying) PlayerState.Playing else PlayerState.Stopped
            }
            Player.STATE_ENDED -> PlayerState.Stopped
            else -> PlayerState.Idle
        }

        val currentPosition = player.currentPosition.coerceAtLeast(0).milliseconds
        val duration = player.duration.coerceAtLeast(0).milliseconds

        _latestPlaybackState.value = PlaybackStateEvent(
            playbackState = PlaybackState(
                playerState = state,
                isLive = false,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = player.playbackParameters.speed,
            ),
            cause = PlaybackStateEvent.Cause.PlayerStateChanged,
        )
    }

    private fun updateAvailableCommands() {
        val commands = mutableSetOf<Command>()
        if (player.isCommandAvailable(COMMAND_PLAY_PAUSE)) commands.add(Command.PlayPause)
        if (player.isCommandAvailable(COMMAND_SEEK_BACK)) commands.add(Command.SeekBack)
        if (player.isCommandAvailable(COMMAND_SEEK_FORWARD)) commands.add(Command.SeekForward)
        if (player.isCurrentMediaItemSeekable) commands.add(Command.SeekInCurrentMediaItem)
        if (player.hasPreviousMediaItem()) commands.add(Command.SkipToPreviousMedia)
        if (player.hasNextMediaItem()) commands.add(Command.SkipToNextMedia)
        commands.add(Command.SetShuffle)
        _availableCommands.value = commands
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekBack() {
        player.seekBack()
    }

    override fun seekForward() {
        player.seekForward()
    }

    override fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackParameters(PlaybackParameters(speed))
    }

    override fun skipToNextMedia() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    override fun skipToPreviousMedia() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
    }

    override fun setMedia(media: Media) {
        mediaList.clear()
        mediaList.add(media)
        val mediaItem = MediaItem.fromUri(media.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        updateCurrentMedia()
    }

    override fun setMediaList(mediaList: List<Media>) {
        this.mediaList.clear()
        this.mediaList.addAll(mediaList)
        val mediaItems = mediaList.map { MediaItem.fromUri(it.uri) }
        player.setMediaItems(mediaItems)
        player.prepare()
        updateCurrentMedia()
    }

    override fun setMediaList(mediaList: List<Media>, index: Int, position: Duration?) {
        this.mediaList.clear()
        this.mediaList.addAll(mediaList)
        val mediaItems = mediaList.map { MediaItem.fromUri(it.uri) }
        player.setMediaItems(mediaItems, index, position?.inWholeMilliseconds ?: 0)
        player.prepare()
        updateCurrentMedia()
    }

    override fun addMedia(media: Media) {
        mediaList.add(media)
        player.addMediaItem(MediaItem.fromUri(media.uri))
    }

    override fun addMedia(index: Int, media: Media) {
        mediaList.add(index, media)
        player.addMediaItem(index, MediaItem.fromUri(media.uri))
    }

    override fun removeMedia(index: Int) {
        mediaList.removeAt(index)
        player.removeMediaItem(index)
    }

    override fun clearMediaList() {
        mediaList.clear()
        player.clearMediaItems()
        _currentMedia.value = null
    }

    override fun getCurrentMediaIndex(): Int = player.currentMediaItemIndex

    override fun getMediaAt(index: Int): Media? = mediaList.getOrNull(index)

    override fun getMediaCount(): Int = mediaList.size

    override fun hasNextMedia(): Boolean = player.hasNextMediaItem()

    override fun hasPreviousMedia(): Boolean = player.hasPreviousMediaItem()

    override fun seekToDefaultPosition(mediaIndex: Int) {
        player.seekToDefaultPosition(mediaIndex)
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        player.shuffleModeEnabled = shuffleModeEnabled
    }
}
