package org.jw.library.auto.data.bible

import org.jw.library.auto.data.model.api.FileInfo
import org.jw.library.auto.data.model.api.MediaFile
import org.junit.Assert.assertEquals
import org.junit.Test

class BibleAudioParserTest {

    @Test
    fun `groups chapters by book number ordered by track`() {
        val files = listOf(
            media(book = 40, track = 2, url = "chapter-2"),
            media(book = 40, track = 1, url = "chapter-1"),
            media(book = 41, track = 1, url = "mark-1"),
        )

        val grouped = BibleAudioParser.groupByBook(files)

        val matthew = grouped[40] ?: error("missing book 40")
        assertEquals(listOf("chapter-1", "chapter-2"), matthew.chapters)
        assertEquals(null, matthew.intro)

        val mark = grouped[41] ?: error("missing book 41")
        assertEquals(listOf("mark-1"), mark.chapters)
    }

    private fun media(book: Int, track: Int, url: String) =
        MediaFile(
            title = "",
            file = FileInfo(url = url, filesize = null, duration = null, bitRate = null),
            label = null,
            track = track,
            bookNumber = book
        )
}
