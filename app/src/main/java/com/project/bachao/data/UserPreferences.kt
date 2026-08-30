package com.project.bachao.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    companion object {
        private const val PREF_NAME = "bachao_prefs"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_ACTIVE_ALERT_ID = "active_alert_id"
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    private val _activeAlertIdFlow = MutableStateFlow(getActiveAlertId())
    val activeAlertIdFlow: StateFlow<Int> = _activeAlertIdFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_ACTIVE_ALERT_ID) {
            _activeAlertIdFlow.value = getActiveAlertId()
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun saveUser(
        userId: Int,
        name: String,
        phone: String,
        email: String
    ) {
        preferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getUserId(): Int = preferences.getInt(KEY_USER_ID, -1)
    fun getName(): String = preferences.getString(KEY_NAME, "") ?: ""
    fun getPhone(): String = preferences.getString(KEY_PHONE, "") ?: ""
    fun getEmail(): String = preferences.getString(KEY_EMAIL, "") ?: ""
    fun isRegistered(): Boolean = getUserId() != -1

    fun saveActiveAlertId(alertId: Int) {
        preferences.edit()
            .putInt(KEY_ACTIVE_ALERT_ID, alertId)
            .apply()
        _activeAlertIdFlow.value = alertId
    }

    fun getActiveAlertId(): Int {
        return preferences.getInt(KEY_ACTIVE_ALERT_ID, -1)
    }

    fun clearActiveAlertId() {
        preferences.edit()
            .remove(KEY_ACTIVE_ALERT_ID)
            .apply()
        _activeAlertIdFlow.value = -1
    }

    fun clearUser() {
        preferences.edit().clear().apply()
    }
}