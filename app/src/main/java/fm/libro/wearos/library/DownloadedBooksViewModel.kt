package fm.libro.wearos.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fm.libro.wearos.data.AppDatabase
import fm.libro.wearos.data.DownloadedBookEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DownloadedBooksViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    val downloadedBooks: StateFlow<List<DownloadedBookEntity>> =
        db.downloadedBookDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
