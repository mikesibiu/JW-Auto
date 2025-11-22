package org.jw.library.auto.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kingdom_songs")
data class CachedSong(
    @PrimaryKey val number: Int,
    val title: String,
    val url: String,
    val language: String,
    val fetchedAt: Long
)
