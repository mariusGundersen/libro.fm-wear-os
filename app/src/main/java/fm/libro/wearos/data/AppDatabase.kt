package fm.libro.wearos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [
        DownloadedBookEntity::class,
        PlaybackProgressEntity::class,
        TrackProgressEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadedBookDao(): DownloadedBookDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun trackProgressDao(): TrackProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = Migration(
            1, 2,
            fun(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloaded_books ADD COLUMN coverLocalPath TEXT")
            }
        )

        private val MIGRATION_2_3 = Migration(
            2, 3,
            fun(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloaded_books ADD COLUMN narratorsJson TEXT NOT NULL DEFAULT '[]'")
            }
        )

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `track_progress` (" +
                        "`isbn` TEXT NOT NULL, " +
                        "`trackIndex` INTEGER NOT NULL, " +
                        "`positionMs` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`isbn`, `trackIndex`))"
                )
                backfillTrackProgress(db)
            }
        }

        private fun backfillTrackProgress(db: SupportSQLiteDatabase) {
            db.query(
                "SELECT p.isbn, p.trackIndex, p.positionMs, p.updatedAt, b.tracksJson " +
                    "FROM playback_progress p " +
                    "INNER JOIN downloaded_books b ON b.isbn = p.isbn"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val rows = trackProgressBackfill(
                        isbn = cursor.getString(0),
                        trackIndex = cursor.getInt(1),
                        positionMs = cursor.getLong(2),
                        updatedAt = cursor.getLong(3),
                        tracksJson = cursor.getString(4),
                    )
                    rows.forEach { row ->
                        db.execSQL(
                            "INSERT OR REPLACE INTO `track_progress` " +
                                "(`isbn`, `trackIndex`, `positionMs`, `updatedAt`) " +
                                "VALUES (?, ?, ?, ?)",
                            arrayOf(row.isbn, row.trackIndex, row.positionMs, row.updatedAt),
                        )
                    }
                }
            }
        }

        internal fun trackProgressBackfill(
            isbn: String,
            trackIndex: Int,
            positionMs: Long,
            updatedAt: Long,
            tracksJson: String?,
        ): List<TrackProgressEntity> {
            val tracks = try {
                Gson().fromJson<List<StoredTrack>>(
                    tracksJson,
                    object : TypeToken<List<StoredTrack>>() {}.type,
                ) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val rows = mutableListOf<TrackProgressEntity>()
            val completedCount = trackIndex.coerceIn(0, tracks.size)
            for (index in 0 until completedCount) {
                val lengthSec = tracks[index].lengthSec
                if (lengthSec > 0) {
                    rows += TrackProgressEntity(
                        isbn = isbn,
                        trackIndex = index,
                        positionMs = lengthSec * 1000L,
                        updatedAt = updatedAt,
                    )
                }
            }
            if (positionMs > 0 && (tracks.isEmpty() || trackIndex < tracks.size)) {
                rows += TrackProgressEntity(
                    isbn = isbn,
                    trackIndex = trackIndex,
                    positionMs = positionMs,
                    updatedAt = updatedAt,
                )
            }
            return rows
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "librofm.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
        }
    }
}
