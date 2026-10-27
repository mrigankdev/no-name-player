package com.mrigank.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)

    var theme by mutableStateOf(sp.getString("theme", "Graphite") ?: "Graphite")
        private set
    var pureBlack by mutableStateOf(sp.getBoolean("pureBlack", false))
        private set
    var grid by mutableStateOf(sp.getBoolean("grid", false))
        private set
    var rememberSub by mutableStateOf(sp.getBoolean("rememberSub", true))
        private set
    var autoPip by mutableStateOf(sp.getBoolean("autoPip", true))
        private set
    var skipShort by mutableIntStateOf(sp.getInt("skipShort", 10))
        private set
    var skipLong by mutableIntStateOf(sp.getInt("skipLong", 85))
        private set
    var subFolder by mutableStateOf<String?>(sp.getString("subFolder", null))
        private set

    fun setTheme(v: String) {
        theme = v
        sp.edit().putString("theme", v).apply()
    }

    fun setPureBlack(v: Boolean) {
        pureBlack = v
        sp.edit().putBoolean("pureBlack", v).apply()
    }

    fun setGrid(v: Boolean) {
        grid = v
        sp.edit().putBoolean("grid", v).apply()
    }

    fun setRememberSub(v: Boolean) {
        rememberSub = v
        sp.edit().putBoolean("rememberSub", v).apply()
    }

    fun setAutoPip(v: Boolean) {
        autoPip = v
        sp.edit().putBoolean("autoPip", v).apply()
    }

    fun setSkipShort(v: Int) {
        skipShort = v
        sp.edit().putInt("skipShort", v).apply()
    }

    fun setSkipLong(v: Int) {
        skipLong = v
        sp.edit().putInt("skipLong", v).apply()
    }

    fun setSubFolder(v: String?) {
        subFolder = v
        sp.edit().putString("subFolder", v).apply()
    }

    // Subtitle and audio memory, used by the player.
    var subLang: String?
        get() = sp.getString("subLang", null)
        set(v) {
            sp.edit().putString("subLang", v).apply()
        }

    var audioLang: String?
        get() = sp.getString("audioLang", null)
        set(v) {
            sp.edit().putString("audioLang", v).apply()
        }

    var subOff: Boolean
        get() = sp.getBoolean("subOff", false)
        set(v) {
            sp.edit().putBoolean("subOff", v).apply()
        }

    // Resume points. -1 means "finished".
    fun position(id: Long): Long = sp.getLong("pos_$id", 0L)

    fun setPosition(id: Long, ms: Long) {
        sp.edit().putLong("pos_$id", ms).apply()
    }
}
