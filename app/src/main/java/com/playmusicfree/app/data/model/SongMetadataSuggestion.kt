package com.playmusicfree.app.data.model

import android.net.Uri

data class SongMetadataSuggestion(
    val songId: Long,
    val originalTitle: String,
    val originalArtist: String,
    val originalAlbum: String,
    val suggestedTitle: String,
    val suggestedArtist: String,
    val suggestedAlbum: String,
    val suggestedArtworkUri: Uri?
)
