package org.jw.library.auto.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jw.library.auto.data.JWOrgRepository
import org.jw.library.auto.data.cache.ContentDatabase
import org.jw.library.auto.util.WeekCalculator
import java.time.Clock

/**
 * Background worker to sync content from jw.org API
 * Pre-fetches content for this week + next 3 weeks
 */
class ContentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = JWOrgRepository(context)
    private val weekCalculator = WeekCalculator(Clock.systemDefaultZone())
    private val contentDao = ContentDatabase.getDatabase(context).contentDao()

    companion object {
        private const val TAG = "ContentSyncWorker"
        const val WORK_NAME = "content_sync"

        // How many weeks ahead to pre-fetch
        private const val WEEKS_TO_PREFETCH = 3
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.i(TAG, "Starting content sync...")

            // Clean up expired cache entries
            contentDao.deleteExpired()

            // Pre-fetch this week + next 3 weeks
            for (offset in 0..WEEKS_TO_PREFETCH) {
                val week = weekCalculator.weekForOffset(offset.toLong())
                Log.d(TAG, "Syncing content for week: ${week.label} (${week.weekStart})")

                // Fetch all content types for this week
                try {
                    repository.getMeetingWorkbookUrl(week.weekStart)
                    repository.getWatchtowerUrl(week.weekStart)
                    repository.getBibleReadingUrls(week.weekStart)
                    repository.getCongregationStudyUrls(week.weekStart)

                    Log.d(TAG, "Successfully synced week: ${week.label}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync week ${week.label}: ${e.message}")
                    // Continue with other weeks even if one fails
                }
            }

            // Refresh Kingdom songs catalog separately (weekly cadence is sufficient)
            try {
                val songs = repository.getKingdomSongs(forceRefresh = true)
                Log.d(TAG, "Synced ${songs.size} kingdom songs")
            } catch (songError: Exception) {
                Log.w(TAG, "Failed to sync kingdom songs", songError)
            }

            Log.i(TAG, "Content sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Content sync failed", e)
            Result.retry()
        }
    }
}
