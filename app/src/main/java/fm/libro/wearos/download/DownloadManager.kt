package fm.libro.wearos.download

import android.content.Context
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.DownloadManifest
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import fm.libro.wearos.data.PlaybackProgressEntity
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

    private fun tracksJson(manifest: DownloadManifest, files: List<File>): String {
        val stored = manifest.tracks.zip(files).map { (track, file) ->
            StoredTrack(number = track.number, lengthSec = track.lengthSec, chapterTitle = track.chapterTitle, filePath = file.absolutePath)
        }
        return gson.toJson(stored)
    }

    private fun narratorsJson(book: Audiobook): String =
        gson.toJson(book.audiobookInfo?.narrators ?: emptyList<String>())

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
                    tracksJson = tracksJson(manifest, manifest.tracks.map{ _ -> file}),
                    narratorsJson = narratorsJson(book),
                    downloadedAt = System.currentTimeMillis(),
                )
            )
            saveProgress(book)
        } else {
            val mp3Files = manifest.parts.flatMapIndexed { index, part ->
                downloadAndExtractZip(part.url, dir) { partProgress ->
                    val totalProgress = ((index.toFloat() / manifest.parts.size) +
                            (partProgress.toFloat() / manifest.parts.size / 100f)) * 100f
                    onProgress(totalProgress.toInt())
                }
            }
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
                    tracksJson = tracksJson(manifest, mp3Files),
                    narratorsJson = narratorsJson(book),
                    downloadedAt = System.currentTimeMillis(),
                )
            )
            saveProgress(book)
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

    private suspend fun saveProgress(book: Audiobook) {
        val meta = book.userMetadata ?: return
        val trackIndex = meta.trackIndex ?: return
        val trackSeconds = meta.trackSeconds ?: return
        if (trackIndex == 0 && trackSeconds == 0f) return
        db.playbackProgressDao().upsert(
            PlaybackProgressEntity(
                isbn = book.isbn,
                trackIndex = trackIndex,
                positionMs = (trackSeconds * 1000).toLong(),
                playbackSpeed = 1.0f,
                updatedAt = System.currentTimeMillis(),
            )
        )
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

    private fun downloadAndExtractZip(
        url: String,
        targetDir: File,
        onProgress: (Int) -> Unit,
    ): List<File> {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw Exception("Empty response")
        val contentLength = body.contentLength()
        val entries = ArrayList<File>();

        body.byteStream().use { rawInput ->
            val progressInput = ProgressInputStream(rawInput, contentLength, onProgress)
            ZipInputStream(progressInput).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = targetDir.resolve(entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        entries.add(outFile)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        return entries
    }

    private class ProgressInputStream(
        private val delegate: java.io.InputStream,
        private val totalBytes: Long,
        private val onProgress: (Int) -> Unit,
    ) : java.io.InputStream() {
        private var bytesRead = 0L
        private var lastReported = -1

        override fun read(): Int {
            val b = delegate.read()
            if (b != -1) {
                bytesRead++
                reportProgress()
            }
            return b
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val n = delegate.read(buffer, offset, length)
            if (n > 0) {
                bytesRead += n
                reportProgress()
            }
            return n
        }

        override fun available() = delegate.available()
        override fun close() = delegate.close()

        private fun reportProgress() {
            if (totalBytes > 0) {
                val progress = (bytesRead * 100 / totalBytes).toInt()
                if (progress > lastReported) {
                    lastReported = progress
                    onProgress(progress)
                }
            }
        }
    }
}
