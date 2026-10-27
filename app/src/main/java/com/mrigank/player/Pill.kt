package com.mrigank.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PlayerPill(onClick: () -> Unit) {
    val pal = LocalPal.current
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(pal.accent)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 18.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(Ic.Play, pal.onAccent, 24.dp)
        Spacer(Modifier.width(4.dp))
        T("Player", 15, pal.onAccent, FontWeight.SemiBold)
    }
}
