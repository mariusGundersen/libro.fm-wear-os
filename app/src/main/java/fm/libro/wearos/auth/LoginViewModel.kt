package fm.libro.wearos.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fm.libro.wearos.api.LibroFmClient
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val authManager: AuthManager,
    ) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val isLoggedIn: Boolean get() = authManager.isLoggedIn

    fun login(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            error = "Email and password required"
            return
        }

        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val response = LibroFmClient.login(email, password)
                authManager.saveLogin(
                    token = response.accessToken,
                    createdAt = response.createdAt,
                    username = email,
                    password = password,
                )
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                isLoading = false
                error = "Login failed: ${e.message}"
            }
        }
    }
}
