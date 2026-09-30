package com.assem.sonar.util

import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import java.util.Locale

object LocaleHelper {

    const val SYSTEM = "system"
    const val ARABIC = "ar"
    const val ENGLISH = "en"

    private const val KEY_LANGUAGE = "language"

    fun get(context: Context): String =
        Prefs.of(context).getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    fun set(context: Context, language: String) {
        Prefs.of(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun localeOf(context: Context): Locale? = when (get(context)) {
        ARABIC -> Locale.forLanguageTag("ar")
        ENGLISH -> Locale.ENGLISH
        else -> null
    }

    /** Returns a context whose resources are bound to the chosen language. */
    fun wrap(context: Context): Context {
        val locale = localeOf(context) ?: return context
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun isRtl(context: Context): Boolean {
        val locales = context.resources.configuration.locales
        val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    }
}
