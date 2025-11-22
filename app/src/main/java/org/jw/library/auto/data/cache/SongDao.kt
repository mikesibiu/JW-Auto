package org.jw.library.auto.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongDao {

    @Query("SELECT * FROM kingdom_songs ORDER BY number ASC")
    suspend fun getAllSongs(): List<CachedSong>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<CachedSong>)

    @Query("DELETE FROM kingdom_songs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM kingdom_songs")
    suspend fun count(): Int
}
