package com.mrigank.player

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

object Library {

    const val OTHER = "Other videos"

    private val sxe = Regex("""(?i)\bS(\d{1,2})\s*E(\d{1,3})\b""")
    private val nxn = Regex("""(?i)\b(\d{1,2})x(\d{2,3})\b""")
    private val epw = Regex("""(?i)\bEp(?:isode)?\s*(\d{1,3})\b""")
    private val dash = Regex("""\s-\s*(\d{1,3})(?:\s|$)""")
    private val brackets = Regex("""\[[^\]]*]|\([^)]*\)""")
    private val spaces = Regex("""\s+""")

    class Parsed(val show: String, val season: Int, val episode: Int)

    /** Finds the show name and episode inside a file name, or returns null. */
    fun parse(raw: String): Parsed? {
        val s = raw
            .replace(brackets, " ")
            .replace('.', ' ')
            .replace('_', ' ')
            .replace(spaces, " ")
            .trim()
        val a = sxe.find(s)
        if (a != null) return make(s, a, a.groupValues[1].toInt(), a.groupValues[2].toInt())
        val b = nxn.find(s)
        if (b != null) return make(s, b, b.groupValues[1].toInt(), b.groupValues[2].toInt())
        val c = epw.find(s)
        if (c != null) return make(s, c, 1, c.groupValues[1].toInt())
        val d = dash.find(s)
        if (d != null) return make(s, d, 1, d.groupValues[1].toInt())
        return null
    }

    private fun make(s: String, m: MatchResult, season: Int, episode: Int): Parsed? {
        val title = s.substring(0, m.range.first).trim().trimEnd('-', ':', '|', '~').trim()
        if (title.length < 2) return null
        return Parsed(title, season, episode)
    }

    fun scan(context: Context): List<Video> {
        val base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        val out = ArrayList<Video>()
        val cursor = context.contentResolver.query(base, projection, null, null, null)
        cursor?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val iName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val display = c.getString(iName) ?: continue
                val id = c.getLong(iId)
                val name = display.substringBeforeLast('.')
                val parsed = parse(name)
                out.add(
                    Video(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id),
                        name = name,
                        durationMs = c.getLong(iDur),
                        size = c.getLong(iSize),
                        dateAdded = c.getLong(iDate),
                        show = parsed?.show ?: OTHER,
                        season = parsed?.season ?: 0,
                        episode = parsed?.episode ?: 0
                    )
                )
            }
        }
        return out
    }

    /** Groups videos by show name, regardless of the folder they are in. */
    fun groups(videos: List<Video>): List<Show> {
        val map = LinkedHashMap<String, MutableList<Video>>()
        val names = HashMap<String, String>()
        for (v in videos) {
            val key = v.show.lowercase()
            if (!names.containsKey(key)) names[key] = v.show
            map.getOrPut(key) { ArrayList() }.add(v)
        }
        val real = ArrayList<Show>()
        var other: Show? = null
        for ((key, list) in map) {
            val name = names[key] ?: key
            if (name == OTHER) {
                other = Show(name, list.sortedByDescending { it.dateAdded })
            } else {
                val sorted = list.sortedWith(
                    compareBy<Video>({ it.season }, { it.episode }, { it.name.lowercase() })
                )
                real.add(Show(name, sorted))
            }
        }
        real.sortBy { it.name.lowercase() }
        val last = other
        if (last != null) real.add(last)
        return real
    }

    // ---------- Subtitle folder matching ----------

    class SubFile(val name: String, val uri: Uri)

    private val subExtensions = setOf("srt", "vtt", "ass", "ssa")

    fun listSubs(context: Context, treeUri: String?): List<SubFile> {
        if (treeUri == null) return emptyList()
        return try {
            val tree = Uri.parse(treeUri)
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val out = ArrayList<SubFile>()
            collect(context, tree, rootId, out, 0)
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun collect(context: Context, tree: Uri, docId: String, out: MutableList<SubFile>, depth: Int) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val cursor = context.contentResolver.query(children, columns, null, null, null)
        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (depth < 2) collect(context, tree, id, out, depth + 1)
                } else if (name.substringAfterLast('.', "").lowercase() in subExtensions) {
                    out.add(SubFile(name, DocumentsContract.buildDocumentUriUsingTree(tree, id)))
                }
            }
        }
    }

    /** Pairs each video with the subtitle file that belongs to it, if there is one. */
    fun match(videos: List<Video>, subs: List<SubFile>): Map<Long, SubFile> {
        if (subs.isEmpty()) return emptyMap()
        val stems = subs.map { it.name.substringBeforeLast('.').lowercase() }
        val parsedSubs = stems.map { parse(it) }
        val out = HashMap<Long, SubFile>()
        for (v in videos) {
            val base = v.name.lowercase()
            var found = -1
            for (i in subs.indices) {
                val stem = stems[i]
                if (stem == base || stem.startsWith("$base.") || stem.startsWith("$base ")) {
                    found = i
                    break
                }
            }
            if (found < 0 && v.episode > 0) {
                for (i in subs.indices) {
                    val p = parsedSubs[i] ?: continue
                    if (p.show.lowercase() == v.show.lowercase() &&
                        p.season == v.season &&
                        p.episode == v.episode
                    ) {
                        found = i
                        break
                    }
                }
            }
            if (found >= 0) out[v.id] = subs[found]
        }
        return out
    }
}
