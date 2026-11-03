package fm.libro.wearos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackProgressDao {

    @Query("SELECT * FROM track_progress WHERE isbn = :isbn ORDER BY trackIndex")
    fun getAllForIsbn(isbn: String): Flow<List<TrackProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: TrackProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<TrackProgressEntity>)

    @Query("DELETE FROM track_progress WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)
}
