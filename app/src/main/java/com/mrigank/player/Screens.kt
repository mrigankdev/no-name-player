package com.mrigank.player

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Center(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    val pal = LocalPal.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(Ic.Back, pal.onBg, onBack)
        T(title, 20, pal.onBg, FontWeight.SemiBold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    T(text.uppercase(), 12, LocalPal.current.muted, FontWeight.SemiBold)
}

@Composable
fun EpisodeRow(v: Video, tick: Int, prefs: Prefs, showLabel: Boolean, onClick: () -> Unit) {
    val pal = LocalPal.current
    val pos = remember(tick, v.id) { prefs.position(v.id) }
    val frac = when {
        pos < 0L -> 1f
        v.durationMs > 0L -> (pos.toFloat() / v.durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val title = if (showLabel && v.episode > 0) {
        if (v.season > 1) "Season ${v.season} · Episode ${v.episode}" else "Episode ${v.episode}"
    } else {
        v.name
    }
    val status = when {
        pos < 0L -> "Watched"
        pos > 3000L && v.durationMs > pos -> fmt(v.durationMs - pos) + " left"
        else -> fmt(v.durationMs)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Thumb(v, Modifier.fillMaxSize())
            if (frac > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(frac)
                        .height(3.dp)
                        .background(pal.accent)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            T(title, 16, pal.onBg, FontWeight.SemiBold, 2)
            if (showLabel && v.episode > 0) T(v.name, 12, pal.muted)
            T(status, 13, pal.muted)
        }
    }
}

@Composable
private fun ShowRow(show: Show, tick: Int, prefs: Prefs, onClick: () -> Unit) {
    val pal = LocalPal.current
    val watched = remember(tick, show) { show.videos.count { prefs.position(it.id) < 0L } }
    val count = if (show.name == Library.OTHER) {
        "${show.videos.size} videos"
    } else {
        "${show.videos.size} episodes · $watched watched"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumb(show.videos.first(), Modifier.width(112.dp).height(64.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            T(show.name, 17, pal.onBg, FontWeight.SemiBold, 2)
            T(count, 13, pal.muted)
        }
    }
}

@Composable
private fun ShowCard(show: Show, onClick: () -> Unit) {
    val pal = LocalPal.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Thumb(show.videos.first(), Modifier.fillMaxWidth().height(100.dp))
        T(show.name, 15, pal.onBg, FontWeight.SemiBold, 2)
        T(
            if (show.name == Library.OTHER) "${show.videos.size} videos" else "${show.videos.size} episodes",
            12,
            pal.muted
        )
    }
}

@Composable
fun HomeScreen(
    shows: List<Show>,
    videos: List<Video>,
    loaded: Boolean,
    granted: Boolean,
    prefs: Prefs,
    tick: Int,
    onOpenShow: (Show) -> Unit,
    onPlay: (List<Video>, Int) -> Unit,
    onAppearance: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onGrant: () -> Unit
) {
    val pal = LocalPal.current
    var searching by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(searching) {
        if (searching) {
            try {
                focus.requestFocus()
            } catch (e: Exception) {
                // the field was not ready yet, the user can tap it
            }
        }
    }
    val query = q.trim().lowercase()

    Column(Modifier.fillMaxSize().background(pal.bg)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searching) {
                BasicTextField(
                    value = q,
                    onValueChange = { q = it },
                    singleLine = true,
                    textStyle = TextStyle(color = pal.onBg, fontSize = 17.sp),
                    cursorBrush = SolidColor(pal.accent),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .focusRequester(focus)
                )
                IconBtn(Ic.Close, pal.onBg, {
                    q = ""
                    searching = false
                })
            } else {
                Spacer(Modifier.weight(1f))
                IconBtn(Ic.Search, pal.onBg, { searching = true })
                Box {
                    IconBtn(Ic.More, pal.onBg, { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Appearance") },
                            onClick = {
                                menu = false
                                onAppearance()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (prefs.grid) "List view" else "Grid view") },
                            onClick = {
                                menu = false
                                prefs.setGrid(!prefs.grid)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            onClick = {
                                menu = false
                                onRefresh()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menu = false
                                onSettings()
                            }
                        )
                    }
                }
            }
        }

        if (!granted) {
            Center {
                T("Allow access to your videos to see them here.", 16, pal.onBg, FontWeight.Normal, 3)
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = pal.onAccent)
                ) { Text("Allow access") }
            }
        } else if (!loaded) {
            Center { T("Loading…", 16, pal.muted) }
        } else if (videos.isEmpty()) {
            Center { T("No videos found on this phone.", 16, pal.muted, FontWeight.Normal, 2) }
        } else if (query.isNotEmpty()) {
            val hits = videos.filter {
                it.name.lowercase().contains(query) || it.show.lowercase().contains(query)
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(hits, key = { _, v -> v.id }) { i, v ->
                    EpisodeRow(v, tick, prefs, false) { onPlay(hits, i) }
                }
            }
        } else if (prefs.grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                gridItems(shows, key = { it.name }) { s ->
                    ShowCard(s) { onOpenShow(s) }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(shows, key = { it.name }) { s ->
                    ShowRow(s, tick, prefs) { onOpenShow(s) }
                }
            }
        }
    }
}

@Composable
fun ShowScreen(
    show: Show,
    tick: Int,
    prefs: Prefs,
    onBack: () -> Unit,
    onPlay: (List<Video>, Int) -> Unit
) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxSize().background(pal.bg)) {
        TopBar(show.name, onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            itemsIndexed(show.videos, key = { _, v -> v.id }) { i, v ->
                EpisodeRow(v, tick, prefs, show.name != Library.OTHER) { onPlay(show.videos, i) }
            }
        }
    }
}

@Composable
private fun Swatch(p: Pal, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cur = LocalPal.current
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(p.bg)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) cur.accent else cur.onBg.copy(alpha = 0.18f),
                    RoundedCornerShape(14.dp)
                )
                .padding(10.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(p.surface)
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(p.accent)
            )
        }
        T(p.name, 13, cur.onBg)
    }
}

@Composable
private fun SwatchGrid(list: List<Pal>, prefs: Prefs) {
    for (row in list.chunked(3)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (p in row) {
                Swatch(p, prefs.theme == p.name, Modifier.weight(1f)) { prefs.setTheme(p.name) }
            }
            for (i in 0 until (3 - row.size)) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val pal = LocalPal.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            T(title, 16, pal.onBg)
            if (sub != null) T(sub, 13, pal.muted, FontWeight.Normal, 3)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = pal.accent,
                checkedThumbColor = pal.onAccent,
                uncheckedTrackColor = pal.surface,
                uncheckedThumbColor = pal.muted,
                uncheckedBorderColor = pal.muted
            )
        )
    }
}

@Composable
fun AppearanceScreen(prefs: Prefs, onBack: () -> Unit) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxSize().background(pal.bg)) {
        TopBar("Appearance", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Dark")
            SwatchGrid(palettes.filter { !it.light }, prefs)
            SectionTitle("Light")
            SwatchGrid(palettes.filter { it.light }, prefs)
            SwitchRow("Pure black", "Use true black behind the dark themes", prefs.pureBlack) {
                prefs.setPureBlack(it)
            }
        }
    }
}

@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val pal = LocalPal.current
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // ignore
            }
            prefs.setSubFolder(uri.toString())
        }
    }
    val folder = prefs.subFolder
    val folderLabel = if (folder == null) {
        "Not set"
    } else {
        (Uri.parse(folder).lastPathSegment ?: "Selected").substringAfterLast(':')
    }

    Column(Modifier.fillMaxSize().background(pal.bg)) {
        TopBar("Settings", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SwitchRow(
                "Remember subtitles",
                "Use the last subtitle language and the subtitle folder on the next video",
                prefs.rememberSub
            ) { prefs.setRememberSub(it) }
            SwitchRow(
                "Automatic picture-in-picture",
                "Shrink the video into a small window when you leave the app",
                prefs.autoPip
            ) { prefs.setAutoPip(it) }

            SectionTitle("Short skip, seconds")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in listOf(5, 10, 15, 30)) {
                    Chip("$s", prefs.skipShort == s) { prefs.setSkipShort(s) }
                }
            }

            SectionTitle("Long skip, seconds")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in listOf(60, 85, 90, 120)) {
                    Chip("$s", prefs.skipLong == s) { prefs.setSkipLong(s) }
                }
            }

            SectionTitle("Subtitle folder")
            T(folderLabel, 15, pal.onBg, FontWeight.Normal, 2)
            T(
                "Pick the folder that holds your .srt, .vtt or .ass files. A subtitle with the same name as the video, or the same season and episode, is applied automatically.",
                13,
                pal.muted,
                FontWeight.Normal,
                5
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Choose folder", false) { picker.launch(null) }
                if (folder != null) Chip("Clear", false) { prefs.setSubFolder(null) }
            }
        }
    }
}
