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
import java.util.zip.ZipInputStream

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
                val zipFile = dir.resolve("part_${index + 1}.zip")
                downloadFile(part.url, zipFile) { partProgress ->
                    val totalProgress = ((index.toFloat() / manifest.parts.size) +
                        (partProgress.toFloat() / manifest.parts.size / 100f)) * 100f
                    onProgress(totalProgress.toInt())
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
                    format = "mp3",
                    filePath = dir.absolutePath,
                    fileSizeBytes = totalSize,
                    durationSeconds = book.durationSeconds,
                    trackCount = mp3Files.size,
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
