package com.ai.assistance.operit.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Opt-in toggle for supervision mode (a smart model observes driver turns). */
class SupervisionPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _enabledFlow = MutableStateFlow(isEnabled())
    val enabledFlow: StateFlow<Boolean> = _enabledFlow.asStateFlow()

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabledFlow.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "supervision_preferences"
        private const val KEY_ENABLED = "supervision_enabled"
    }
}
