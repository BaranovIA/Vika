package com.baranova.multiplication

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("trainer", Context.MODE_PRIVATE)

    /** Лучший результат для режима (проценты или количество примеров) или -1, если ещё не проходили. */
    fun best(key: String): Int = sp.getInt("best_$key", -1)

    fun saveBest(key: String, score: Int) {
        if (score > best(key)) {
            sp.edit().putInt("best_$key", score).apply()
        }
    }

    fun totalSolved(): Int = sp.getInt("solved", 0)

    fun addSolved(count: Int) {
        if (count > 0) {
            sp.edit().putInt("solved", totalSolved() + count).apply()
        }
    }

    /** Включена ли озвучка. */
    var soundOn: Boolean
        get() = sp.getBoolean("sound", true)
        set(value) {
            sp.edit().putBoolean("sound", value).apply()
        }
}
