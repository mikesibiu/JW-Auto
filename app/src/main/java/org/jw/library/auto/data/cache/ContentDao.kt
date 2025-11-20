package org.jw.library.auto.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for accessing cached content
 */
@Dao
interface ContentDao {

    @Query("SELECT * FROM cached_content WHERE cacheKey = :key")
    suspend fun getByKey(key: String): CachedContent?

    @Query("SELECT * FROM cached_content WHERE contentType = :type AND weekStart = :weekStart")
    suspend fun getByTypeAndWeek(type: String, weekStart: String): CachedContent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: CachedContent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<CachedContent>)

    @Query("DELETE FROM cached_content WHERE cacheKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM cached_content WHERE expiresAt < :timestamp")
    suspend fun deleteExpired(timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_content")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_content")
    suspend fun count(): Int

    @Query("SELECT * FROM cached_content WHERE expiresAt > :timestamp")
    suspend fun getAllValid(timestamp: Long = System.currentTimeMillis()): List<CachedContent>
}
