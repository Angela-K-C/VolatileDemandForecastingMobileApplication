package com.example.volatiledemandforecasting.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

/** Which workspace and store the user is working in. Not secret, so it lives in DataStore (spec §13.1). */
data class StoreSelection(
    val workspaceId: String,
    val workspaceName: String,
    val role: String,
    val isReference: Boolean,
    val storeId: String,
    val storeName: String,
)

/** Light, dark or follow the phone (spec §8.12). */
enum class ThemeMode { System, Light, Dark }

/** How numbers in forecast tables are shown (spec §8.10). Totals always use the raw values. */
enum class TableDisplay { Decimals, WholeUnits }

/** Non-sensitive preferences. Session secrets are in [com.example.volatiledemandforecasting.core.security.KeystoreTokenStore]. */
class AppPreferences(context: Context) {
    private val store = context.applicationContext.dataStore

    val selection: Flow<StoreSelection?> = store.data.map { it.toSelection() }

    suspend fun saveSelection(selection: StoreSelection) {
        store.edit {
            it[WORKSPACE_ID] = selection.workspaceId
            it[WORKSPACE_NAME] = selection.workspaceName
            it[ROLE] = selection.role
            it[IS_REFERENCE] = selection.isReference
            it[STORE_ID] = selection.storeId
            it[STORE_NAME] = selection.storeName
        }
    }

    suspend fun clearSelection() {
        store.edit { prefs ->
            listOf(WORKSPACE_ID, WORKSPACE_NAME, ROLE, STORE_ID, STORE_NAME).forEach { prefs.remove(it) }
            prefs.remove(IS_REFERENCE)
        }
    }

    val tableDisplay: Flow<TableDisplay> = store.data.map { prefs ->
        prefs[TABLE_DISPLAY]?.let { runCatching { TableDisplay.valueOf(it) }.getOrNull() } ?: TableDisplay.Decimals
    }

    suspend fun setTableDisplay(display: TableDisplay) {
        store.edit { it[TABLE_DISPLAY] = display.name }
    }

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    /** Whether the intro tour has been shown on this phone. It isn't cleared on sign-out. */
    val tourSeen: Flow<Boolean> = store.data.map { it[TOUR_SEEN] ?: false }

    suspend fun setTourSeen() {
        store.edit { it[TOUR_SEEN] = true }
    }

    private fun Preferences.toSelection(): StoreSelection? {
        val workspaceId = this[WORKSPACE_ID] ?: return null
        val storeId = this[STORE_ID] ?: return null
        return StoreSelection(
            workspaceId = workspaceId,
            workspaceName = this[WORKSPACE_NAME].orEmpty(),
            role = this[ROLE].orEmpty(),
            isReference = this[IS_REFERENCE] ?: false,
            storeId = storeId,
            storeName = this[STORE_NAME].orEmpty(),
        )
    }

    private companion object {
        val WORKSPACE_ID = stringPreferencesKey("workspace_id")
        val WORKSPACE_NAME = stringPreferencesKey("workspace_name")
        val ROLE = stringPreferencesKey("role")
        val IS_REFERENCE = booleanPreferencesKey("is_reference")
        val STORE_ID = stringPreferencesKey("store_id")
        val STORE_NAME = stringPreferencesKey("store_name")
        val TABLE_DISPLAY = stringPreferencesKey("table_display")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TOUR_SEEN = booleanPreferencesKey("tour_seen")
    }
}
