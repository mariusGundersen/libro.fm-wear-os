package fm.libro.wearos.di

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.horologist.media3.navigation.IntentBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fm.libro.wearos.MainActivity
import fm.libro.wearos.complication.DataUpdates
import fm.libro.wearos.complication.MediaStatusComplicationService
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

    @Provides
    @Singleton
    fun dataUpdates(
        @ApplicationContext application: Context,
    ): DataUpdates {
        val updater = ComplicationDataSourceUpdateRequester.create(
            application,
            ComponentName(application, MediaStatusComplicationService::class.java),
        )
        return DataUpdates(updater)
    }
}
