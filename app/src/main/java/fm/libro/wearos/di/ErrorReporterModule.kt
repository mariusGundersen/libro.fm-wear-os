package fm.libro.wearos.di

import android.util.Log
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media3.logging.ErrorReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ErrorReporterModule {
    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    @Singleton
    fun errorReporter(): ErrorReporter = object : ErrorReporter {
        override fun showMessage(message: Int) {
            Log.w("LibroFm", "Message resource: $message")
        }

        override fun logMessage(
            message: String,
            category: ErrorReporter.Category,
            level: ErrorReporter.Level,
        ) {
            Log.w("LibroFm", "[$category/$level] $message")
        }
    }
}
