package fm.libro.wearos.complication

import android.graphics.drawable.Icon
import androidx.media3.common.MediaItem
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import coil.ImageLoader
import com.google.android.horologist.media.ui.complication.MediaStatusTemplate
import com.google.android.horologist.media.ui.complication.MediaStatusTemplate.Data
import com.google.android.horologist.media3.navigation.IntentBuilder
import com.google.android.horologist.tiles.complication.ComplicationTemplate
import com.google.android.horologist.tiles.complication.DataComplicationService
import com.google.android.horologist.tiles.images.loadImage
import dagger.hilt.android.AndroidEntryPoint
import fm.libro.wearos.R
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MediaStatusComplicationService :
    DataComplicationService<Data, ComplicationTemplate<Data>>() {
    @Inject
    lateinit var intentBuilder: IntentBuilder

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var dataUpdates: DataUpdates

    override val renderer: MediaStatusTemplate = MediaStatusTemplate(this)

    override fun previewData(type: ComplicationType): Data = renderer.previewData()

    override suspend fun data(request: ComplicationRequest): Data {
        val state = dataUpdates.stateFlow.value

        return if (state.mediaItem != null) {
            whilePlayingData(state.mediaItem)
        } else {
            notPlayingData()
        }
    }

    private fun notPlayingData(): Data {
        val mediaTitle = getString(R.string.no_title)
        val mediaArtist = getString(R.string.app_name)
        return Data(
            text = mediaTitle,
            title = mediaArtist,
            appIconRes = R.mipmap.ic_launcher,
            launchIntent = intentBuilder.buildPlayerIntent(),
            type = SmallImageType.ICON,
            contentDescription = createContentDescription(mediaTitle, mediaArtist),
        )
    }

    private suspend fun whilePlayingData(mediaItem: MediaItem): Data {
        val bitmap = withTimeoutOrNull(2.seconds) {
            imageLoader.loadImage(
                context = this@MediaStatusComplicationService,
                data = mediaItem.mediaMetadata.artworkUri,
            ) {
                size(64)
            }
        }
        val icon = if (bitmap != null) Icon.createWithBitmap(bitmap) else null
        val mediaTitle = mediaItem.mediaMetadata.displayTitle.toString()
        val mediaArtist = mediaItem.mediaMetadata.artist.toString()
        return Data(
            text = mediaTitle,
            title = mediaArtist,
            icon = icon,
            type = SmallImageType.PHOTO,
            launchIntent = intentBuilder.buildPlayerIntent(),
            contentDescription = createContentDescription(mediaTitle, mediaArtist),
        )
    }

    private fun createContentDescription(
        mediaTitle: String,
        mediaArtist: String,
    ): ComplicationText =
        PlainComplicationText.Builder(
            text = getString(
                R.string.complication_content_description,
                mediaTitle,
                mediaArtist,
            ),
        ).build()
}
