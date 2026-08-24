package fm.libro.wearos.player

import com.google.android.horologist.media.ui.snackbar.SnackbarManager
import com.google.android.horologist.media.ui.snackbar.SnackbarViewModel as HorologistSnackbarViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LibroSnackbarViewModel
    @Inject
    constructor(
        snackbarManager: SnackbarManager,
    ) : HorologistSnackbarViewModel(snackbarManager)
