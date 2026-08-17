package fm.libro.wearos.download

import android.content.Context
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.DownloadManifest
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class DownloadManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val client = OkHttpClient()

    suspend fun downloadAudiobook(
        book: Audiobook,
        manifest: DownloadManifest,
        onProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val isbn = book.isbn
        val dir = context.filesDir.resolve("audiobooks").resolve(isbn)
        dir.mkdirs()

        val isM4b = manifest.version == "m4b" && manifest.parts.isNotEmpty()

        if (isM4b) {
            val part = manifest.parts.first()
            val file = dir.resolve("$isbn.m4b")
            downloadFile(part.url, file, onProgress)

            db.downloadedBookDao().insert(
                DownloadedBookEntity(
                    isbn = isbn,
                    title = book.title,
                    author = book.authorString,
                    coverUrl = book.coverUrl,
                    format = "m4b",
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    durationSeconds = book.durationSeconds,
                    trackCount = manifest.tracks.size,
                    tracksJson = "[]",
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        } else {
            manifest.parts.forEachIndexed { index, part ->
                val file = dir.resolve("track_${index + 1}.mp3")
                downloadFile(part.url, file) { partProgress ->
                    val totalProgress = ((index.toFloat() / manifest.parts.size) +
                        (partProgress.toFloat() / manifest.parts.size / 100f)) * 100f
                    onProgress(totalProgress.toInt())
                }
            }
            val totalSize = dir.listFiles()?.sumOf { it.length() } ?: 0L

            db.downloadedBookDao().insert(
                DownloadedBookEntity(
                    isbn = isbn,
                    title = book.title,
                    author = book.authorString,
                    coverUrl = book.coverUrl,
                    format = "mp3",
                    filePath = dir.absolutePath,
                    fileSizeBytes = totalSize,
                    durationSeconds = book.durationSeconds,
                    trackCount = manifest.parts.size,
                    tracksJson = "[]",
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun deleteBook(isbn: String) = withContext(Dispatchers.IO) {
        val entity = db.downloadedBookDao().getByIsbn(isbn) ?: return@withContext
        val file = File(entity.filePath)
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
        db.downloadedBookDao().deleteByIsbn(isbn)
        db.playbackProgressDao().deleteByIsbn(isbn)
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
}
