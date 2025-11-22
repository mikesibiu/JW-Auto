package org.jw.library.auto.data.bible

import org.jw.library.auto.data.JWOrgRepository
import org.jw.library.auto.data.model.api.MediaFile

/** Utility that groups jw.org MP3 entries by book number. */
object BibleAudioParser {

    fun groupByBook(files: List<MediaFile>): Map<Int, JWOrgRepository.BibleBookAudio> {
        return files
            .mapNotNull { media ->
                val book = media.bookNumber
                    ?: BibleBooks.findByName(media.title.orEmpty())?.number
                    ?: return@mapNotNull null
                val url = media.file?.url ?: return@mapNotNull null
                val track = media.track ?: 0
                Triple(book, track, url)
            }
            .groupBy({ it.first }) { it.second to it.third }
            .mapValues { entry ->
                val chapters = entry.value.sortedBy { it.first }.map { it.second }
                JWOrgRepository.BibleBookAudio(intro = null, chapters = chapters)
            }
    }
}

