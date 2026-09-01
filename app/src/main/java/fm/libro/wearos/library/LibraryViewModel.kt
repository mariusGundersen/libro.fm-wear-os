package fm.libro.wearos.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.api.LibraryPagingSource
import fm.libro.wearos.api.LibroFmApi
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.models.Audiobook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val db: AppDatabase,
        authManager: AuthManager,
        private val api: LibroFmApi,
    ) : ViewModel() {

    val knownIsbns = db.downloadedBookDao().getAll().map { it.map { book -> book.isbn } }
    val books: Flow<PagingData<Audiobook>>

    init {
        val token = authManager.token
        books = if (token != null) {
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource(api, token) },
            ).flow
        } else {
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource(api, "") },
            ).flow
        }
            .combineTransform(knownIsbns) { a, b ->
                emit(a.filter { book -> !b.contains(book.isbn) })
            }
            .map { pagingData -> pagingData.map { Audiobook.fromApi(it) } }
            .cachedIn(viewModelScope)
    }
}
