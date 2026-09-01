package fm.libro.wearos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedBookDao {

    @Transaction
    @Query(
        """
        SELECT 
            b.*, 
            p.isbn AS progress_isbn, 
            p.trackIndex AS progress_trackIndex, 
            p.positionMs AS progress_positionMs,
            p.playbackSpeed AS progress_playbackSpeed, 
            p.updatedAt AS progress_updatedAt
        FROM downloaded_books b
        LEFT JOIN playback_progress p ON p.isbn = b.isbn
        ORDER BY b.downloadedAt DESC
        """
    )
    fun getAllWithProgress(): Flow<List<DownloadedBookWithProgress>>

    @Query("SELECT * FROM downloaded_books ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<DownloadedBookEntity>>

    @Query("SELECT * FROM downloaded_books WHERE isbn = :isbn")
    suspend fun getByIsbn(isbn: String): DownloadedBookEntity?

    @Query("SELECT * FROM downloaded_books WHERE isbn = :isbn")
    fun getByIsbnFlow(isbn: String): Flow<DownloadedBookEntity?>

    @Transaction
    @Query(
        """
        SELECT 
            b.*, 
            p.isbn AS progress_isbn, 
            p.trackIndex AS progress_trackIndex, 
            p.positionMs AS progress_positionMs,
            p.playbackSpeed AS progress_playbackSpeed, 
            p.updatedAt AS progress_updatedAt
        FROM downloaded_books b
        LEFT JOIN playback_progress p ON p.isbn = b.isbn
        WHERE b.isbn = :isbn
        """
    )
    fun getByIsbnWithProgress(isbn: String): Flow<DownloadedBookWithProgress>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: DownloadedBookEntity)

    @Delete
    suspend fun delete(book: DownloadedBookEntity)

    @Query("DELETE FROM downloaded_books WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)
}
