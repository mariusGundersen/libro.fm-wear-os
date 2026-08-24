package fm.libro.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.navigation.AppNavGraph
import fm.libro.wearos.player.LibroVolumeViewModel
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
            val navController = rememberSwipeDismissableNavController()
            val volumeViewModel: LibroVolumeViewModel = hiltViewModel()

            LibroFmTheme {
                AppNavGraph(
                    authManager = authManager,
                    navController = navController,
                    volumeViewModel = volumeViewModel,
                )
            }
        }
    }
}
