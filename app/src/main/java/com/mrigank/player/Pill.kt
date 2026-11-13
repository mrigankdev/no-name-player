package com.mrigank.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The blue button at the bottom right. Tap it to continue the last video. */
@Composable
fun PlayerPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Row(
        modifier
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(pal.accent)
            .clickable(onClick = onClick)
            .height(52.dp)
            .padding(start = 14.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(Ic.Play, pal.onAccent, 24.dp)
        Spacer(Modifier.width(6.dp))
        T(label, 15, pal.onAccent, FontWeight.SemiBold, 1, Modifier.widthIn(max = 150.dp))
    }
}
