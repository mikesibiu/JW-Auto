package org.jw.library.auto.data.model.api

import com.google.gson.annotations.SerializedName

/**
 * Response from jw.org GETPUBMEDIALINKS API
 * Endpoint: https://b.jw-cdn.org/apis/pub-media/GETPUBMEDIALINKS
 */
data class PublicationMediaResponse(
    @SerializedName("files")
    val files: Map<String, LanguageFiles>?
)

data class LanguageFiles(
    @SerializedName("MP3")
    val mp3Files: List<MediaFile>?,

    @SerializedName("AAC")
    val aacFiles: List<MediaFile>?
)
