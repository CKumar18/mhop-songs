package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AdminSession
import com.example.domain.model.UserRole
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.authPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mhop_local_auth")

/**
 * Phase 1 Local Admin Preview implementation of [AuthRepository].
 *
 * No hardcoded passwords are used and no multi-device cloud authorization is claimed.
 * When Firebase Authentication & Cloud Firestore are connected in Phase 2, replace or back
 * this implementation with FirebaseAuth + CredentialManager Google Sign-In and read
 * `users/{uid}.role` from Firestore.
 */
class LocalAuthRepository(
    private val context: Context,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EMAIL = stringPreferencesKey("email")
        val UID = stringPreferencesKey("uid")
    }

    private val _currentSession = MutableStateFlow(AdminSession(isLocalPreviewMode = true))
    override val currentSession: StateFlow<AdminSession> = _currentSession.asStateFlow()

    init {
        scope.launch {
            val prefs = context.authPrefsDataStore.data.first()
            val signedIn = prefs[Keys.IS_SIGNED_IN] ?: false
            if (signedIn) {
                _currentSession.value = AdminSession(
                    isSignedIn = true,
                    uid = prefs[Keys.UID] ?: "local-preview-admin",
                    displayName = prefs[Keys.DISPLAY_NAME] ?: "Local Admin Preview",
                    email = prefs[Keys.EMAIL] ?: "",
                    role = UserRole.ADMIN,
                    isLocalPreviewMode = true,
                )
            }
        }
    }

    override suspend fun signInLocalAdminPreview(displayName: String, email: String): Result<AdminSession> =
        runCatching {
            val cleanName = displayName.trim().ifBlank { "Church Administrator" }
            val cleanEmail = email.trim()
            val uid = "local-admin-${cleanEmail.ifBlank { cleanName }.lowercase().hashCode()}"
            val session = AdminSession(
                isSignedIn = true,
                uid = uid,
                displayName = cleanName,
                email = cleanEmail,
                role = UserRole.ADMIN,
                isLocalPreviewMode = true,
            )
            _currentSession.value = session
            context.authPrefsDataStore.edit { prefs ->
                prefs[Keys.IS_SIGNED_IN] = true
                prefs[Keys.DISPLAY_NAME] = cleanName
                prefs[Keys.EMAIL] = cleanEmail
                prefs[Keys.UID] = uid
            }
            session
        }

    override suspend fun signOut(): Result<Unit> = runCatching {
        context.authPrefsDataStore.edit { it.clear() }
        _currentSession.value = AdminSession(isSignedIn = false, role = UserRole.MEMBER, isLocalPreviewMode = true)
    }
}
