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

// ---------- Home ----------

@Composable
fun HomeScreen(
    videos: List<Video>,
    groups: List<Group>,
    loaded: Boolean,
    granted: Boolean,
    lastId: Long,
    prefs: Prefs,
    actions: Actions,
    onOpenGroup: (Group) -> Unit,
    onRefresh: () -> Unit,
    onGrant: () -> Unit
) {
    val pal = LocalPal.current
    val sel = remember { Selection() }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var modeMenu by remember { mutableStateOf(false) }
    val rev = prefs.rev

    BackHandler(enabled = sel.active || searching) {
        if (sel.active) {
            sel.clear()
        } else {
            searching = false
            query = ""
        }
    }

    val lastKey = remember(lastId, groups) {
        groups.firstOrNull { g -> g.videos.any { it.id == lastId } }?.key
    }
    val entries = remember(videos, groups, prefs.view, prefs.sortKey, prefs.sortAsc, query, rev) {
        buildEntries(videos, groups, prefs, query)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding()
    ) {
        if (sel.active) {
            SelectionBar(sel, videos, actions)
        } else if (searching) {
            SearchBar(query, { query = it }) {
                searching = false
                query = ""
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 4.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                T("Shiro ", 26, pal.onBg, FontWeight.Bold)
                T("Player", 26, pal.accent, FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconBtn(Ic.Search, pal.onBg, { searching = true })
                DotsMenu { close ->
                    DropdownMenuItem(
                        text = { T("Refresh", 15, pal.onBg) },
                        onClick = {
                            close()
                            onRefresh()
                        }
                    )
                }
            }
        }

        if (!sel.active) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 22.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { modeMenu = true }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        T(
                            if (prefs.view == "all") "All videos" else "Grouped",
                            15,
                            pal.onBg,
                            FontWeight.SemiBold
                        )
                        Nglyph(Nic.Down, pal.onBg, 20.dp)
                    }
                    DropdownMenu(
                        expanded = modeMenu,
                        onDismissRequest = { modeMenu = false },
                        modifier = Modifier.background(pal.surface)
                    ) {
                        DropdownMenuItem(
                            text = { T("Grouped", 15, pal.onBg) },
                            onClick = {
                                modeMenu = false
                                prefs.changeView("grouped")
                            }
                        )
                        DropdownMenuItem(
                            text = { T("All videos", 15, pal.onBg) },
                            onClick = {
                                modeMenu = false
                                prefs.changeView("all")
                            }
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (prefs.grid) {
                    IconBtn(Ic.List, pal.muted, { prefs.changeGrid(false) })
                } else {
                    NIconBtn(Nic.Grid, pal.muted) { prefs.changeGrid(true) }
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(pal.onBg.copy(alpha = 0.15f))
                )
                NIconBtn(Nic.Sort, pal.muted) { showSort = true }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!granted) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    T("Allow access to your videos", 16, pal.onBg)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onGrant,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pal.accent,
                            contentColor = pal.onAccent
                        )
                    ) {
                        T("Allow", 15, pal.onAccent, FontWeight.SemiBold)
                    }
                }
            } else if (!loaded) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    T("Loading videos", 15, pal.muted)
                }
            } else if (entries.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    T(if (query.isNotBlank()) "No results" else "No videos found", 15, pal.muted)
                }
            } else if (prefs.grid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(entries, key = { it.key }) { e ->
                        EntryView(e, true, lastKey, prefs, sel, actions, onOpenGroup)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                ) {
                    items(entries, key = { it.key }) { e ->
                        EntryView(e, false, lastKey, prefs, sel, actions, onOpenGroup)
                    }
                }
            }
        }
    }

    if (showSort) SortDialog(prefs) { showSort = false }
}

@Composable
private fun EntryView(
    e: Entry,
    grid: Boolean,
    lastKey: String?,
    prefs: Prefs,
    sel: Selection,
    actions: Actions,
    onOpenGroup: (Group) -> Unit
) {
    val g = e.g
    val v = e.v
    if (g != null) {
        GroupItem(g, grid, g.key == lastKey, prefs, actions) {
            if (!sel.active) onOpenGroup(g)
        }
    } else if (v != null) {
        VideoItem(v, grid, prefs, sel, actions) {
            actions.play(e.queue, e.index)
        }
    }
}

// ---------- Inside a group ----------

@Composable
fun GroupScreen(group: Group, prefs: Prefs, actions: Actions, onBack: () -> Unit) {
    val pal = LocalPal.current
    val sel = remember { Selection() }
    val rev = prefs.rev
    val done = remember(rev, group) { Grouping.watchedCount(group, prefs) }

    BackHandler(enabled = sel.active) { sel.clear() }

    Column(
        Modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding()
    ) {
        if (sel.active) {
            SelectionBar(sel, group.videos, actions)
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBtn(Ic.Back, pal.onBg, onBack)
                T(group.name, 20, pal.onBg, FontWeight.SemiBold, 1, Modifier.weight(1f))
                GroupMenu(group, actions)
            }
            Column(Modifier.padding(start = 24.dp, top = 4.dp, bottom = 10.dp)) {
                T(
                    countText(group.videos.size) + " · " + done + " of " + group.videos.size + " watched",
                    13,
                    pal.muted
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
        ) {
            items(group.videos, key = { it.id }) { v ->
                VideoItem(v, false, prefs, sel, actions) {
                    actions.play(group.videos, group.videos.indexOf(v))
                }
            }
        }
    }
}
