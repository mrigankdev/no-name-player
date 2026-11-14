package com.mrigank.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Small helpers ----------

/** Which videos are ticked while the person is selecting. */
class Selection {
    var ids by mutableStateOf(setOf<Long>())

    val active: Boolean
        get() = ids.isNotEmpty()

    fun toggle(id: Long) {
        ids = if (ids.contains(id)) ids - id else ids + id
    }

    fun clear() {
        ids = emptySet()
    }
}

/** One row on the home screen: either a group or a single video. */
internal class Entry(val g: Group?, val v: Video?, val queue: List<Video>, val index: Int) {
    val key: String
        get() = g?.key ?: ("v" + (v?.id ?: 0L))
}

internal fun buildEntries(
    videos: List<Video>,
    groups: List<Group>,
    prefs: Prefs,
    query: String
): List<Entry> {
    val q = query.trim().lowercase()
    if (q.isNotEmpty()) {
        val hits = Grouping.sortVideos(
            videos.filter { it.name.lowercase().contains(q) },
            prefs.sortKey,
            prefs.sortAsc,
            false
        )
        return hits.mapIndexed { i, v -> Entry(null, v, hits, i) }
    }
    if (prefs.view == "all") {
        val all = Grouping.sortVideos(videos, prefs.sortKey, prefs.sortAsc, false)
        return all.mapIndexed { i, v -> Entry(null, v, all, i) }
    }
    return groups.map { g ->
        if (g.videos.size == 1 && g.key.startsWith("v:")) {
            Entry(null, g.videos[0], g.videos, 0)
        } else {
            Entry(g, null, g.videos, 0)
        }
    }
}

@Composable
fun NIconBtn(ic: Nic, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Nglyph(ic, tint, 22.dp)
    }
}

@Composable
internal fun ProgressLine(frac: Float, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(pal.onBg.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(frac)
                .fillMaxHeight()
                .background(pal.accent)
        )
    }
}

@Composable
internal fun Tick(on: Boolean) {
    val pal = LocalPal.current
    if (on) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(pal.accent),
            contentAlignment = Alignment.Center
        ) {
            Nglyph(Nic.Check, pal.onAccent, 14.dp)
        }
    } else {
        Box(
            Modifier
                .size(22.dp)
                .border(2.dp, pal.muted, CircleShape)
        )
    }
}

// ---------- Rows ----------

/** Folder picture: four small pictures in a 2 x 2 block, like VLC. */
@Composable
internal fun Mosaic(videos: List<Video>, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    val list = videos.take(4)
    if (list.size < 4) {
        Thumb(list.first(), modifier)
    } else {
        Column(modifier.clip(RoundedCornerShape(8.dp)).background(pal.bg)) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Thumb(list[0], Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.width(2.dp))
                Thumb(list[1], Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(2.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Thumb(list[2], Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.width(2.dp))
                Thumb(list[3], Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
internal fun GroupItem(
    g: Group,
    grid: Boolean,
    isLast: Boolean,
    prefs: Prefs,
    actions: Actions,
    onClick: () -> Unit
) {
    val pal = LocalPal.current
    val total = g.videos.size
    val titleColor = if (isLast) pal.accent else pal.onBg

    if (grid) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
        ) {
            Mosaic(g.videos, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    T(g.name, 15, titleColor, FontWeight.Normal, 2)
                    T(countText(total), 13, pal.muted)
                }
                GroupMenu(g, actions)
            }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Mosaic(g.videos, Modifier.size(96.dp, 60.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                T(g.name, 17, titleColor, FontWeight.Normal, 2)
                Spacer(Modifier.height(2.dp))
                T(countText(total), 14, pal.muted)
            }
            GroupMenu(g, actions)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoItem(
    v: Video,
    grid: Boolean,
    prefs: Prefs,
    sel: Selection,
    actions: Actions,
    onPlay: () -> Unit
) {
    val pal = LocalPal.current
    val rev = prefs.rev
    val pos = remember(rev, v.id) { prefs.position(v.id) }
    val watched = pos < 0L
    val frac = when {
        watched -> 1f
        v.durationMs > 0L && pos > 0L -> (pos.toFloat() / v.durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val picked = sel.ids.contains(v.id)
    val meta = fmt(v.durationMs) + if (sel.active) " · " + sizeText(v.size) else ""
    val titleColor = if (watched) pal.muted else pal.onBg
    val bg = if (picked) pal.accent.copy(alpha = 0.14f) else Color.Transparent

    if (grid) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .combinedClickable(
                    onClick = {
                        if (sel.active) sel.toggle(v.id) else onPlay()
                    },
                    onLongClick = { sel.toggle(v.id) }
                )
        ) {
            Box {
                Thumb(v, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                if (!watched && frac > 0f) {
                    ProgressLine(
                        frac,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    )
                }
                if (sel.active) {
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { Tick(picked) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    T(v.name, 15, titleColor, FontWeight.Normal, 2)
                    T(meta, 13, pal.muted)
                }
                if (!sel.active) VideoMenu(v, prefs, actions)
            }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .combinedClickable(
                    onClick = {
                        if (sel.active) sel.toggle(v.id) else onPlay()
                    },
                    onLongClick = { sel.toggle(v.id) }
                )
                .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(96.dp, 60.dp)) {
                Thumb(v, Modifier.fillMaxSize())
                if (!watched && frac > 0f) {
                    ProgressLine(
                        frac,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                T(v.name, 17, titleColor, FontWeight.Normal, 2)
                Spacer(Modifier.height(2.dp))
                T(meta, 14, pal.muted)
            }
            if (sel.active) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Tick(picked) }
            } else {
                VideoMenu(v, prefs, actions)
            }
        }
    }
}

// ---------- Top bars ----------

@Composable
internal fun SelectionBar(sel: Selection, videos: List<Video>, actions: Actions) {
    val pal = LocalPal.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(Ic.Close, pal.onBg, { sel.clear() })
        T("${sel.ids.size} selected", 18, pal.onBg, FontWeight.SemiBold, 1, Modifier.weight(1f))
        NIconBtn(Nic.Move, pal.onBg) {
            val picked = videos.filter { sel.ids.contains(it.id) }
            sel.clear()
            if (picked.isNotEmpty()) actions.move(picked)
        }
        NIconBtn(Nic.Check, pal.onBg) {
            val picked = videos.filter { sel.ids.contains(it.id) }
            sel.clear()
            if (picked.isNotEmpty()) actions.watched(picked, true)
        }
        NIconBtn(Nic.Trash, Color(0xFFEF5350)) {
            val picked = videos.filter { sel.ids.contains(it.id) }
            sel.clear()
            if (picked.isNotEmpty()) actions.delete(picked)
        }
    }
}

@Composable
internal fun SearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val pal = LocalPal.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(Ic.Back, pal.onBg, onClose)
        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(pal.surface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) T("Search videos", 15, pal.muted)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = pal.onBg, fontSize = 15.sp),
                cursorBrush = SolidColor(pal.accent),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
    }
}
