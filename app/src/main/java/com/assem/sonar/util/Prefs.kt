package com.assem.sonar.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "sonar_prefs"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
