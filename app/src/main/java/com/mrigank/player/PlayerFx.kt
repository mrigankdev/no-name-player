package com.mrigank.player

import android.content.ContentResolver
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import java.util.Locale
import kotlin.math.log10

/** Volume boost and equalizer for the current audio session. */
internal class AudioFx {
    var boostPercent by mutableIntStateOf(100)
    var eqPreset by mutableIntStateOf(-1)
    private var enhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null

    fun bind(session: Int) {
        if (session == C.AUDIO_SESSION_ID_UNSET || session == 0) return
        try {
            enhancer?.release()
            enhancer = LoudnessEnhancer(session)
            applyBoost()
        } catch (e: Exception) {
            enhancer = null
        }
        try {
            equalizer?.release()
            val eq = Equalizer(0, session)
            equalizer = eq
            if (eqPreset >= 0 && eqPreset < eq.numberOfPresets.toInt()) {
                eq.usePreset(eqPreset.toShort())
                eq.setEnabled(true)
            } else {
                eq.setEnabled(false)
            }
        } catch (e: Exception) {
            equalizer = null
        }
    }

    fun setBoost(v: Int) {
        boostPercent = v
        applyBoost()
    }

    private fun applyBoost() {
        val fx = enhancer ?: return
        try {
            if (boostPercent <= 100) {
                fx.setEnabled(false)
            } else {
                fx.setTargetGain((2000.0 * log10(boostPercent / 100.0)).toInt())
                fx.setEnabled(true)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun eqNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try {
            val n = eq.numberOfPresets.toInt()
            (0 until n).map { eq.getPresetName(it.toShort()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setEq(index: Int) {
        eqPreset = index
        val eq = equalizer ?: return
        try {
            if (index < 0) {
                eq.setEnabled(false)
            } else {
                eq.usePreset(index.toShort())
                eq.setEnabled(true)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        try {
            enhancer?.release()
        } catch (e: Exception) {
            // ignore
        }
        try {
            equalizer?.release()
        } catch (e: Exception) {
            // ignore
        }
        enhancer = null
        equalizer = null
    }
}

internal fun plSubConfig(uri: Uri, name: String): MediaItem.SubtitleConfiguration {
    val ext = name.substringAfterLast('.', "").lowercase()
    val mime = when (ext) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> MimeTypes.APPLICATION_SUBRIP
    }
    return MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(mime)
        .setLabel(name)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()
}

internal fun plQueryName(resolver: ContentResolver, uri: Uri): String {
    try {
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getString(0)
                if (n != null) return n
            }
        }
    } catch (e: Exception) {
        // ignore
    }
    return uri.lastPathSegment ?: "subtitle.srt"
}

internal fun plTrackLabel(f: Format, n: Int): String {
    val label = f.label
    if (label != null && label.isNotBlank()) return label
    val lang = f.language
    if (lang != null && lang != "und") {
        val d = Locale.forLanguageTag(lang).displayLanguage
        return if (d.isNotBlank()) d else lang.uppercase()
    }
    return "Track $n"
}

internal fun plSpeedLabel(s: Float): String {
    return if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"
}
