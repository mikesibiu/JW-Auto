package org.jw.library.auto.data

import android.content.Context
import java.util.Locale

/**
 * Manages the app content language (English / French / Romanian).
 *
 * Default: follows device locale — French device → French, Romanian device → Romanian.
 * Override: user taps the language toggle in the browse tree to cycle EN → FR → RO → EN.
 *
 * jw.org API language codes:  E = English,  F = French (Français),  M = Romanian (Română)
 * ⚠️  French code "F" is inferred from jw.org patterns — verify against live API if issues arise.
 */
object LanguagePreference {

    const val LANG_ENGLISH  = "E"
    const val LANG_FRENCH   = "F"
    const val LANG_ROMANIAN = "M"

    private val CYCLE = listOf(LANG_ENGLISH, LANG_FRENCH, LANG_ROMANIAN)

    private const val PREFS        = "lang_prefs"
    private const val KEY_OVERRIDE = "lang_override"

    /** Returns the active language code ("E", "F", or "M"). */
    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_OVERRIDE, null)
            ?: autoDetect()

    /** Cycles EN → FR → RO → EN, storing the override. */
    fun toggle(context: Context) {
        val current = get(context)
        val idx = CYCLE.indexOf(current).takeIf { it >= 0 } ?: 0
        val next = CYCLE[(idx + 1) % CYCLE.size]
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_OVERRIDE, next).apply()
    }

    fun isRomanian(context: Context) = get(context) == LANG_ROMANIAN
    fun isFrench(context: Context)   = get(context) == LANG_FRENCH

    /** Label showing current language and what tapping will switch to (next in cycle). */
    fun toggleLabel(context: Context): String = when (get(context)) {
        LANG_FRENCH   -> "🌐 Français  |  tap pentru Română"
        LANG_ROMANIAN -> "🌐 Română  |  tap for English"
        else          -> "🌐 English  |  tap pour Français"
    }

    private fun autoDetect(): String = when (Locale.getDefault().language) {
        "fr" -> LANG_FRENCH
        "ro" -> LANG_ROMANIAN
        else -> LANG_ENGLISH
    }
}
