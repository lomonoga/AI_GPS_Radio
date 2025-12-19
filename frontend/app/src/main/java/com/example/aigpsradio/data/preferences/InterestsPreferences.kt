package com.example.aigpsradio.data.preferences

import android.content.Context
import android.content.SharedPreferences

class InterestsPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveInterests(interests: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_INTERESTS, interests)
            .apply()
    }

    fun getInterests(): Set<String> {
        return prefs.getStringSet(KEY_INTERESTS, setOf("architecture")) ?: setOf("architecture")
    }

    companion object {
        private const val PREFS_NAME = "aigpsradio_prefs"
        private const val KEY_INTERESTS = "user_interests"
    }
}