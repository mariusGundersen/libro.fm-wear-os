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
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.ui.WearUnsuitableOutputPlaybackSuppressionResolverListener
import com.google.android.horologist.media3.config.WearMedia3Factory
import com.google.android.horologist.media3.logging.AnalyticsEventLogger
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.logging.TransferListener
import com.google.android.horologist.media3.navigation.IntentBuilder
import com.google.android.horologist.media3.tracing.TracingListener
import fm.libro.wearos.player.LibroMediaLibrarySessionCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope

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
    fun extractorsFactory(): ExtractorsFactory = DefaultExtractorsFactory()

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
    fun suppressSpeakerPlayback(): Boolean = true

    @ServiceScoped
    @Provides
    fun exoPlayer(
        service: Service,
        loadControl: LoadControl,
        audioOnlyRenderersFactory: RenderersFactory,
        analyticsCollector: AnalyticsCollector,
        mediaSourceFactory: MediaSource.Factory,
        @SuppressSpeakerPlayback suppressSpeakerPlayback: Boolean,
    ): Player = ExoPlayer.Builder(service, audioOnlyRenderersFactory)
        .setAnalyticsCollector(analyticsCollector)
        .setMediaSourceFactory(mediaSourceFactory)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setLoadControl(loadControl)
        .setSeekForwardIncrementMs(10_000)
        .setSeekBackIncrementMs(10_000)
        .setSuppressPlaybackOnUnsuitableOutput(suppressSpeakerPlayback)
        .build().apply {
            addListener(analyticsCollector)
            addListener(WearUnsuitableOutputPlaybackSuppressionResolverListener(service))
            addListener(TracingListener())
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setAudioOffloadPreferences(
                    AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                        .setIsSpeedChangeSupportRequired(false)
                        .setIsGaplessSupportRequired(false)
                        .build(),
                )
                .build()
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
                            it.release()
                        }
                    },
                )
            }

    @ServiceScoped
    @Provides
    fun audioSink(
        wearMedia3Factory: WearMedia3Factory,
        service: Service,
    ): DefaultAudioSink = wearMedia3Factory.audioSink(
        audioOffloadListener = null,
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
