package fm.libro.wearos.player

import fm.libro.wearos.data.TrackProgressDao
import fm.libro.wearos.data.TrackProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTrackProgressDao : TrackProgressDao {

    val writes = mutableListOf<TrackProgressEntity>()

    override fun getAllForIsbn(isbn: String): Flow<List<TrackProgressEntity>> =
        flowOf(currentFor(isbn))

    override suspend fun upsert(progress: TrackProgressEntity) {
        writes += progress
    }

    override suspend fun upsertAll(progress: List<TrackProgressEntity>) {
        writes += progress
    }

    override suspend fun deleteByIsbn(isbn: String) {
        writes.removeAll { it.isbn == isbn }
    }

    fun lastWriteFor(isbn: String, trackIndex: Int): TrackProgressEntity? =
        writes.lastOrNull { it.isbn == isbn && it.trackIndex == trackIndex }

    private fun currentFor(isbn: String): List<TrackProgressEntity> =
        writes.filter { it.isbn == isbn }.distinctBy { it.trackIndex }
}
