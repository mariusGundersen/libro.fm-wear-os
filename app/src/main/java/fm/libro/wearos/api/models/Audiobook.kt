package fm.libro.wearos.api.models

import com.google.gson.annotations.SerializedName

data class Audiobook(
    val isbn: String,
    val title: String,
    val authors: Any?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("audiobook_info") val audiobookInfo: AudiobookInfo?,
    val series: String?,
    @SerializedName("series_num") val seriesNum: Int?,
    val publisher: String?,
    @SerializedName("publication_date") val publicationDate: String?,
    val description: String?,
    @SerializedName("user_metadata") val userMetadata: UserMetadata?,
) {
    val authorString: String
        get() = when (val a = authors) {
            is String -> a
            is List<*> -> a.filterIsInstance<String>().joinToString(", ")
            else -> "Unknown"
        }

    val durationSeconds: Int
        get() = audiobookInfo?.duration ?: 0

    val trackCount: Int
        get() = audiobookInfo?.trackCount ?: 0

    val progressSeconds: Int
        get() = userMetadata?.trackSeconds ?: 0
}

data class AudiobookInfo(
    val narrators: List<String>?,
    val duration: Int?,
    @SerializedName("size_bytes") val sizeBytes: Long?,
    @SerializedName("track_count") val trackCount: Int?,
    @SerializedName("parts_count") val partsCount: Int?,
    @SerializedName("audio_language") val audioLanguage: String?,
)

data class UserMetadata(
    @SerializedName("track_index") val trackIndex: Int?,
    @SerializedName("track_seconds") val trackSeconds: Int?,
    @SerializedName("finished") val finished: Boolean?,
    @SerializedName("added_at") val addedAt: String?,
)

data class LibraryMetadata(
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int,
    val audiobooks: List<Audiobook>,
    val tags: List<String>,
)
