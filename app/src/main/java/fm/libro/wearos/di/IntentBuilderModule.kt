package fm.libro.wearos.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.horologist.media3.navigation.IntentBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fm.libro.wearos.MainActivity
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntentBuilderModule {
    @Provides
    @Singleton
    fun intentBuilder(
        @ApplicationContext context: Context,
    ): IntentBuilder = object : IntentBuilder {
        override fun buildDownloadIntent(): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        override fun buildPlayerIntent(): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
