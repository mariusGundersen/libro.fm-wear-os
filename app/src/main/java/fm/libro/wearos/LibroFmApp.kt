package fm.libro.wearos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.horologist.media.sync.initializers.Sync
import dagger.hilt.android.HiltAndroidApp
import fm.libro.wearos.data.AppDatabase
import javax.inject.Inject

@HiltAndroidApp
class LibroFmApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        Sync.initialize(context = this)
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
