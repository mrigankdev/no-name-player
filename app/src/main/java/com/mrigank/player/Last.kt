package com.mrigank.player

import android.content.Context

object LastPlayed {
    private fun sp(c: Context) = c.getSharedPreferences("last_played", Context.MODE_PRIVATE)

    fun set(c: Context, id: Long) {
        sp(c).edit().putLong("id", id).apply()
    }

    fun get(c: Context): Long = sp(c).getLong("id", -1L)
}
