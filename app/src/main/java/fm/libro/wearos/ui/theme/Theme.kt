package fm.libro.wearos.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme


@Composable
fun LibroFmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content,
    )
}
