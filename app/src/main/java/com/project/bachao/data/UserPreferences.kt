package com.project.bachao.data

import android.content.Context

class UserPreferences(context: Context) {

    companion object {
        private const val PREF_NAME = "bachao_prefs"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_EMAIL = "user_email"
    }

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

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

    fun getUserId(): Int {

        return preferences.getInt(
            KEY_USER_ID,
            -1
        )
    }

    fun getName(): String {

        return preferences.getString(
            KEY_NAME,
            ""
        ) ?: ""
    }

    fun getPhone(): String {

        return preferences.getString(
            KEY_PHONE,
            ""
        ) ?: ""
    }

    fun getEmail(): String {

        return preferences.getString(
            KEY_EMAIL,
            ""
        ) ?: ""
    }

    fun isRegistered(): Boolean {

        return getUserId() != -1
    }

    fun clearUser() {

        preferences.edit()
            .clear()
            .apply()
    }
}