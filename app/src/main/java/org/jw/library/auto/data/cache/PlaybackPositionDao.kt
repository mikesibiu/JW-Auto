package org.jw.library.auto.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackPositionDao {
    @Query("SELECT * FROM playback_positions WHERE mediaId = :mediaId")
    suspend fun get(mediaId: String): PlaybackPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PlaybackPosition)
}
