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
import fm.libro.wearos.library.BookStore
import fm.libro.wearos.library.DownloadedBooksViewModel
import fm.libro.wearos.library.LibraryScreen
import fm.libro.wearos.library.LibraryViewModel
import fm.libro.wearos.library.LibroBrowseScreen
import fm.libro.wearos.library.PlaylistScreen
import fm.libro.wearos.library.PlaylistViewModel
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
    const val PLAYLIST = "playlist"

    fun bookDetail(isbn: String) = "book/$isbn"
}

@Composable
fun AppNavGraph(
    authManager: AuthManager,
    bookStore: BookStore,
    navController: androidx.navigation.NavHostController,
    volumeViewModel: LibroVolumeViewModel,
) {
    val snackbarViewModel: LibroSnackbarViewModel = hiltViewModel()

    MediaPlayerScaffold(
        snackbarViewModel = snackbarViewModel,
        volumeViewModel = volumeViewModel,
        playerScreen = {
            val playerViewModel: LibroPlayerViewModel = hiltViewModel()
            LibroMediaPlayerScreen(
                playerViewModel = playerViewModel,
                volumeViewModel = volumeViewModel,
                onChaptersClick = { navController.navigate(Routes.PLAYLIST) },
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
                    onPlaylist = { navController.navigate(Routes.PLAYLIST) },
                )
            }

            composable(Routes.LIBRARY) {
                val viewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    viewModel = viewModel,
                    onBookClick = { isbn, book ->
                        bookStore.put(book)
                        navController.navigate(Routes.bookDetail(isbn))
                    },
                )
            }

            composable(Routes.PLAYLIST) {
                val viewModel: PlaylistViewModel = hiltViewModel()
                PlaylistScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onTrackSelected = { navController.navigateToPlayer() },
                )
            }
        },
    )
}
