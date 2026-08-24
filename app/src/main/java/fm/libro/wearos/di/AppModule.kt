package fm.libro.wearos.di

import android.content.Context
import android.os.Vibrator
import androidx.work.WorkManager
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media.ui.snackbar.SnackbarManager
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fm.libro.wearos.auth.AuthManager
import okhttp3.Call
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun snackbarManager(): SnackbarManager = SnackbarManager()

    @Provides
    @Singleton
    fun vibrator(
        @ApplicationContext context: Context,
    ): Vibrator = context.getSystemService(Vibrator::class.java)

    @Provides
    @Singleton
    fun systemAudioRepository(
        @ApplicationContext context: Context,
    ): SystemAudioRepository = SystemAudioRepository.fromContext(context)

    @Provides
    @Singleton
    fun workManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun authManager(
        @ApplicationContext context: Context,
    ): AuthManager = AuthManager(context)

    @Provides
    @Singleton
    fun imageLoader(
        @ApplicationContext context: Context,
        callFactory: Call.Factory,
    ): ImageLoader = ImageLoader.Builder(context)
        .crossfade(false)
        .respectCacheHeaders(false)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .callFactory {
            NetworkAwareCallFactory(callFactory, RequestType.ImageRequest)
        }
        .build()
}
