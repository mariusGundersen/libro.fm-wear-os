package fm.libro.wearos.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import dagger.hilt.android.lifecycle.HiltViewModel
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
        db: AppDatabase,
        authManager: AuthManager,
        bookRepository: BookRepository,
    ) : ViewModel() {

    val knownIsbns = db.downloadedBookDao().getAll().map { it.map { book -> book.isbn } }
    val books: Flow<PagingData<Audiobook>>

    init {
        val token = authManager.token
        books = bookRepository.library(token)
            .combineTransform(knownIsbns) { a, b ->
                emit(a.filter { book -> !b.contains(book.isbn) })
            }
            .cachedIn(viewModelScope)
    }
}
