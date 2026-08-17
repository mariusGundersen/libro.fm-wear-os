package fm.libro.wearos

import android.app.Application
import fm.libro.wearos.data.AppDatabase

class LibroFmApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
