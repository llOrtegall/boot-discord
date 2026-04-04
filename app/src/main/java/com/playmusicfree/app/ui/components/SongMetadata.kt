package com.playmusicfree.app.ui.components

import com.playmusicfree.app.data.model.Song
import java.util.Locale

private val metadataPlaceholders = setOf(
    "",
    "<unknown>",
    "unknown",
    "unknown artist",
    "unknown album"
)

fun Song.displayTitle(): String = title.toDisplayValue()

fun Song.displayArtist(): String = artist.toDisplayValue()

fun Song.displayAlbum(): String = album.toDisplayValue()

private fun String?.toDisplayValue(): String {
    val normalized = this.orEmpty().trim()
    if (normalized.isEmpty()) return "X"

    return if (normalized.lowercase(Locale.ROOT) in metadataPlaceholders) {
        "X"
    } else {
        normalized
    }
}
