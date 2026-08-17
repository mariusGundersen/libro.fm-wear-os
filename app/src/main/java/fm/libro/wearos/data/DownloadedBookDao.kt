package fm.libro.wearos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedBookDao {

    @Query("SELECT * FROM downloaded_books ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<DownloadedBookEntity>>

    @Query("SELECT * FROM downloaded_books WHERE isbn = :isbn")
    suspend fun getByIsbn(isbn: String): DownloadedBookEntity?

    @Query("SELECT * FROM downloaded_books WHERE isbn = :isbn")
    fun getByIsbnFlow(isbn: String): Flow<DownloadedBookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: DownloadedBookEntity)

    @Delete
    suspend fun delete(book: DownloadedBookEntity)

    @Query("DELETE FROM downloaded_books WHERE isbn = :isbn")
    suspend fun deleteByIsbn(isbn: String)
}
