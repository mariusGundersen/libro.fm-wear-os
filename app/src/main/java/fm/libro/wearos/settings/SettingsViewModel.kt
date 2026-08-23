package fm.libro.wearos.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {

    private val dataStore get() = context.settingsDataStore

    val settings = dataStore.data.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null,
    )

    fun setSuppressSpeaker(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_SUPPRESS_SPEAKER] = enabled }
        }
    }

    fun setAudioOffload(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_AUDIO_OFFLOAD] = enabled }
        }
    }

    companion object {
        val KEY_SUPPRESS_SPEAKER = booleanPreferencesKey("suppress_speaker")
        val KEY_AUDIO_OFFLOAD = booleanPreferencesKey("audio_offload")
    }
}
