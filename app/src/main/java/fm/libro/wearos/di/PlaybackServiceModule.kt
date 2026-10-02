package fm.libro.wearos.di

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlayer.AudioOffloadListener
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.ui.WearUnsuitableOutputPlaybackSuppressionResolverListener
import com.google.android.horologist.media3.config.WearMedia3Factory
import com.google.android.horologist.media3.logging.AnalyticsEventLogger
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.logging.TransferListener
import com.google.android.horologist.media3.navigation.IntentBuilder
import com.google.android.horologist.media3.tracing.TracingListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import fm.libro.wearos.complication.DataUpdates
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.PlaybackProgressDao
import fm.libro.wearos.offload.AudioOffloadManager
import fm.libro.wearos.player.LibroMediaLibrarySessionCallback
import fm.libro.wearos.player.PlayerProgressPersister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@SuppressLint("UnsafeOptInUsageError")
@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceModule {

    @ServiceScoped
    @Provides
    fun wearMedia3Factory(
        @ApplicationContext context: Context,
    ): WearMedia3Factory = WearMedia3Factory(context)

    @ServiceScoped
    @Provides
    fun loadControl(): LoadControl = DefaultLoadControl.Builder()
        .setBackBuffer(30_000, false)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    @ServiceScoped
    @Provides
    fun mediaCodecSelector(
        wearMedia3Factory: WearMedia3Factory,
    ): MediaCodecSelector = wearMedia3Factory.mediaCodecSelector()

    @ServiceScoped
    @Provides
    fun audioOnlyRenderersFactory(
        wearMedia3Factory: WearMedia3Factory,
        audioSink: DefaultAudioSink,
        mediaCodecSelector: MediaCodecSelector,
    ): RenderersFactory = wearMedia3Factory.audioOnlyRenderersFactory(audioSink, mediaCodecSelector)

    @ServiceScoped
    @Provides
    fun defaultAnalyticsCollector(
        logger: ErrorReporter,
    ): AnalyticsCollector = DefaultAnalyticsCollector(Clock.DEFAULT).apply {
        addListener(AnalyticsEventLogger(logger))
    }

    @ServiceScoped
    @Provides
    fun extractorsFactory(): ExtractorsFactory = DefaultExtractorsFactory().setMp3ExtractorFlags(
        Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)

    @ServiceScoped
    @Provides
    fun transferListener(
        logger: ErrorReporter,
    ) = TransferListener(logger)

    @ServiceScoped
    @Provides
    fun mediaSourceFactory(
        @ApplicationContext context: Context,
    ): MediaSource.Factory = DefaultMediaSourceFactory(context)

    @SuppressSpeakerPlayback
    @ServiceScoped
    @Provides
    fun suppressSpeakerPlayback(
        @IsEmulator isEmulator: Boolean,
    ) = !isEmulator

    @ServiceScoped
    @Provides
    fun exoPlayer(
        service: Service,
        loadControl: LoadControl,
        audioOnlyRenderersFactory: RenderersFactory,
        analyticsCollector: AnalyticsCollector,
        mediaSourceFactory: MediaSource.Factory,
        @SuppressSpeakerPlayback suppressSpeakerPlayback: Boolean,
        audioOffloadManager: AudioOffloadManager,
        serviceCoroutineScope: CoroutineScope,
        @IsSamsungDevice isSamsungDevice: Boolean,
        dataUpdates: DataUpdates,
    ): Player = ExoPlayer.Builder(service, audioOnlyRenderersFactory)
        .setAnalyticsCollector(analyticsCollector)
        .setMediaSourceFactory(mediaSourceFactory)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setLoadControl(loadControl)
        .setSeekForwardIncrementMs(10_000)
        .setSeekBackIncrementMs(10_000)
        .setSuppressPlaybackOnUnsuitableOutput(suppressSpeakerPlayback)
        .setStuckBufferingDetectionTimeoutMs(1000)
        .build().apply {
            addListener(analyticsCollector)
            addListener(dataUpdates.listener)
            addListener(WearUnsuitableOutputPlaybackSuppressionResolverListener(service))
            addListener(TracingListener())
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setAudioOffloadPreferences(
                    AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(
                            if (isSamsungDevice) {
                                AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                            } else {
                                AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_REQUIRED
                            }
                        )
                        .setIsSpeedChangeSupportRequired(false)
                        .setIsGaplessSupportRequired(false)
                        .build(),
                )
                .build()
            serviceCoroutineScope.launch {
                audioOffloadManager.connect(this@apply)
            }
        }

    @ServiceScoped
    @Provides
    fun serviceCoroutineScope(
        service: Service,
    ): CoroutineScope = (service as LifecycleOwner).lifecycleScope

    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        serviceCoroutineScope: CoroutineScope,
        logger: ErrorReporter,
    ): MediaLibraryService.MediaLibrarySession.Callback =
        LibroMediaLibrarySessionCallback(serviceCoroutineScope, logger)

    @ServiceScoped
    @Provides
    fun mediaLibrarySession(
        service: Service,
        player: Player,
        playerProgressPersister: PlayerProgressPersister,
        librarySessionCallback: MediaLibraryService.MediaLibrarySession.Callback,
        intentBuilder: IntentBuilder,
    ): MediaLibrarySession =
        MediaLibrarySession.Builder(
            service as MediaLibraryService,
            player,
            librarySessionCallback,
        )
            .setSessionActivity(intentBuilder.buildPlayerIntent())
            .build().also {
                (service as LifecycleOwner).lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            playerProgressPersister.flush()
                            it.release()
                        }
                    },
                )
            }

    @ServiceScoped
    @Provides
    fun playbackProgressDao(
        db: AppDatabase,
    ): PlaybackProgressDao = db.playbackProgressDao()

    @ServiceScoped
    @Provides
    fun playerProgressPersister(
        player: Player,
        playbackProgressDao: PlaybackProgressDao,
        @ForApplicationScope applicationScope: CoroutineScope,
    ): PlayerProgressPersister = PlayerProgressPersister(
        player = player,
        playbackProgressDao = playbackProgressDao,
        scope = applicationScope,
    ).also {
        player.addListener(it)
    }

    @ServiceScoped
    @Provides
    fun audioSink(
        wearMedia3Factory: WearMedia3Factory,
        audioOffloadListener: AudioOffloadListener,
        service: Service,
    ): DefaultAudioSink = wearMedia3Factory.audioSink(
        audioOffloadListener = audioOffloadListener,
    ).also { audioSink ->
        if (service is LifecycleOwner) {
            service.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        audioSink.reset()
                    }
                },
            )
        }
    }
}
