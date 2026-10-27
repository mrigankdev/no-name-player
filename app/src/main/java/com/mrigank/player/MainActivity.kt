package com.mrigank.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(applicationContext)
        setContent {
            AppTheme(prefs) {
                Root(prefs)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue = tick.intValue + 1
    }

    private fun play(list: List<Video>, index: Int) {
        PlayQueue.videos = list
        PlayQueue.index = index
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    @Composable
    private fun Root(prefs: Prefs) {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        var granted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED
            )
        }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
        }
        LaunchedEffect(Unit) {
            if (!granted) launcher.launch(permission)
        }

        var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
        var loaded by remember { mutableStateOf(false) }
        val t = tick.intValue
        LaunchedEffect(granted, t) {
            if (granted) {
                videos = withContext(Dispatchers.IO) { Library.scan(applicationContext) }
                loaded = true
            }
        }
        val shows = remember(videos) { Library.groups(videos) }

        var screen by remember { mutableStateOf("home") }
        var showName by remember { mutableStateOf("") }
        BackHandler(enabled = screen != "home") { screen = "home" }

        val openShow = shows.firstOrNull { it.name == showName }

        Box(Modifier.fillMaxSize().background(LocalPal.current.bg)) {
            if (screen == "show" && openShow != null) {
                ShowScreen(
                    show = openShow,
                    tick = t,
                    prefs = prefs,
                    onBack = { screen = "home" },
                    onPlay = { list, i -> play(list, i) }
                )
            } else if (screen == "appearance") {
                AppearanceScreen(prefs) { screen = "home" }
            } else if (screen == "settings") {
                SettingsScreen(prefs) { screen = "home" }
            } else {
                HomeScreen(
                    shows = shows,
                    videos = videos,
                    loaded = loaded,
                    granted = granted,
                    prefs = prefs,
                    tick = t,
                    onOpenShow = { s ->
                        showName = s.name
                        screen = "show"
                    },
                    onPlay = { list, i -> play(list, i) },
                    onAppearance = { screen = "appearance" },
                    onSettings = { screen = "settings" },
                    onRefresh = { tick.intValue = tick.intValue + 1 },
                    onGrant = { launcher.launch(permission) }
                )
            }
        }
    }
}
