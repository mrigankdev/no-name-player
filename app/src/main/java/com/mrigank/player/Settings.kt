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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
private fun SectionLabel(text: String) {
    val pal = LocalPal.current
    T(text, 13, pal.accent, FontWeight.SemiBold, 1, Modifier.padding(top = 12.dp))
}

@Composable
private fun NavRow(title: String, sub: String?, onClick: () -> Unit) {
    val pal = LocalPal.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            T(title, 16, pal.onBg)
            if (sub != null) T(sub, 13, pal.muted, FontWeight.Normal, 2)
        }
        Nglyph(Nic.Move, pal.muted, 20.dp)
    }
}

@Composable
private fun ChipRow(
    title: String,
    sub: String,
    options: List<Int>,
    current: Int,
    onPick: (Int) -> Unit
) {
    val pal = LocalPal.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            T(title, 16, pal.onBg)
            T(sub, 13, pal.muted, FontWeight.Normal, 2)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in options) {
                Chip("$n s", current == n) { onPick(n) }
            }
        }
    }
}

@Composable
fun SettingsScreen(prefs: Prefs, onAppearance: () -> Unit) {
    val pal = LocalPal.current
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // ignore
            }
            prefs.changeSubFolder(uri.toString())
        }
    }
    val folder = prefs.subFolder
    val folderLabel = if (folder == null) {
        "Not set"
    } else {
        (Uri.parse(folder).lastPathSegment ?: "Selected").substringAfterLast(':')
    }
    val modeLabel = when (prefs.mode) {
        "light" -> "Light"
        "dark" -> "Dark"
        else -> "Follow system"
    }
    val themeLabel = if (prefs.mode == "light") prefs.lightTheme else prefs.darkTheme

    Column(
        Modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding()
    ) {
        T("Settings", 26, pal.onBg, FontWeight.Bold, 1, Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionLabel("Playback")
            SwitchRow(
                "Resume playback",
                "Continue a video from where you stopped",
                prefs.resumePlayback
            ) { prefs.changeResume(it) }
            ChipRow(
                "Fast forward and rewind",
                "How far the small skip buttons jump",
                listOf(5, 10, 15, 30),
                prefs.skipShort
            ) { prefs.changeSkipShort(it) }
            ChipRow(
                "Long skip",
                "How far the big skip buttons jump",
                listOf(60, 85, 90, 120),
                prefs.skipLong
            ) { prefs.changeSkipLong(it) }
            SwitchRow(
                "Auto picture-in-picture",
                "Keep playing in a small window when you leave the app",
                prefs.autoPip
            ) { prefs.changeAutoPip(it) }

            SectionLabel("Subtitles")
            SwitchRow(
                "Remember subtitles",
                "Reuse your last subtitle and audio language",
                prefs.rememberSub
            ) { prefs.changeRememberSub(it) }
            NavRow("Subtitle folder", folderLabel) { picker.launch(null) }
            if (folder != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(pal.onBg.copy(alpha = 0.10f))
                        .clickable { prefs.changeSubFolder(null) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    T("Clear folder", 14, pal.onBg, FontWeight.SemiBold)
                }
            }

            SectionLabel("Home")
            SwitchRow(
                "Show floating play button",
                "A blue button that continues your last video",
                prefs.showPill
            ) { prefs.changeShowPill(it) }

            SectionLabel("Look")
            NavRow("Appearance", "$modeLabel · $themeLabel", onAppearance)

            SectionLabel("About")
            Column {
                T("Shiro Player", 16, pal.onBg)
                T("Version 1.0", 13, pal.muted)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------- Appearance ----------

@Composable
private fun Swatch(p: Pal, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pal = LocalPal.current
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(p.bg)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) pal.onBg else pal.onBg.copy(alpha = 0.18f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(p.accent)
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(p.surface)
            )
        }
        Spacer(Modifier.height(6.dp))
        T(p.name, 12, if (selected) pal.onBg else pal.muted, if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun SwatchGrid(list: List<Pal>, selectedName: String, onPick: (Pal) -> Unit) {
    val perRow = 4
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (row in list.chunked(perRow)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in row) {
                    Swatch(p, p.name == selectedName, Modifier.weight(1f)) { onPick(p) }
                }
                for (i in 0 until (perRow - row.size)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pal = LocalPal.current
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) pal.accent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        T(label, 14, if (on) pal.onAccent else pal.muted, if (on) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
fun AppearanceScreen(prefs: Prefs, onBack: () -> Unit) {
    val pal = LocalPal.current

    Column(
        Modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBtn(Ic.Back, pal.onBg, onBack)
            T("Appearance", 22, pal.onBg, FontWeight.SemiBold, 1, Modifier.padding(start = 4.dp))
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Mode")
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(pal.surface)
                    .padding(3.dp)
            ) {
                ModeButton("Follow system", prefs.mode == "system", Modifier.weight(1f)) { prefs.changeMode("system") }
                ModeButton("Dark", prefs.mode == "dark", Modifier.weight(1f)) { prefs.changeMode("dark") }
                ModeButton("Light", prefs.mode == "light", Modifier.weight(1f)) { prefs.changeMode("light") }
            }

            SectionLabel("Dark themes")
            SwatchGrid(palettes.filter { !it.light }, prefs.darkTheme) { p ->
                prefs.changeDarkTheme(p.name)
                if (prefs.mode == "light") prefs.changeMode("dark")
            }

            SectionLabel("Light themes")
            SwatchGrid(palettes.filter { it.light }, prefs.lightTheme) { p ->
                prefs.changeLightTheme(p.name)
                if (prefs.mode == "dark") prefs.changeMode("light")
            }

            SectionLabel("Accent color")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip("Theme", prefs.accentIdx < 0) { prefs.changeAccent(-1) }
                for (i in accents.indices) {
                    val on = prefs.accentIdx == i
                    val ring = if (on) Modifier.border(3.dp, pal.onBg, CircleShape) else Modifier
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(accents[i]))
                            .then(ring)
                            .clickable { prefs.changeAccent(i) }
                    )
                }
            }

            SwitchRow(
                "Pure black",
                "Use true black behind the dark themes",
                prefs.pureBlack
            ) { prefs.changePureBlack(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}
