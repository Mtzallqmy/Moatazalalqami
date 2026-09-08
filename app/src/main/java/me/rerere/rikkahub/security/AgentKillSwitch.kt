package me.rerere.rikkahub.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentKillSwitch(context: Context) {
    private val prefs = context.getSharedPreferences("agent_security", Context.MODE_PRIVATE)
    private val mutableEnabled = MutableStateFlow(prefs.getBoolean(KEY, false))
    val enabled: StateFlow<Boolean> = mutableEnabled.asStateFlow()
    fun setEnabled(value: Boolean) {
        check(prefs.edit().putBoolean(KEY, value).commit())
        mutableEnabled.value = value
    }
    fun requireBackgroundAllowed() { check(!mutableEnabled.value) { "Global agent kill switch is enabled" } }
    fun requireWritesAllowed() { check(!mutableEnabled.value) { "Global agent kill switch blocks agent writes" } }
    companion object { private const val KEY = "global_kill_switch" }
}
