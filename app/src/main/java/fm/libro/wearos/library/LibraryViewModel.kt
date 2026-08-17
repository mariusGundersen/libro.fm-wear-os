package fm.libro.wearos.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fm.libro.wearos.api.LibroFmClient
import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Audiobook> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val downloadedIsbns: Set<String> = emptySet(),
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
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

    fun loadLibrary() {
        val token = authManager.token ?: run {
            _uiState.value = _uiState.value.copy(error = "Not logged in")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val books = LibroFmClient.getAllLibraryBooks(token)
                _uiState.value = _uiState.value.copy(
                    books = books,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load library: ${e.message}",
                )
            }
        }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = LibraryUiState()
    }
}
