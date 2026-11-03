package fm.libro.wearos.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.composable
import com.google.android.horologist.media.ui.navigation.MediaNavController.navigateToPlayer
import com.google.android.horologist.media.ui.navigation.MediaPlayerScaffold
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.auth.LoginScreen
import fm.libro.wearos.auth.LoginViewModel
import fm.libro.wearos.library.BookDetailScreen
import fm.libro.wearos.library.BookDetailViewModel
import fm.libro.wearos.library.ChaptersScreen
import fm.libro.wearos.library.ChaptersViewModel
import fm.libro.wearos.library.DownloadedBooksViewModel
import fm.libro.wearos.library.LibraryScreen
import fm.libro.wearos.library.LibraryViewModel
import fm.libro.wearos.library.LibroBrowseScreen
import fm.libro.wearos.player.LibroMediaPlayerScreen
import fm.libro.wearos.player.LibroPlayerViewModel
import fm.libro.wearos.player.LibroSnackbarViewModel
import fm.libro.wearos.player.LibroVolumeViewModel
import fm.libro.wearos.settings.SettingsScreen
import fm.libro.wearos.settings.SettingsViewModel

object Routes {
    const val LOGIN = "login"
    const val LIBRARY = "library"
    const val BOOK_DETAIL = "book/{isbn}"
    const val CHAPTERS = "book/{isbn}/chapters"

    fun bookDetail(isbn: String) = "book/$isbn"
    fun bookChapters(isbn: String) = "book/$isbn/chapters"
}

@Composable
fun AppNavGraph(
    authManager: AuthManager,
    navController: androidx.navigation.NavHostController,
    volumeViewModel: LibroVolumeViewModel,
) {
    val snackbarViewModel: LibroSnackbarViewModel = hiltViewModel()
    val playerViewModel: LibroPlayerViewModel = hiltViewModel()

    MediaPlayerScaffold(
        snackbarViewModel = snackbarViewModel,
        volumeViewModel = volumeViewModel,
        playerScreen = {
            LibroMediaPlayerScreen(
                playerViewModel = playerViewModel,
                volumeViewModel = volumeViewModel,
                onChaptersClick = { isbn -> navController.navigate(Routes.bookChapters(isbn)) },
            )
        },
        libraryScreen = {
            val downloadedBooksViewModel: DownloadedBooksViewModel = hiltViewModel()
            LibroBrowseScreen(
                viewModel = downloadedBooksViewModel,
                onBookClick = { isbn ->
                    navController.navigate(Routes.bookDetail(isbn))
                },
                onBrowseAllClick = {
                    if (authManager.isLoggedIn) {
                        navController.navigate(Routes.LIBRARY)
                    } else {
                        navController.navigate(Routes.LOGIN)
                    }
                },
            )
        },
        categoryEntityScreen = { },
        mediaEntityScreen = { },
        playlistsScreen = { },
        settingsScreen = {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = viewModel)
        },
        deepLinkPrefix = "librofm",
        navController = navController,
        additionalNavRoutes = {
            composable(Routes.LOGIN) {
                val viewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.popBackStack()
                    },
                )
            }

            composable(
                Routes.BOOK_DETAIL,
                arguments = listOf(navArgument("isbn") { type = NavType.StringType }),
            ) {
                val viewModel: BookDetailViewModel = hiltViewModel()
                BookDetailScreen(
                    viewModel = viewModel,
                    onPlay = { navController.navigateToPlayer() },
                    onBack = { navController.popBackStack() },
                    onChapters = {isbn -> navController.navigate(Routes.bookChapters(isbn)) },
                )
            }

            composable(Routes.LIBRARY) {
                val viewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        navController.navigate(Routes.bookDetail(book.isbn))
                    },
                )
            }

            composable(
                Routes.CHAPTERS,
                arguments = listOf(navArgument("isbn") { type = NavType.StringType }),
            ) {
                val viewModel: ChaptersViewModel = hiltViewModel()
                ChaptersScreen(
                    viewModel = viewModel,
                    onTrackSelected = { navController.navigateToPlayer() },
                )
            }
        },
    )
}
