package fm.libro.wearos.player

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_state",
)

@Singleton
class PlayerStateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.playerDataStore

    val lastPlayingIsbn: Flow<String?> = dataStore.data.map { prefs ->
        prefs[LAST_PLAYING_ISBN]
    }

    suspend fun setLastPlayingIsbn(isbn: String?) {
        dataStore.edit { prefs ->
            if (isbn != null) {
                prefs[LAST_PLAYING_ISBN] = isbn
            } else {
                prefs.remove(LAST_PLAYING_ISBN)
            }
        }
    }

    companion object {
        private val LAST_PLAYING_ISBN = stringPreferencesKey("last_playing_isbn")
    }
}
