package fm.libro.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.navigation.AppNavGraph
import fm.libro.wearos.ui.theme.LibroFmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authManager = AuthManager(this)

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
