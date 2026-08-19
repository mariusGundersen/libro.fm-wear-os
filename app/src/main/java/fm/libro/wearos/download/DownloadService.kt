package fm.libro.wearos.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fm.libro.wearos.MainActivity
import fm.libro.wearos.api.LibroFmClient
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.auth.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing download..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val isbn = intent.getStringExtra(EXTRA_ISBN) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Audiobook"
                val author = intent.getStringExtra(EXTRA_AUTHOR) ?: ""
                val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
                val duration = intent.getIntExtra(EXTRA_DURATION, 0)
                startDownload(isbn, title, author, coverUrl, duration)
            }
            ACTION_STOP_DOWNLOAD -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(
        isbn: String,
        title: String,
        author: String,
        coverUrl: String?,
        duration: Int,
    ) {
        scope.launch {
            try {
                val authManager = AuthManager(applicationContext)
                val token = authManager.token ?: throw Exception("Not logged in")
                val manifest = LibroFmClient.getDownloadManifest(token, isbn)
                val book = Audiobook(
                    isbn = isbn,
                    title = title,
                    authors = author,
                    coverUrl = coverUrl,
                    audiobookInfo = null,
                    series = null,
                    seriesNum = null,
                    publisher = null,
                    publicationDate = null,
                    description = null,
                    userMetadata = null,
                )

                val downloadManager = DownloadManager(applicationContext)
                downloadManager.downloadAudiobook(book, manifest) { progress ->
                    updateNotification("Downloading $title...", progress)
                }

                updateNotification("Downloaded $title", 100)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            } catch (e: Exception) {
                updateNotification("Download failed: ${e.message}", 0)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audiobook Downloads",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Libro.fm")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Libro.fm")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_DOWNLOAD = "fm.libro.wearos.download.START"
        const val ACTION_STOP_DOWNLOAD = "fm.libro.wearos.download.STOP"
        const val EXTRA_ISBN = "isbn"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUTHOR = "author"
        const val EXTRA_COVER_URL = "cover_url"
        const val EXTRA_DURATION = "duration"
        const val CHANNEL_ID = "audiobook_downloads"
        const val NOTIFICATION_ID = 1002
    }
}
