package fm.libro.wearos.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadedBooksViewModel
    @Inject
    constructor(
        private val db: AppDatabase,
        private val authManager: AuthManager,
    ) : ViewModel() {

    val downloadedBooks: StateFlow<List<DownloadedBookEntity>> =
        db.downloadedBookDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAuthenticated = authManager.isLoggedIn
}
