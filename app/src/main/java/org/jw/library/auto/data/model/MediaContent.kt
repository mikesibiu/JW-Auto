package org.jw.library.auto.data.model

data class MediaContent(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val streamUrl: String? = null,
    val isBrowsable: Boolean = false,
    val playlistUrls: List<String> = emptyList(),
)
