package org.jw.library.auto.data

import android.content.Context
import java.util.Locale

/**
 * Manages the app content language (English / Romanian).
 *
 * Default: follows device locale — Romanian device → Romanian content automatically.
 * Override: user can tap the language toggle in the browse tree to switch manually.
 *
 * jw.org API language codes:  E = English,  M = Romanian (Română)
 */
object LanguagePreference {

    const val LANG_ENGLISH  = "E"
    const val LANG_ROMANIAN = "M"

    private const val PREFS        = "lang_prefs"
    private const val KEY_OVERRIDE = "lang_override"

    /** Returns the active language code ("E" or "M"). */
    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_OVERRIDE, null)
            ?: autoDetect()

    /** Toggle between English and Romanian, storing the override. */
    fun toggle(context: Context) {
        val next = if (get(context) == LANG_ROMANIAN) LANG_ENGLISH else LANG_ROMANIAN
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_OVERRIDE, next).apply()
    }

    /** True if the current language is Romanian. */
    fun isRomanian(context: Context) = get(context) == LANG_ROMANIAN

    /** Label describing the current language for the toggle item in the browse tree. */
    fun toggleLabel(context: Context): String =
        if (isRomanian(context)) "🌐 Română  |  tap for English"
        else                     "🌐 English  |  tap pentru Română"

    private fun autoDetect(): String =
        if (Locale.getDefault().language == "ro") LANG_ROMANIAN else LANG_ENGLISH
}
