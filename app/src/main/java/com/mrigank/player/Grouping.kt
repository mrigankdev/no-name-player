package com.mrigank.player

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/** A group of videos shown as one row on the home screen. Only exists inside the app. */
data class Group(val key: String, val name: String, val videos: List<Video>)

fun countText(n: Int): String = if (n == 1) "1 video" else "$n videos"

object Grouping {

    /** The group a video belongs to when nobody moved it: its show name. */
    fun autoKey(v: Video): String = "a:" + v.show.lowercase()

    fun keyOf(v: Video, prefs: Prefs): String {
        val o = prefs.groupOverride(v.id)
        return when {
            o == null -> autoKey(v)
            o.isEmpty() -> "v:" + v.id
            else -> o
        }
    }

    fun newKey(): String = "u:" + System.currentTimeMillis()

    fun watched(v: Video, prefs: Prefs): Boolean = prefs.position(v.id) < 0L

    fun watchedCount(g: Group, prefs: Prefs): Int = g.videos.count { watched(it, prefs) }

    fun sortVideos(list: List<Video>, key: String, asc: Boolean, episodic: Boolean): List<Video> {
        val cmp: Comparator<Video> = when (key) {
            "date" -> compareBy<Video> { it.dateAdded }
            "length" -> compareBy<Video> { it.durationMs }
            "size" -> compareBy<Video> { it.size }
            else -> if (episodic) {
                compareBy<Video>({ it.season }, { it.episode }, { it.name.lowercase() })
            } else {
                compareBy<Video> { it.name.lowercase() }
            }
        }
        val sorted = list.sortedWith(cmp)
        return if (asc) sorted else sorted.reversed()
    }

    fun build(videos: List<Video>, prefs: Prefs): List<Group> {
        val map = LinkedHashMap<String, MutableList<Video>>()
        for (v in videos) {
            map.getOrPut(keyOf(v, prefs)) { ArrayList() }.add(v)
        }
        val groups = ArrayList<Group>()
        for ((key, list) in map) {
            val sorted = sortVideos(list, prefs.sortKey, prefs.sortAsc, true)
            groups.add(Group(key, nameOf(key, list, prefs), sorted))
        }
        val cmp: Comparator<Group> = when (prefs.sortKey) {
            "date" -> compareBy<Group> { g -> g.videos.maxOf { it.dateAdded } }
            "length" -> compareBy<Group> { g -> g.videos.sumOf { it.durationMs } }
            "size" -> compareBy<Group> { g -> g.videos.sumOf { it.size } }
            else -> compareBy<Group> { it.name.lowercase() }
        }
        val sortedGroups = groups.sortedWith(cmp)
        return if (prefs.sortAsc) sortedGroups else sortedGroups.reversed()
    }

    private fun nameOf(key: String, list: List<Video>, prefs: Prefs): String {
        val saved = prefs.groupName(key)
        if (saved != null && saved.isNotBlank()) return saved
        if (key.startsWith("a:")) {
            val home = list.firstOrNull { autoKey(it) == key }
            return home?.show ?: key.removePrefix("a:")
        }
        if (key.startsWith("v:")) return list.first().name
        return "Group"
    }
}

/** Renaming and deleting real files, through the permission pop-up Android shows. */
object MediaOps {

    fun displayName(c: Context, uri: Uri): String? {
        return try {
            val cursor = c.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            var name: String? = null
            cursor?.use {
                if (it.moveToFirst()) name = it.getString(0)
            }
            name
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun writeSender(c: Context, uris: List<Uri>): IntentSender {
        return MediaStore.createWriteRequest(c.contentResolver, uris).intentSender
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deleteSender(c: Context, uris: List<Uri>): IntentSender {
        return MediaStore.createDeleteRequest(c.contentResolver, uris).intentSender
    }

    /** Renames the file but keeps its ending (.mkv, .mp4). Needs write access first on Android 11+. */
    fun rename(c: Context, uri: Uri, newName: String): Boolean {
        val old = displayName(c, uri) ?: return false
        val ext = old.substringAfterLast('.', "")
        val clean = newName.trim().replace(Regex("""[\\/:*?"<>|]"""), "")
        if (clean.isEmpty()) return false
        val full = if (ext.isEmpty()) clean else "$clean.$ext"
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, full)
        return try {
            c.contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    /** Only for Android 10 and older, where no pop-up is needed. */
    fun deleteDirect(c: Context, uris: List<Uri>) {
        for (u in uris) {
            try {
                c.contentResolver.delete(u, null, null)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
