package org.jw.library.auto.data

import android.content.Context
import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jw.library.auto.R
import org.jw.library.auto.data.api.MediatorApiClient
import org.jw.library.auto.data.api.MediatorMediaItem
import org.jw.library.auto.data.model.MediaContent

class JWBroadcastRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mediatorService: org.jw.library.auto.data.api.MediatorApiService = MediatorApiClient.service
) {
    companion object {
        private const val TAG = "JWBroadcastRepository"
        private const val LANGUAGE = "E"
        private const val CATEGORY_MONTHLY = "StudioMonthlyPrograms"
        private const val CATEGORY_GB_UPDATES = "StudioNewsReports"
        private val ONE_YEAR = Duration.ofDays(365)
    }

    private val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    suspend fun latestBroadcasts(): List<MediaContent> = withContext(dispatcher) {
        val cutoff = Instant.now().minus(ONE_YEAR)
        val monthly = runCatching { loadCategory(CATEGORY_MONTHLY) }.getOrElse { emptyList() }
        val updates = runCatching { loadCategory(CATEGORY_GB_UPDATES) }.getOrElse { emptyList() }

        (monthly + updates)
            .filter { it.firstPublishedInstant?.isAfter(cutoff) ?: false }
            .sortedByDescending { it.firstPublishedInstant }
            .mapNotNull { it.toMediaContent(context) }
    }

    private suspend fun loadCategory(category: String): List<MediatorMediaWrapper> {
        return try {
            mediatorService.getCategory(LANGUAGE, category).items()
                .mapNotNull { MediatorMediaWrapper(category, it) }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch $category", t)
            emptyList()
        }
    }

    private inner class MediatorMediaWrapper(
        val sourceCategory: String,
        private val item: MediatorMediaItem
    ) {
        val firstPublishedInstant: Instant? = item.firstPublished?.let {
            runCatching { Instant.parse(it) }.getOrNull()
        }

        fun toMediaContent(context: Context): MediaContent? {
            val url = item.files?.firstNotNullOfOrNull { it.url }
                ?: return null
            val title = item.title ?: context.getString(
                if (sourceCategory == CATEGORY_GB_UPDATES) R.string.content_broadcasting_update else R.string.content_broadcasting_monthly
            )
            val subtitle = firstPublishedInstant?.let(formatter::format)
            val id = "broadcast-${sourceCategory}-${item.guid ?: item.naturalKey ?: url.hashCode()}"
            return MediaContent(
                id = id,
                title = title,
                subtitle = subtitle,
                streamUrl = url
            )
        }
    }
}
