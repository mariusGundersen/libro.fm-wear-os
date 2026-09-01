package fm.libro.wearos.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.models.Audiobook
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadedBooksViewModel
    @Inject
    constructor(
        private val db: AppDatabase,
        private val authManager: AuthManager,
    ) : ViewModel() {

    val downloadedBooks: StateFlow<List<Audiobook>> =
        db.downloadedBookDao().getAllWithProgress()
            .map { items -> items.map { Audiobook.fromDownloaded(it) } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val isAuthenticated = authManager.isLoggedIn

}
