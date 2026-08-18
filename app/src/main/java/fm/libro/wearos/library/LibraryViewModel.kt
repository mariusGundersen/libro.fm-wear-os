package fm.libro.wearos.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import fm.libro.wearos.api.LibraryPagingSource
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.auth.AuthManager
import kotlinx.coroutines.flow.Flow

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)

    val books: Flow<PagingData<Audiobook>>

    init {
        val token = authManager.token
        books = if (token != null) {
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource(token) },
            ).flow.cachedIn(viewModelScope)
        } else {
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource("") },
            ).flow.cachedIn(viewModelScope)
        }
    }
}
