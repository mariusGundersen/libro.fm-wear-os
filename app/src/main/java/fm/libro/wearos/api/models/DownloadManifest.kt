package fm.libro.wearos.api.models

import com.google.gson.annotations.SerializedName

data class DownloadManifest(
    val isbn: String,
    val parts: List<DownloadPart>,
    val tracks: List<DownloadTrack>,
    @SerializedName("expires_at") val expiresAt: String,
    val version: String,
    @SerializedName("size_bytes") val sizeBytes: Long,
)

data class DownloadPart(
    val url: String,
    @SerializedName("size_bytes") val sizeBytes: Long,
)

data class DownloadTrack(
    val number: Int,
    @SerializedName("length_sec") val lengthSec: Int,
    @SerializedName("chapter_title") val chapterTitle: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
)
