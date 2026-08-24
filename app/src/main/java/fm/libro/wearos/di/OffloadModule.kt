package fm.libro.wearos.di

import android.os.Build
import androidx.media3.exoplayer.ExoPlayer.AudioOffloadListener
import com.google.android.horologist.media3.logging.ErrorReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fm.libro.wearos.offload.AudioOffloadListenerList
import fm.libro.wearos.offload.AudioOffloadManager
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

    @Provides
    @Singleton
    fun audioOffloadManager(
        errorReporter: ErrorReporter,
        audioOffloadListenerList: AudioOffloadListenerList,
    ): AudioOffloadManager = AudioOffloadManager(errorReporter).also { manager ->
        if (Build.VERSION.SDK_INT >= 30) {
            audioOffloadListenerList.addListener(manager.audioOffloadListener)
        }
    }
}
