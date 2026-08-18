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
import fm.libro.wearos.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val downloadedIsbns: Set<String> = emptySet(),
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

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
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            db.downloadedBookDao().getAll().collect { downloaded ->
                _uiState.value = _uiState.value.copy(
                    downloadedIsbns = downloaded.map { it.isbn }.toSet(),
                )
            }
        }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = LibraryUiState()
    }
}
