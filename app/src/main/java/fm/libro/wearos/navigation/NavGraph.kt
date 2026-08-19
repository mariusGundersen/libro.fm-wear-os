package fm.libro.wearos.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import fm.libro.wearos.auth.AuthManager
import fm.libro.wearos.auth.LoginScreen
import fm.libro.wearos.auth.LoginViewModel
import fm.libro.wearos.library.BookDetailScreen
import fm.libro.wearos.library.BookDetailViewModel
import fm.libro.wearos.library.DownloadedBookDetailScreen
import fm.libro.wearos.library.DownloadedBookDetailViewModel
import fm.libro.wearos.library.DownloadedBooksScreen
import fm.libro.wearos.library.DownloadedBooksViewModel
import fm.libro.wearos.library.LibraryScreen
import fm.libro.wearos.library.LibraryViewModel
import fm.libro.wearos.player.PlayerScreen
import fm.libro.wearos.player.PlayerViewModel
import kotlin.getValue

object Routes {
    const val LOGIN = "login"
    const val DOWNLOADED = "downloaded"
    const val LIBRARY = "library"
    const val BOOK_DETAIL = "book/{isbn}"
    const val DOWNLOADED_BOOK_DETAIL = "downloaded_book/{isbn}"
    const val PLAYER = "player/{isbn}"

    fun bookDetail(isbn: String) = "book/$isbn"
    fun downloadedBookDetail(isbn: String) = "downloaded_book/$isbn"
    fun player(isbn: String) = "player/$isbn"
}

@Composable
fun AppNavGraph(
    authManager: AuthManager,
    onLogout: () -> Unit,
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = if (authManager.isLoggedIn) Routes.DOWNLOADED else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.DOWNLOADED) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.DOWNLOADED) {
            val viewModel: DownloadedBooksViewModel = viewModel()
            DownloadedBooksScreen(
                viewModel = viewModel,
                onBookClick = { isbn ->
                    navController.navigate(Routes.downloadedBookDetail(isbn))
                },
                onBrowseLibrary = {
                    navController.navigate(Routes.LIBRARY)
                },
            )
        }

        composable(Routes.LIBRARY) {
            val viewModel: LibraryViewModel = viewModel()
            LibraryScreen(
                viewModel = viewModel,
                onBookClick = { isbn, book ->
                    BookDetailViewModel.cacheBook(book)
                    navController.navigate(Routes.bookDetail(isbn))
                },
            )
        }

        composable(
            Routes.BOOK_DETAIL,
            arguments = listOf(navArgument("isbn") { type = NavType.StringType }),
        ) {
            val viewModel: BookDetailViewModel = viewModel()
            BookDetailScreen(
                viewModel = viewModel,
                onPlay = { isbn ->
                    navController.navigate(Routes.player(isbn))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.DOWNLOADED_BOOK_DETAIL,
            arguments = listOf(navArgument("isbn") { type = NavType.StringType }),
        ) {
            val viewModel: DownloadedBookDetailViewModel = viewModel()
            DownloadedBookDetailScreen(
                viewModel = viewModel,
                onPlay = { isbn ->
                    navController.navigate(Routes.player(isbn))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.PLAYER,
            arguments = listOf(navArgument("isbn") { type = NavType.StringType }),
        ) {
            val viewModel: PlayerViewModel = viewModel()
            PlayerScreen(viewModel = viewModel)
        }
    }
}
