package org.jw.library.auto.data.bible

/**
 * Catalog of Bible books grouped by Hebrew (Old Testament) and Greek (New Testament) scriptures.
 */
object BibleBooks {

    enum class Testament { HEBREW, GREEK }

    data class Book(
        val number: Int,
        val abbreviation: String,
        val title: String,
        val testament: Testament
    )

    private val BOOKS = listOf(
        Book(1, "Gen", "Genesis", Testament.HEBREW),
        Book(2, "Ex", "Exodus", Testament.HEBREW),
        Book(3, "Lev", "Leviticus", Testament.HEBREW),
        Book(4, "Num", "Numbers", Testament.HEBREW),
        Book(5, "Deut", "Deuteronomy", Testament.HEBREW),
        Book(6, "Josh", "Joshua", Testament.HEBREW),
        Book(7, "Judg", "Judges", Testament.HEBREW),
        Book(8, "Ruth", "Ruth", Testament.HEBREW),
        Book(9, "1 Sam", "1 Samuel", Testament.HEBREW),
        Book(10, "2 Sam", "2 Samuel", Testament.HEBREW),
        Book(11, "1 Ki", "1 Kings", Testament.HEBREW),
        Book(12, "2 Ki", "2 Kings", Testament.HEBREW),
        Book(13, "1 Chron", "1 Chronicles", Testament.HEBREW),
        Book(14, "2 Chron", "2 Chronicles", Testament.HEBREW),
        Book(15, "Ezra", "Ezra", Testament.HEBREW),
        Book(16, "Neh", "Nehemiah", Testament.HEBREW),
        Book(17, "Esther", "Esther", Testament.HEBREW),
        Book(18, "Job", "Job", Testament.HEBREW),
        Book(19, "Ps", "Psalms", Testament.HEBREW),
        Book(20, "Prov", "Proverbs", Testament.HEBREW),
        Book(21, "Eccl", "Ecclesiastes", Testament.HEBREW),
        Book(22, "Song", "Song of Solomon", Testament.HEBREW),
        Book(23, "Isa", "Isaiah", Testament.HEBREW),
        Book(24, "Jer", "Jeremiah", Testament.HEBREW),
        Book(25, "Lam", "Lamentations", Testament.HEBREW),
        Book(26, "Ezek", "Ezekiel", Testament.HEBREW),
        Book(27, "Dan", "Daniel", Testament.HEBREW),
        Book(28, "Hos", "Hosea", Testament.HEBREW),
        Book(29, "Joel", "Joel", Testament.HEBREW),
        Book(30, "Amos", "Amos", Testament.HEBREW),
        Book(31, "Obad", "Obadiah", Testament.HEBREW),
        Book(32, "Jonah", "Jonah", Testament.HEBREW),
        Book(33, "Mic", "Micah", Testament.HEBREW),
        Book(34, "Nah", "Nahum", Testament.HEBREW),
        Book(35, "Hab", "Habakkuk", Testament.HEBREW),
        Book(36, "Zeph", "Zephaniah", Testament.HEBREW),
        Book(37, "Hag", "Haggai", Testament.HEBREW),
        Book(38, "Zech", "Zechariah", Testament.HEBREW),
        Book(39, "Mal", "Malachi", Testament.HEBREW),
        Book(40, "Matt", "Matthew", Testament.GREEK),
        Book(41, "Mark", "Mark", Testament.GREEK),
        Book(42, "Luke", "Luke", Testament.GREEK),
        Book(43, "John", "John", Testament.GREEK),
        Book(44, "Acts", "Acts", Testament.GREEK),
        Book(45, "Rom", "Romans", Testament.GREEK),
        Book(46, "1 Cor", "1 Corinthians", Testament.GREEK),
        Book(47, "2 Cor", "2 Corinthians", Testament.GREEK),
        Book(48, "Gal", "Galatians", Testament.GREEK),
        Book(49, "Eph", "Ephesians", Testament.GREEK),
        Book(50, "Phil", "Philippians", Testament.GREEK),
        Book(51, "Col", "Colossians", Testament.GREEK),
        Book(52, "1 Thes", "1 Thessalonians", Testament.GREEK),
        Book(53, "2 Thes", "2 Thessalonians", Testament.GREEK),
        Book(54, "1 Tim", "1 Timothy", Testament.GREEK),
        Book(55, "2 Tim", "2 Timothy", Testament.GREEK),
        Book(56, "Titus", "Titus", Testament.GREEK),
        Book(57, "Philem", "Philemon", Testament.GREEK),
        Book(58, "Heb", "Hebrews", Testament.GREEK),
        Book(59, "Jas", "James", Testament.GREEK),
        Book(60, "1 Pet", "1 Peter", Testament.GREEK),
        Book(61, "2 Pet", "2 Peter", Testament.GREEK),
        Book(62, "1 John", "1 John", Testament.GREEK),
        Book(63, "2 John", "2 John", Testament.GREEK),
        Book(64, "3 John", "3 John", Testament.GREEK),
        Book(65, "Jude", "Jude", Testament.GREEK),
        Book(66, "Rev", "Revelation", Testament.GREEK),
    )

    fun booksFor(testament: Testament): List<Book> = BOOKS.filter { it.testament == testament }

    fun findByName(text: String): Book? {
        val normalized = text.lowercase()
        return BOOKS.firstOrNull {
            normalized.contains(it.title.lowercase()) ||
                normalized.contains(it.abbreviation.lowercase())
        }
    }
}
