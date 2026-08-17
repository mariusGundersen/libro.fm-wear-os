package fm.libro.wearos.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "librofm_auth")

class AuthManager(private val context: Context) {

    private val dataStore get() = context.dataStore

    var token: String?
        get() = runBlocking {
            dataStore.data.map { it[KEY_TOKEN] }.first()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                if (value != null) prefs[KEY_TOKEN] = value else prefs.remove(KEY_TOKEN)
            }
        }

    var username: String?
        get() = runBlocking {
            dataStore.data.map { it[KEY_USERNAME] }.first()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                if (value != null) prefs[KEY_USERNAME] = value else prefs.remove(KEY_USERNAME)
            }
        }

    var password: String?
        get() = runBlocking {
            dataStore.data.map { it[KEY_PASSWORD] }.first()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                if (value != null) prefs[KEY_PASSWORD] = value else prefs.remove(KEY_PASSWORD)
            }
        }

    var tokenCreatedAt: Long
        get() = runBlocking {
            dataStore.data.map { it[KEY_TOKEN_CREATED_AT] ?: 0L }.first()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs -> prefs[KEY_TOKEN_CREATED_AT] = value }
        }

    val isLoggedIn: Boolean
        get() = token != null

    fun saveLogin(token: String, createdAt: Long, username: String, password: String) {
        this.token = token
        this.tokenCreatedAt = createdAt
        this.username = username
        this.password = password
    }

    fun logout() {
        runBlocking {
            dataStore.edit { it.clear() }
        }
    }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private val KEY_TOKEN_CREATED_AT = longPreferencesKey("token_created_at")
    }
}
