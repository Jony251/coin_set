package com.example.coinset.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "coinset_prefs")

object TokenManager {
    private var dataStore: DataStore<Preferences>? = null
    
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    
    fun init(context: Context) {
        dataStore = context.dataStore
    }
    
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore?.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
    
    fun getAccessToken(): String? = runBlocking {
        dataStore?.data?.first()?.get(ACCESS_TOKEN_KEY)
    }
    
    fun getRefreshToken(): String? = runBlocking {
        dataStore?.data?.first()?.get(REFRESH_TOKEN_KEY)
    }
    
    suspend fun clearTokens() {
        dataStore?.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}
