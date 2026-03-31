package com.cuchieman.tamatask.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

object DebugPrefs {
    private const val PREFS_NAME = "debug_prefs"
    private const val KEY_UNLOCK_ALL_SITES = "unlock_all_sites"
    private const val KEY_UNLOCK_ALL_DINOS = "unlock_all_dinos"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUnlockAllSites(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UNLOCK_ALL_SITES, false)

    fun setUnlockAllSites(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_UNLOCK_ALL_SITES, value) }
    }

    fun getUnlockAllDinos(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UNLOCK_ALL_DINOS, false)

    fun setUnlockAllDinos(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_UNLOCK_ALL_DINOS, value) }
    }
}

@Composable
fun rememberDebugPref(
    key: String,
    getter: (Context) -> Boolean,
    setter: (Context, Boolean) -> Unit
): MutableState<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(getter(context)) }
    return object : MutableState<Boolean> {
        override var value: Boolean
            get() = state.value
            set(newValue) {
                state.value = newValue
                setter(context, newValue)
            }
        override fun component1() = value
        override fun component2(): (Boolean) -> Unit = { value = it }
    }
}
