package fm.libro.wearos.download

import android.content.Context
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.DownloadManifest
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import fm.libro.wearos.data.StoredTrack
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

class DownloadManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val client = OkHttpClient()
    private val gson = Gson()

    private fun tracksJson(manifest: DownloadManifest): String {
        val stored = manifest.tracks.map {
            StoredTrack(number = it.number, lengthSec = it.lengthSec, chapterTitle = it.chapterTitle)
        }
        return gson.toJson(stored)
    }

    suspend fun downloadAudiobook(
        book: Audiobook,
        manifest: DownloadManifest,
        onProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val isbn = book.isbn
        val dir = context.filesDir.resolve("audiobooks").resolve(isbn)
        dir.mkdirs()

        val coverFile = dir.resolve("cover.jpg")
        if (book.coverUrl != null && !coverFile.exists()) {
            try {
                downloadFile("https:${book.coverUrl}", coverFile) {}
            } catch (_: Exception) {
            }
        }
        val coverLocalPath = if (coverFile.exists()) coverFile.absolutePath else null

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
                    coverLocalPath = coverLocalPath,
                    format = "m4b",
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    durationSeconds = book.durationSeconds,
                    trackCount = manifest.tracks.size,
                    tracksJson = tracksJson(manifest),
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        } else {
            var previousProgress = -1.0f
            manifest.parts.forEachIndexed { index, part ->
                val zipFile = dir.resolve("part_${index + 1}.zip")
                downloadFile(part.url, zipFile) { partProgress ->
                    val totalProgress = ((index.toFloat() / manifest.parts.size) +
                        (partProgress.toFloat() / manifest.parts.size / 100f)) * 100f
                    if(totalProgress > previousProgress) {
                        previousProgress = totalProgress
                        onProgress(totalProgress.toInt())
                    }
                }
                extractZip(zipFile, dir)
                zipFile.delete()
            }
            val mp3Files = dir.listFiles()?.filter { it.extension == "mp3" }?.sorted() ?: emptyList()
            val totalSize = mp3Files.sumOf { it.length() }

            db.downloadedBookDao().insert(
                DownloadedBookEntity(
                    isbn = isbn,
                    title = book.title,
                    author = book.authorString,
                    coverUrl = book.coverUrl,
                    coverLocalPath = coverLocalPath,
                    format = "mp3",
                    filePath = dir.absolutePath,
                    fileSizeBytes = totalSize,
                    durationSeconds = book.durationSeconds,
                    trackCount = mp3Files.size,
                    tracksJson = tracksJson(manifest),
                    downloadedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun deleteBook(isbn: String) = withContext(Dispatchers.IO) {
        val entity = db.downloadedBookDao().getByIsbn(isbn) ?: return@withContext
        val file = File(entity.filePath)
        val dir = if (file.isDirectory) file else file.parentFile
        dir?.deleteRecursively()
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
}
