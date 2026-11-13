package com.mrigank.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Bottom bar. tab is "video", "music" or "settings". */
@Composable
fun BottomBar(tab: String, onTab: (String) -> Unit) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxWidth().background(pal.bg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(pal.onBg.copy(alpha = 0.08f)))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 8.dp)
        ) {
            BarItem("Video", Nic.Video, tab == "video") { onTab("video") }
            BarItem("Music", Nic.Music, tab == "music") { onTab("music") }
            BarItem("Settings", Nic.Gear, tab == "settings") { onTab("settings") }
        }
    }
}

@Composable
private fun RowScope.BarItem(label: String, ic: Nic, on: Boolean, onClick: () -> Unit) {
    val pal = LocalPal.current
    val pillColor by animateColorAsState(
        if (on) pal.accent.copy(alpha = 0.22f) else pal.accent.copy(alpha = 0f),
        tween(220),
        label = "tabPill"
    )
    val tint by animateColorAsState(if (on) pal.onBg else pal.muted, tween(220), label = "tabTint")
    Column(
        Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Nglyph(ic, tint, 24.dp)
        }
        Spacer(Modifier.height(4.dp))
        T(
            label,
            12,
            tint,
            if (on) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/** The Music tab is not ready yet. */
@Composable
fun MusicScreen() {
    val pal = LocalPal.current
    Column(
        Modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Nglyph(Nic.Music, pal.muted, 48.dp)
        Spacer(Modifier.height(16.dp))
        T("Music", 22, pal.onBg, FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        T("Coming soon", 15, pal.muted)
    }
}
