package com.mrigank.player

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val tick = mutableIntStateOf(0)
    private var prefsRef: Prefs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(applicationContext)
        prefsRef = prefs
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
        // With "Resume playback" off, start the chosen video from the beginning.
        val p = prefsRef
        val first = list.getOrNull(index)
        if (p != null && first != null && !p.resumePlayback && p.position(first.id) > 0L) {
            p.setPosition(first.id, 0L)
        }
        PlayQueue.videos = list
        PlayQueue.index = index
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun share(v: Video) {
        try {
            val send = Intent(Intent.ACTION_SEND)
            send.type = "video/*"
            send.putExtra(Intent.EXTRA_STREAM, v.uri)
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, "Share video"))
        } catch (e: Exception) {
            toast("Could not share this video")
        }
    }

    @Composable
    private fun Root(prefs: Prefs) {
        val pal = LocalPal.current
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

        val rev = prefs.rev
        val groups = remember(videos, rev, prefs.sortKey, prefs.sortAsc) {
            Grouping.build(videos, prefs)
        }
        val lastId = remember(t, rev, videos) { LastPlayed.get(applicationContext) }
        val lastVideo = videos.firstOrNull { it.id == lastId }

        var tab by remember { mutableStateOf("video") }
        var sub by remember { mutableStateOf("") }
        var openKey by remember { mutableStateOf<String?>(null) }
        var dlg by remember { mutableStateOf<Dlg?>(null) }
        var pendingRename by remember { mutableStateOf<Pair<Video, String>?>(null) }

        val openGroup = openKey?.let { k -> groups.firstOrNull { it.key == k } }

        BackHandler(enabled = sub.isNotEmpty() || openGroup != null || tab != "video") {
            if (sub.isNotEmpty()) {
                sub = ""
            } else if (openGroup != null) {
                openKey = null
            } else {
                tab = "video"
            }
        }

        val writeLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { res ->
            val p = pendingRename
            pendingRename = null
            if (res.resultCode == Activity.RESULT_OK && p != null) {
                val ok = MediaOps.rename(applicationContext, p.first.uri, p.second)
                toast(if (ok) "Renamed" else "Could not rename")
                tick.intValue = tick.intValue + 1
            }
        }
        val deleteLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                toast("Deleted")
                tick.intValue = tick.intValue + 1
            }
        }

        fun doRename(v: Video, name: String) {
            if (Build.VERSION.SDK_INT >= 30) {
                pendingRename = Pair(v, name)
                val sender = MediaOps.writeSender(applicationContext, listOf(v.uri))
                writeLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                val ok = MediaOps.rename(applicationContext, v.uri, name)
                toast(if (ok) "Renamed" else "Could not rename")
                tick.intValue = tick.intValue + 1
            }
        }

        fun doDelete(list: List<Video>) {
            val uris = list.map { it.uri }
            if (Build.VERSION.SDK_INT >= 30) {
                val sender = MediaOps.deleteSender(applicationContext, uris)
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                MediaOps.deleteDirect(applicationContext, uris)
                toast("Deleted")
                tick.intValue = tick.intValue + 1
            }
        }

        val actions = Actions(
            play = { list, i -> play(list, i) },
            rename = { v -> dlg = Dlg.RenameVideo(v) },
            delete = { list -> dlg = Dlg.Delete(list) },
            share = { v -> share(v) },
            info = { v -> dlg = Dlg.Info(v) },
            move = { list -> dlg = Dlg.Move(list) },
            watched = { list, w -> prefs.markWatched(list.map { it.id }, w) },
            renameGroup = { g -> dlg = Dlg.RenameGroup(g) },
            ungroup = { g -> prefs.setGroups(g.videos.map { it.id }, "") }
        )

        Column(Modifier.fillMaxSize().background(pal.bg)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab == "music") {
                    MusicScreen()
                } else if (tab == "settings") {
                    if (sub == "appearance") {
                        AppearanceScreen(prefs) { sub = "" }
                    } else {
                        SettingsScreen(prefs) { sub = "appearance" }
                    }
                } else if (openGroup != null) {
                    GroupScreen(openGroup, prefs, actions) { openKey = null }
                } else {
                    HomeScreen(
                        videos = videos,
                        groups = groups,
                        loaded = loaded,
                        granted = granted,
                        lastId = lastId,
                        prefs = prefs,
                        actions = actions,
                        onOpenGroup = { g -> openKey = g.key },
                        onRefresh = { tick.intValue = tick.intValue + 1 },
                        onGrant = { launcher.launch(permission) }
                    )
                }

                if (tab == "video" && prefs.showPill && lastVideo != null) {
                    PlayerPill(
                        lastVideo.name,
                        {
                            val g = groups.firstOrNull { gr -> gr.videos.any { it.id == lastVideo.id } }
                            if (g != null) {
                                play(g.videos, g.videos.indexOfFirst { it.id == lastVideo.id })
                            } else {
                                play(listOf(lastVideo), 0)
                            }
                        },
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
            BottomBar(tab) { next ->
                tab = next
                sub = ""
                if (next != "video") openKey = null
            }
        }

        when (val d = dlg) {
            is Dlg.RenameVideo -> NameDialog(
                "Rename video",
                d.v.name,
                "Rename",
                { name ->
                    dlg = null
                    doRename(d.v, name)
                },
                { dlg = null }
            )
            is Dlg.RenameGroup -> NameDialog(
                "Rename group",
                d.g.name,
                "Rename",
                { name ->
                    dlg = null
                    prefs.renameGroup(d.g.key, name)
                },
                { dlg = null }
            )
            is Dlg.Delete -> ConfirmDialog(
                "Delete " + countText(d.list.size) + "?",
                "The files will be removed from your phone. This cannot be undone.",
                "Delete",
                {
                    dlg = null
                    doDelete(d.list)
                },
                { dlg = null }
            )
            is Dlg.Move -> MoveDialog(
                groups.filter { !it.key.startsWith("v:") },
                { g ->
                    dlg = null
                    prefs.setGroups(d.list.map { it.id }, g.key)
                },
                { name ->
                    dlg = null
                    val key = Grouping.newKey()
                    prefs.renameGroup(key, name)
                    prefs.setGroups(d.list.map { it.id }, key)
                },
                { dlg = null }
            )
            is Dlg.Info -> InfoDialog(d.v) { dlg = null }
            null -> {}
        }
    }
}
