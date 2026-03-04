package org.jw.library.auto.data.meeting

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import org.jw.library.auto.R

class MeetingSectionsProvider(context: Context) {
    private val sections: Map<String, SectionEntry>

    init {
        val raw = context.resources.openRawResource(R.raw.meeting_sections)
            .bufferedReader()
            .use { it.readText() }
        val type = object : TypeToken<Map<String, SectionEntry>>() {}.type
        sections = Gson().fromJson(raw, type) ?: emptyMap()
    }

    fun bibleReadingUrls(weekStart: LocalDate): List<String> {
        return sections[weekStart.toString()]?.bibleReading.orEmpty()
    }

    fun congregationStudyUrls(weekStart: LocalDate): List<String> {
        return sections[weekStart.toString()]?.congregationStudy.orEmpty()
    }

    data class SectionEntry(
        val bibleReading: List<String> = emptyList(),
        val congregationStudy: List<String> = emptyList()
    )
}
