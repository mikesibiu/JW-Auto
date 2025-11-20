package org.jw.library.auto.data.api

import org.jw.library.auto.data.model.api.PublicationMediaResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for jw.org content API
 * Base URL: https://b.jw-cdn.org/apis/pub-media/
 */
interface JWOrgApiService {

    /**
     * Get publication media links
     *
     * @param pub Publication code (mwb=Meeting Workbook, w=Watchtower, lfb=Lessons)
     * @param issue Issue code in YYYYMM format (e.g., "202511" for November 2025)
     * @param fileformat File format (MP3, AAC, etc.)
     * @param langwritten Language code (E=English, S=Spanish, etc.)
     * @param output Output format (json, html)
     */
    @GET("GETPUBMEDIALINKS")
    suspend fun getPublicationMedia(
        @Query("pub") pub: String,
        @Query("issue") issue: String? = null,
        @Query("fileformat") fileformat: String = "MP3",
        @Query("langwritten") langwritten: String = "E",
        @Query("output") output: String = "json",
        @Query("alllangs") alllangs: Int = 0,
        @Query("txtCMSLang") txtCMSLang: String = "E"
    ): PublicationMediaResponse
}
