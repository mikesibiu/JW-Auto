package org.jw.library.auto.data.model

data class BibleDrama(
    val id: String,
    val title: String,
    val streamUrl: String,
    val description: String? = null,
    val durationSeconds: Int? = null,
)
