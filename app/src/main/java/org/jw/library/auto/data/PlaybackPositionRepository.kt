package org.jw.library.auto.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jw.library.auto.data.cache.ContentDatabase
import org.jw.library.auto.data.cache.PlaybackPosition

class PlaybackPositionRepository(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = ContentDatabase.getDatabase(context).playbackPositionDao()

    suspend fun get(mediaId: String): Long = withContext(dispatcher) {
        dao.get(mediaId)?.positionMs ?: 0L
    }

    suspend fun save(mediaId: String, positionMs: Long) = withContext(dispatcher) {
        dao.upsert(
            PlaybackPosition(
                mediaId = mediaId,
                positionMs = positionMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
