package com.mrigank.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Shows the Shiro logo and name once when the app opens, then fades into the app. */
@Composable
fun ShiroSplashHost(content: @Composable () -> Unit) {
    var showing by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        content()
        if (showing) ShiroSplash { showing = false }
    }
}

@Composable
private fun ShiroSplash(onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    val leave = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        delay(950)
        leave.animateTo(0f, tween(350))
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = leave.value }
            .background(Color(0xFF0B0C10))
            .pointerInput(Unit) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.graphicsLayer {
                alpha = appear.value
                val s = 0.86f + 0.14f * appear.value
                scaleX = s
                scaleY = s
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painterResource(R.drawable.shiro_symbol), null, Modifier.size(150.dp))
            Spacer(Modifier.height(14.dp))
            Row {
                Text("Shiro", color = Color(0xFFF2F3F5), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(" Player", color = Color(0xFF3985FF), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
