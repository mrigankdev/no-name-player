package com.mrigank.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything a row or menu can ask the app to do. MainActivity fills this in. */
class Actions(
    val play: (List<Video>, Int) -> Unit,
    val rename: (Video) -> Unit,
    val delete: (List<Video>) -> Unit,
    val share: (Video) -> Unit,
    val info: (Video) -> Unit,
    val move: (List<Video>) -> Unit,
    val watched: (List<Video>, Boolean) -> Unit,
    val renameGroup: (Group) -> Unit,
    val ungroup: (Group) -> Unit
)

/** Which pop-up is open right now. */
sealed class Dlg {
    class RenameVideo(val v: Video) : Dlg()
    class RenameGroup(val g: Group) : Dlg()
    class Delete(val list: List<Video>) : Dlg()
    class Move(val list: List<Video>) : Dlg()
    class Info(val v: Video) : Dlg()
}

fun sizeText(bytes: Long): String {
    val mb = bytes / 1048576.0
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}

fun dateText(seconds: Long): String {
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(seconds * 1000L))
}

// ---------- Menus ----------

@Composable
fun MenuRow(ic: Nic, text: String, danger: Boolean = false, onClick: () -> Unit) {
    val pal = LocalPal.current
    val c = if (danger) Color(0xFFEF5350) else pal.onBg
    DropdownMenuItem(
        text = { T(text, 15, c) },
        leadingIcon = { Nglyph(ic, c, 22.dp) },
        onClick = onClick
    )
}

@Composable
fun DotsMenu(content: @Composable (close: () -> Unit) -> Unit) {
    val pal = LocalPal.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconBtn(Ic.More, pal.muted, { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(pal.surface)
        ) {
            content { open = false }
        }
    }
}

@Composable
fun VideoMenu(v: Video, prefs: Prefs, actions: Actions) {
    DotsMenu { close ->
        val watched = prefs.position(v.id) < 0L
        MenuRow(Nic.Edit, "Rename") {
            close()
            actions.rename(v)
        }
        MenuRow(Nic.Move, "Move to group") {
            close()
            actions.move(listOf(v))
        }
        MenuRow(Nic.Check, if (watched) "Mark as unwatched" else "Mark as watched") {
            close()
            actions.watched(listOf(v), !watched)
        }
        MenuRow(Nic.Share, "Share") {
            close()
            actions.share(v)
        }
        MenuRow(Nic.Info, "Info") {
            close()
            actions.info(v)
        }
        MenuRow(Nic.Trash, "Delete", true) {
            close()
            actions.delete(listOf(v))
        }
    }
}

@Composable
fun GroupMenu(g: Group, actions: Actions) {
    DotsMenu { close ->
        MenuRow(Nic.Edit, "Rename group") {
            close()
            actions.renameGroup(g)
        }
        MenuRow(Nic.Folder, "Ungroup") {
            close()
            actions.ungroup(g)
        }
        MenuRow(Nic.Check, "Mark all as watched") {
            close()
            actions.watched(g.videos, true)
        }
        MenuRow(Nic.Trash, "Delete videos", true) {
            close()
            actions.delete(g.videos)
        }
    }
}

// ---------- Dialogs ----------

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit
) {
    val pal = LocalPal.current
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = pal.surface,
        title = { T(title, 18, pal.onBg, FontWeight.SemiBold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = pal.onBg,
                    unfocusedTextColor = pal.onBg,
                    focusedBorderColor = pal.accent,
                    unfocusedBorderColor = pal.muted,
                    cursorColor = pal.accent
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) onDone(text.trim())
            }) {
                T(confirm, 15, pal.accent, FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                T("Cancel", 15, pal.muted)
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val pal = LocalPal.current
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = pal.surface,
        title = { T(title, 18, pal.onBg, FontWeight.SemiBold) },
        text = { T(message, 15, pal.muted, FontWeight.Normal, 6) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                T(confirm, 15, Color(0xFFEF5350), FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                T("Cancel", 15, pal.muted)
            }
        }
    )
}

@Composable
fun InfoDialog(v: Video, onClose: () -> Unit) {
    val pal = LocalPal.current
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = pal.surface,
        title = { T("Info", 18, pal.onBg, FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("Name", v.name)
                InfoLine("Length", fmt(v.durationMs))
                InfoLine("Size", sizeText(v.size))
                InfoLine("Added", dateText(v.dateAdded))
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                T("Close", 15, pal.accent, FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    val pal = LocalPal.current
    Column {
        T(label, 12, pal.muted)
        T(value, 15, pal.onBg, FontWeight.Normal, 4)
    }
}

@Composable
fun MoveDialog(
    groups: List<Group>,
    onPick: (Group) -> Unit,
    onNew: (String) -> Unit,
    onCancel: () -> Unit
) {
    val pal = LocalPal.current
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog("New group", "", "Create", { name -> onNew(name) }, { creating = false })
    } else {
        AlertDialog(
            onDismissRequest = onCancel,
            containerColor = pal.surface,
            title = { T("Move to group", 18, pal.onBg, FontWeight.SemiBold) },
            text = {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { creating = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Nglyph(Nic.Folder, pal.accent, 22.dp)
                            Spacer(Modifier.width(14.dp))
                            T("New group", 16, pal.accent, FontWeight.SemiBold)
                        }
                    }
                    items(groups) { g ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(g) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Nglyph(Nic.Folder, pal.muted, 22.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                T(g.name, 16, pal.onBg)
                                T(countText(g.videos.size), 12, pal.muted)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onCancel) {
                    T("Cancel", 15, pal.muted)
                }
            }
        )
    }
}

private fun orderLabels(key: String): Pair<String, String> {
    return when (key) {
        "date" -> Pair("Oldest first", "Newest first")
        "length" -> Pair("Shortest first", "Longest first")
        "size" -> Pair("Smallest first", "Largest first")
        else -> Pair("A to Z", "Z to A")
    }
}

@Composable
fun SortDialog(prefs: Prefs, onClose: () -> Unit) {
    val pal = LocalPal.current
    val options = listOf(
        Pair("name", "Name"),
        Pair("date", "Date"),
        Pair("length", "Length"),
        Pair("size", "Size")
    )
    val labels = orderLabels(prefs.sortKey)
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = pal.surface,
        title = { T("Sort by", 18, pal.onBg, FontWeight.SemiBold) },
        text = {
            Column {
                for (o in options) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { prefs.changeSort(o.first, prefs.sortAsc) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        T(
                            o.second,
                            16,
                            if (prefs.sortKey == o.first) pal.accent else pal.onBg,
                            if (prefs.sortKey == o.first) FontWeight.SemiBold else FontWeight.Normal,
                            1,
                            Modifier.weight(1f)
                        )
                        if (prefs.sortKey == o.first) Nglyph(Nic.Check, pal.accent, 22.dp)
                    }
                }
                Spacer(Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(labels.first, prefs.sortAsc) { prefs.changeSort(prefs.sortKey, true) }
                    Chip(labels.second, !prefs.sortAsc) { prefs.changeSort(prefs.sortKey, false) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                T("Done", 15, pal.accent, FontWeight.SemiBold)
            }
        }
    )
}
