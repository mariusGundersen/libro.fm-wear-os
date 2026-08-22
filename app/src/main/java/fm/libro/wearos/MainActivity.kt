package fm.libro.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.navigation.AppNavGraph
import fm.libro.wearos.ui.theme.LibroFmTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            LibroFmTheme {
                AppNavGraph(
                    authManager = authManager,
                    onLogout = { authManager.logout() },
                )
            }
        }
    }
}
