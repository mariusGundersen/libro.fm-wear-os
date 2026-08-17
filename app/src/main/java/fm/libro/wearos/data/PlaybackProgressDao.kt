package fm.libro.wearos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress WHERE isbn = :isbn")
    suspend fun getByIsbn(isbn: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE isbn = :isbn")
    fun getByIsbnFlow(isbn: String): Flow<PlaybackProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)
}
