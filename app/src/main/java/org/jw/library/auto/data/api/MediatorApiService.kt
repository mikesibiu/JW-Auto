package org.jw.library.auto.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MediatorApiService {
    @GET("categories/{language}/{category}")
    suspend fun getCategory(
        @Path("language") language: String,
        @Path("category") category: String,
        @Query("detailed") detailed: Int = 1
    ): MediatorCategoryResponse
}

data class MediatorCategoryResponse(
    @SerializedName("category") val category: MediatorCategory?
) {
    fun items(): List<MediatorMediaItem> = category?.media.orEmpty()
}

data class MediatorCategory(
    @SerializedName("key") val key: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("media") val media: List<MediatorMediaItem>?
)

data class MediatorMediaItem(
    @SerializedName("guid") val guid: String?,
    @SerializedName("naturalKey") val naturalKey: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("firstPublished") val firstPublished: String?,
    @SerializedName("duration") val durationSeconds: Double?,
    @SerializedName("files") val files: List<MediatorMediaFile>?
)

data class MediatorMediaFile(
    @SerializedName("label") val label: String?,
    @SerializedName("mimetype") val mimeType: String?,
    @SerializedName("progressiveDownloadURL") val url: String?
)
