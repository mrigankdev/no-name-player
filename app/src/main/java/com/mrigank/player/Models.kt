package com.mrigank.player

import android.net.Uri

data class Video(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val size: Long,
    val dateAdded: Long,
    val show: String,
    val season: Int,
    val episode: Int
)

data class Show(
    val name: String,
    val videos: List<Video>
)

/** The list of videos the player should play. Set before opening the player. */
object PlayQueue {
    var videos: List<Video> = emptyList()
    var index: Int = 0
}
