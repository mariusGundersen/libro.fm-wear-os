package fm.libro.wearos.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.api.LibroFmApi
import fm.libro.wearos.api.LibraryPagingSource
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.auth.AuthManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val authManager: AuthManager,
        private val api: LibroFmApi,
    ) : ViewModel() {

    val books: Flow<PagingData<Audiobook>>

    init {
        val token = authManager.token
        books = if (token != null) {
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource(api, token) },
            ).flow.cachedIn(viewModelScope)
        } else {
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { LibraryPagingSource(api, "") },
            ).flow.cachedIn(viewModelScope)
        }
    }
}
