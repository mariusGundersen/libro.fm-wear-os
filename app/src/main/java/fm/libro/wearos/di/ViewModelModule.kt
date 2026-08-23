package fm.libro.wearos.di

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.data.mapper.MediaExtrasMapperNoopImpl
import com.google.android.horologist.media.data.mapper.MediaItemExtrasMapperNoopImpl
import com.google.android.horologist.media.data.mapper.MediaItemMapper
import com.google.android.horologist.media.data.mapper.MediaMapper
import com.google.android.horologist.media.data.mapper.PlaybackStateMapper
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.android.horologist.media3.flows.buildSuspend
import fm.libro.wearos.player.PlaybackService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Module
@InstallIn(ActivityRetainedComponent::class)
object ViewModelModule {

    @Provides
    fun providesCoroutineScope(
        activityRetainedLifecycle: ActivityRetainedLifecycle,
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            activityRetainedLifecycle.addOnClearedListener {
                it.cancel()
            }
        }
    }

    @Provides
    fun mediaBrowser(
        @ApplicationContext application: Context,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
        coroutineScope: CoroutineScope,
    ): Deferred<MediaBrowser> =
        coroutineScope.async {
            MediaBrowser.Builder(
                application,
                SessionToken(application, ComponentName(application, PlaybackService::class.java)),
            ).buildSuspend()
        }.also {
            activityRetainedLifecycle.addOnClearedListener {
                it.cancel()
                if (it.isCompleted && !it.isCancelled) {
                    it.getCompleted().release()
                }
            }
        }

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    fun playerRepositoryImpl(
        mediaMapper: MediaMapper,
        mediaItemMapper: MediaItemMapper,
        playbackStateMapper: PlaybackStateMapper,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
        coroutineScope: CoroutineScope,
        mediaBrowser: Deferred<MediaBrowser>,
    ): PlayerRepositoryImpl =
        PlayerRepositoryImpl(
            mediaMapper = mediaMapper,
            mediaItemMapper = mediaItemMapper,
            playbackStateMapper = playbackStateMapper,
        ).also { playerRepository ->
            activityRetainedLifecycle.addOnClearedListener {
                playerRepository.close()
            }

            coroutineScope.launch(Dispatchers.Main) {
                val player = mediaBrowser.await()
                playerRepository.connect(
                    player = player,
                    onClose = player::release,
                )
            }
        }

    @Provides
    fun playerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl,
    ): PlayerRepository = playerRepositoryImpl

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    fun mediaItemMapper(): MediaItemMapper = MediaItemMapper(MediaItemExtrasMapperNoopImpl)

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    fun mediaMapper(): MediaMapper = MediaMapper(MediaExtrasMapperNoopImpl)

    @Provides
    fun playbackStateMapper(): PlaybackStateMapper = PlaybackStateMapper()
}
