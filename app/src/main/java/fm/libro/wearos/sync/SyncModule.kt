package fm.libro.wearos.sync

import com.google.android.horologist.media.sync.api.ChangeListVersionRepository
import com.google.android.horologist.media.sync.api.CoroutineDispatcherProvider
import com.google.android.horologist.media.sync.api.NotificationConfigurationProvider
import com.google.android.horologist.media.sync.api.Syncable
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatcherProvider(): CoroutineDispatcherProvider =
        object : CoroutineDispatcherProvider {
            override fun getIODispatcher() = Dispatchers.IO
        }

    @Provides
    @Singleton
    fun provideNotificationConfigurationProvider(): NotificationConfigurationProvider =
        object : NotificationConfigurationProvider {
            override fun getNotificationTitle() = "Libro.fm"
            override fun getNotificationIcon() = android.R.drawable.ic_media_play
            override fun getChannelName() = "Sync"
            override fun getChannelDescription() = "Synchronizing audiobook data"
        }

    @Provides
    @Singleton
    fun provideChangeListVersionRepository(): ChangeListVersionRepository =
        object : ChangeListVersionRepository {
            private val versions = mutableMapOf<String, Int>()
            override suspend fun getChangeListVersion(model: String) = versions[model] ?: 0
            override suspend fun updateChangeListVersion(model: String, newVersion: Int) {
                versions[model] = newVersion
            }
        }

    @Provides
    @Singleton
    fun provideSyncables(): Array<Syncable> = emptyArray()
}
