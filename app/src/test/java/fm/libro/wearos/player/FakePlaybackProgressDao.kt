package fm.libro.wearos.player

import fm.libro.wearos.data.PlaybackProgressDao
import fm.libro.wearos.data.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePlaybackProgressDao : PlaybackProgressDao {

    val writes = mutableListOf<PlaybackProgressEntity>()

    override suspend fun getByIsbn(isbn: String): PlaybackProgressEntity? =
        writes.lastOrNull { it.isbn == isbn }

    override fun getByIsbnFlow(isbn: String): Flow<PlaybackProgressEntity?> =
        flowOf(writes.lastOrNull { it.isbn == isbn })

    override suspend fun upsert(progress: PlaybackProgressEntity) {
        writes += progress
    }

    override suspend fun deleteByIsbn(isbn: String) {
        writes.removeAll { it.isbn == isbn }
    }

    fun lastWriteFor(isbn: String): PlaybackProgressEntity? =
        writes.lastOrNull { it.isbn == isbn }
}