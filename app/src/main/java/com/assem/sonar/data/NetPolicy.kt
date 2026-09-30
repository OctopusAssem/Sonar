package com.assem.sonar.data

import android.content.Context
import com.assem.sonar.util.Prefs

/**
 * The user's own switch for whether this app is allowed to use the network.
 * When it is off, Sonar performs no network requests at all.
 */
object NetPolicy {

    private const val KEY_ALLOW_INTERNET = "allow_internet"

    fun isAllowed(context: Context): Boolean =
        Prefs.of(context).getBoolean(KEY_ALLOW_INTERNET, true)

    fun setAllowed(context: Context, allowed: Boolean) {
        Prefs.of(context).edit().putBoolean(KEY_ALLOW_INTERNET, allowed).apply()
    }
}
