package org.jw.library.auto.data.model.api

import com.google.gson.annotations.SerializedName

/**
 * Represents a single media file from jw.org API
 */
data class MediaFile(
    @SerializedName("title")
    val title: String?,

    @SerializedName("file")
    val file: FileInfo?,

    @SerializedName("label")
    val label: String?,

    @SerializedName("track")
    val track: Int?
)

data class FileInfo(
    @SerializedName("url")
    val url: String?,

    @SerializedName("filesize")
    val filesize: Long?,

    @SerializedName("duration")
    val duration: Int?,

    @SerializedName("bitRate")
    val bitRate: Int?
)
