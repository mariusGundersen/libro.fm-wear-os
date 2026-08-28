package fm.libro.wearos.di

import android.os.Build
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.exoplayer.ExoPlayer.AudioOffloadListener
import com.google.android.horologist.media3.logging.ErrorReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fm.libro.wearos.offload.AudioOffloadListenerList
import fm.libro.wearos.offload.AudioOffloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OffloadModule {

    @Provides
    @Singleton
    fun audioOffloadListenerList(): AudioOffloadListenerList = AudioOffloadListenerList()

    @Provides
    @Singleton
    fun audioOffloadListener(
        listeners: AudioOffloadListenerList,
    ): AudioOffloadListener = listeners


    @Singleton
    @Provides
    @ForApplicationScope
    fun coroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun audioOffloadManager(
        errorReporter: ErrorReporter,
        audioOffloadListenerList: AudioOffloadListenerList,
        @ForApplicationScope coroutineScope: CoroutineScope,
    ): AudioOffloadManager {
        return AudioOffloadManager(errorReporter).also { manager ->
            if (Build.VERSION.SDK_INT >= 30) {
                audioOffloadListenerList.addListener(manager.audioOffloadListener)

                coroutineScope.launch {
                    manager.printDebugLogsLoop()
                }
            }
        }
    }
}
