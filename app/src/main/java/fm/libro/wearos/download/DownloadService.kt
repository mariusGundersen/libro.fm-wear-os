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
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()

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
                val format = intent.getStringExtra(EXTRA_FORMAT) ?: "mp3"
                val trackCount = intent.getIntExtra(EXTRA_TRACK_COUNT, 0)

                startDownload(isbn, title, author, coverUrl, duration, format, trackCount)
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
        format: String,
        trackCount: Int,
    ) {
        scope.launch {
            try {
                val authManager = AuthManager(applicationContext)
                val token = authManager.token ?: throw Exception("Not logged in")
                val manifest = LibroFmClient.getDownloadManifest(token, isbn)

                val dir = filesDir.resolve("audiobooks").resolve(isbn)
                dir.mkdirs()

                val isM4b = manifest.version == "m4b" && manifest.parts.isNotEmpty()

                if (isM4b) {
                    val part = manifest.parts.first()
                    val file = dir.resolve("$isbn.m4b")
                    updateNotification("Downloading $title...", 0)
                    downloadFile(part.url, file) { progress ->
                        updateNotification("Downloading $title...", progress)
                    }
                    db().downloadedBookDao().insert(
                        DownloadedBookEntity(
                            isbn = isbn,
                            title = title,
                            author = author,
                            coverUrl = coverUrl,
                            format = "m4b",
                            filePath = file.absolutePath,
                            fileSizeBytes = file.length(),
                            durationSeconds = duration,
                            trackCount = trackCount,
                            tracksJson = "[]",
                            downloadedAt = System.currentTimeMillis(),
                        )
                    )
                } else {
                    manifest.parts.forEachIndexed { index, part ->
                        val zipFile = dir.resolve("part_${index + 1}.zip")
                        updateNotification(
                            "Downloading $title (part ${index + 1}/${manifest.parts.size})...",
                            0,
                        )
                        downloadFile(part.url, zipFile) { partProgress ->
                            val totalProgress = ((index.toFloat() / manifest.parts.size) +
                                (partProgress.toFloat() / manifest.parts.size / 100f)) * 100f
                            updateNotification("Downloading $title...", totalProgress.toInt())
                        }
                        extractZip(zipFile, dir)
                        zipFile.delete()
                    }
                    val mp3Files = dir.listFiles()
                        ?.filter { it.extension == "mp3" }
                        ?.sorted() ?: emptyList()
                    val totalSize = mp3Files.sumOf { it.length() }

                    db().downloadedBookDao().insert(
                        DownloadedBookEntity(
                            isbn = isbn,
                            title = title,
                            author = author,
                            coverUrl = coverUrl,
                            format = "mp3",
                            filePath = dir.absolutePath,
                            fileSizeBytes = totalSize,
                            durationSeconds = duration,
                            trackCount = mp3Files.size,
                            tracksJson = "[]",
                            downloadedAt = System.currentTimeMillis(),
                        )
                    )
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

    private fun downloadFile(
        url: String,
        file: File,
        onProgress: (Int) -> Unit,
    ) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw Exception("Empty response")
        val contentLength = body.contentLength()

        body.byteStream().use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (contentLength > 0) {
                        onProgress((totalRead * 100 / contentLength).toInt())
                    }
                }
            }
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = targetDir.resolve(entry.name)
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun db() = AppDatabase.getInstance(applicationContext)

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
        const val EXTRA_FORMAT = "format"
        const val EXTRA_TRACK_COUNT = "track_count"
        const val CHANNEL_ID = "audiobook_downloads"
        const val NOTIFICATION_ID = 1002
    }
}
