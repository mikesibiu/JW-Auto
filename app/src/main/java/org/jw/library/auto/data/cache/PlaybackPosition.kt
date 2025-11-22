package org.jw.library.auto.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_positions")
data class PlaybackPosition(
    @PrimaryKey val mediaId: String,
    val positionMs: Long,
    val updatedAt: Long
)
