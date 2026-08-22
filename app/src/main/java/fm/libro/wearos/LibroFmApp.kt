package fm.libro.wearos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import fm.libro.wearos.data.AppDatabase

@HiltAndroidApp
class LibroFmApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
