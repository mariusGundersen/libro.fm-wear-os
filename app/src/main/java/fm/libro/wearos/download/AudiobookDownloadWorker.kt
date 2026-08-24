package fm.libro.wearos.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.AudiobookInfo
import fm.libro.wearos.api.models.DownloadManifest

@HiltWorker
class AudiobookDownloadWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
    ) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val isbn = inputData.getString(KEY_ISBN) ?: return Result.failure()
        val manifestJson = inputData.getString(KEY_MANIFEST_JSON) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val author = inputData.getString(KEY_AUTHOR) ?: return Result.failure()
        val coverUrl = inputData.getString(KEY_COVER_URL)
        val durationSeconds = inputData.getInt(KEY_DURATION_SECONDS, 0)

        val manifest = try {
            Gson().fromJson(manifestJson, DownloadManifest::class.java)
        } catch (_: Exception) {
            return Result.failure()
        }

        val book = Audiobook(
            isbn = isbn,
            title = title,
            authors = author,
            coverUrl = coverUrl,
            audiobookInfo = AudiobookInfo(
                narrators = null,
                duration = durationSeconds,
                sizeBytes = null,
                trackCount = manifest.tracks.size,
                partsCount = null,
                audioLanguage = null,
            ),
            series = null,
            seriesNum = null,
            publisher = null,
            publicationDate = null,
            description = null,
            userMetadata = null,
        )
        book.manifest = manifest

        createNotificationChannel()

        return try {
            setForeground(createForegroundInfo(book.title, 0))

            val downloadManager = DownloadManager(applicationContext)
            downloadManager.downloadAudiobook(book, manifest) { progress ->
                setForegroundAsync(createForegroundInfo(book.title, progress))
                setProgressAsync(
                    Data.Builder().putInt(KEY_PROGRESS, progress).build()
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure(
                Data.Builder().putString(KEY_ERROR, e.message).build()
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audiobook Downloads",
            NotificationManager.IMPORTANCE_LOW,
        )
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Libro.fm")
            .setContentText("Downloading $title...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_ISBN = "isbn"
        const val KEY_MANIFEST_JSON = "manifest_json"
        const val KEY_TITLE = "title"
        const val KEY_AUTHOR = "author"
        const val KEY_COVER_URL = "cover_url"
        const val KEY_DURATION_SECONDS = "duration_seconds"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val CHANNEL_ID = "audiobook_downloads"
        const val NOTIFICATION_ID = 1002
        const val WORK_NAME_PREFIX = "audiobook_download_"

        fun createInputData(
            isbn: String,
            manifest: DownloadManifest,
            title: String,
            author: String,
            coverUrl: String?,
            durationSeconds: Int,
        ): Data {
            return Data.Builder()
                .putString(KEY_ISBN, isbn)
                .putString(KEY_MANIFEST_JSON, Gson().toJson(manifest))
                .putString(KEY_TITLE, title)
                .putString(KEY_AUTHOR, author)
                .putString(KEY_COVER_URL, coverUrl)
                .putInt(KEY_DURATION_SECONDS, durationSeconds)
                .build()
        }
    }
}
