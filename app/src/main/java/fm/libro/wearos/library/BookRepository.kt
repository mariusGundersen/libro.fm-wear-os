package fm.libro.wearos.library

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import fm.libro.wearos.api.LibroFmApi
import fm.libro.wearos.api.LibraryPagingSource
import fm.libro.wearos.models.Audiobook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
    @Inject
    constructor(
        private val api: LibroFmApi,
    ) {
        private val books = ConcurrentHashMap<String, Audiobook>()

        fun getBook(isbn: String): Audiobook? = books[isbn]

        fun library(token: String?): Flow<PagingData<Audiobook>> =
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource(api, token ?: "") },
            ).flow.map { pagingData ->
                pagingData.map { apiBook ->
                    Audiobook.fromApi(apiBook).also { books[it.isbn] = it }
                }
            }
    }