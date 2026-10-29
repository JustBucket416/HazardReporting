package com.example.hazardreporting.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/**
 * Stores the [Session] in DataStore
 *
 * @property dataStore preferences that hold the session
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Reads the stored session
     *
     * @return the session, or null if none is stored or the file can't be read
     */
    suspend fun read(): Session? {
        val preferences = dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        val token = preferences[TOKEN] ?: return null
        val user = User(
            id = preferences[USER_ID] ?: return null,
            email = preferences[USER_EMAIL] ?: return null,
            name = preferences[USER_NAME] ?: return null,
            role = UserRole.entries.find { it.name == preferences[USER_ROLE] } ?: UserRole.Unknown,
        )
        return Session(token, user)
    }

    /**
     * Stores [session], replacing any stored one
     */
    suspend fun write(session: Session) {
        dataStore.edit {
            it[TOKEN] = session.token
            it[USER_ID] = session.user.id
            it[USER_EMAIL] = session.user.email
            it[USER_NAME] = session.user.name
            it[USER_ROLE] = session.user.role.name
        }
    }

    /**
     * Deletes the stored session
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
    }
}
