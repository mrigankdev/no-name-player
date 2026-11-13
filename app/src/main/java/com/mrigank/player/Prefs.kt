package com.mrigank.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)

    // ---------- Look ----------
    var mode by mutableStateOf(sp.getString("mode", "dark") ?: "dark")
        private set
    var darkTheme by mutableStateOf(sp.getString("darkTheme", "Midnight") ?: "Midnight")
        private set
    var lightTheme by mutableStateOf(sp.getString("lightTheme", "Cloud") ?: "Cloud")
        private set
    var accentIdx by mutableIntStateOf(sp.getInt("accentIdx", -1))
        private set
    var pureBlack by mutableStateOf(sp.getBoolean("pureBlack", false))
        private set

    // ---------- Home ----------
    var grid by mutableStateOf(sp.getBoolean("grid", false))
        private set
    var view by mutableStateOf(sp.getString("view", "grouped") ?: "grouped")
        private set
    var sortKey by mutableStateOf(sp.getString("sortKey", "name") ?: "name")
        private set
    var sortAsc by mutableStateOf(sp.getBoolean("sortAsc", true))
        private set
    var showPill by mutableStateOf(sp.getBoolean("showPill", true))
        private set

    // Goes up every time groups, names or watched marks change, so screens redraw.
    var rev by mutableIntStateOf(0)
        private set

    // ---------- Player ----------
    var resumePlayback by mutableStateOf(sp.getBoolean("resumePlayback", true))
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

    fun changeMode(v: String) {
        mode = v
        sp.edit().putString("mode", v).apply()
    }

    fun changeDarkTheme(v: String) {
        darkTheme = v
        sp.edit().putString("darkTheme", v).apply()
    }

    fun changeLightTheme(v: String) {
        lightTheme = v
        sp.edit().putString("lightTheme", v).apply()
    }

    fun changeAccent(v: Int) {
        accentIdx = v
        sp.edit().putInt("accentIdx", v).apply()
    }

    fun changePureBlack(v: Boolean) {
        pureBlack = v
        sp.edit().putBoolean("pureBlack", v).apply()
    }

    fun changeGrid(v: Boolean) {
        grid = v
        sp.edit().putBoolean("grid", v).apply()
    }

    fun changeView(v: String) {
        view = v
        sp.edit().putString("view", v).apply()
    }

    fun changeSort(key: String, asc: Boolean) {
        sortKey = key
        sortAsc = asc
        sp.edit().putString("sortKey", key).putBoolean("sortAsc", asc).apply()
        rev += 1
    }

    fun changeShowPill(v: Boolean) {
        showPill = v
        sp.edit().putBoolean("showPill", v).apply()
    }

    fun changeResume(v: Boolean) {
        resumePlayback = v
        sp.edit().putBoolean("resumePlayback", v).apply()
    }

    fun changeRememberSub(v: Boolean) {
        rememberSub = v
        sp.edit().putBoolean("rememberSub", v).apply()
    }

    fun changeAutoPip(v: Boolean) {
        autoPip = v
        sp.edit().putBoolean("autoPip", v).apply()
    }

    fun changeSkipShort(v: Int) {
        skipShort = v
        sp.edit().putInt("skipShort", v).apply()
    }

    fun changeSkipLong(v: Int) {
        skipLong = v
        sp.edit().putInt("skipLong", v).apply()
    }

    fun changeSubFolder(v: String?) {
        subFolder = v
        sp.edit().putString("subFolder", v).apply()
    }

    // ---------- Subtitle and audio memory, used by the player ----------
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

    // ---------- Resume points. -1 means "watched" ----------
    fun position(id: Long): Long = sp.getLong("pos_$id", 0L)

    fun setPosition(id: Long, ms: Long) {
        sp.edit().putLong("pos_$id", ms).apply()
    }

    fun markWatched(ids: List<Long>, watched: Boolean) {
        val e = sp.edit()
        for (id in ids) e.putLong("pos_$id", if (watched) -1L else 0L)
        e.apply()
        rev += 1
    }

    // ---------- Groups (only inside the app, files never move) ----------
    // null = automatic group from the file name, "" = on its own, otherwise a group key.
    fun groupOverride(id: Long): String? = sp.getString("go_$id", null)

    fun setGroups(ids: List<Long>, key: String?) {
        val e = sp.edit()
        for (id in ids) {
            if (key == null) e.remove("go_$id") else e.putString("go_$id", key)
        }
        e.apply()
        rev += 1
    }

    fun groupName(key: String): String? = sp.getString("gn_$key", null)

    fun renameGroup(key: String, name: String) {
        sp.edit().putString("gn_$key", name).apply()
        rev += 1
    }
}
