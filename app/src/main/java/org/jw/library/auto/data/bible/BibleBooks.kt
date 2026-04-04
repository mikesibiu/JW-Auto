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

    /** Romanian book names: number → (abbreviation, full title) */
    private val RO_NAMES: Map<Int, Pair<String, String>> = mapOf(
        1 to ("Gen" to "Geneza"),
        2 to ("Ex" to "Exodul"),
        3 to ("Lev" to "Leviticul"),
        4 to ("Num" to "Numeri"),
        5 to ("Deut" to "Deuteronomul"),
        6 to ("Ios" to "Iosua"),
        7 to ("Jud" to "Judecători"),
        8 to ("Rut" to "Rut"),
        9 to ("1 Sam" to "1 Samuel"),
        10 to ("2 Sam" to "2 Samuel"),
        11 to ("1 Reg" to "1 Regi"),
        12 to ("2 Reg" to "2 Regi"),
        13 to ("1 Cron" to "1 Cronici"),
        14 to ("2 Cron" to "2 Cronici"),
        15 to ("Ezra" to "Ezra"),
        16 to ("Nee" to "Neemia"),
        17 to ("Est" to "Estera"),
        18 to ("Iov" to "Iov"),
        19 to ("Ps" to "Psalmii"),
        20 to ("Prov" to "Proverbele"),
        21 to ("Ecl" to "Eclesiastul"),
        22 to ("Cânt" to "Cântarea Cântărilor"),
        23 to ("Isa" to "Isaia"),
        24 to ("Ier" to "Ieremia"),
        25 to ("Plâng" to "Plângerile"),
        26 to ("Ezec" to "Ezechiel"),
        27 to ("Dan" to "Daniel"),
        28 to ("Osea" to "Osea"),
        29 to ("Ioel" to "Ioel"),
        30 to ("Amos" to "Amos"),
        31 to ("Obad" to "Obadia"),
        32 to ("Iona" to "Iona"),
        33 to ("Mica" to "Mica"),
        34 to ("Naum" to "Naum"),
        35 to ("Hab" to "Habacuc"),
        36 to ("Țef" to "Țefania"),
        37 to ("Hag" to "Hagai"),
        38 to ("Zah" to "Zaharia"),
        39 to ("Mal" to "Maleahi"),
        40 to ("Mat" to "Matei"),
        41 to ("Mar" to "Marcu"),
        42 to ("Luc" to "Luca"),
        43 to ("Ioan" to "Ioan"),
        44 to ("Fapt" to "Faptele"),
        45 to ("Rom" to "Romani"),
        46 to ("1 Cor" to "1 Corinteni"),
        47 to ("2 Cor" to "2 Corinteni"),
        48 to ("Gal" to "Galateni"),
        49 to ("Efes" to "Efeseni"),
        50 to ("Filip" to "Filipeni"),
        51 to ("Col" to "Coloseni"),
        52 to ("1 Tes" to "1 Tesaloniceni"),
        53 to ("2 Tes" to "2 Tesaloniceni"),
        54 to ("1 Tim" to "1 Timotei"),
        55 to ("2 Tim" to "2 Timotei"),
        56 to ("Tit" to "Tit"),
        57 to ("Filem" to "Filimon"),
        58 to ("Evr" to "Evrei"),
        59 to ("Iac" to "Iacov"),
        60 to ("1 Pet" to "1 Petru"),
        61 to ("2 Pet" to "2 Petru"),
        62 to ("1 Ioan" to "1 Ioan"),
        63 to ("2 Ioan" to "2 Ioan"),
        64 to ("3 Ioan" to "3 Ioan"),
        65 to ("Iuda" to "Iuda"),
        66 to ("Rev" to "Revelația"),
    )

    fun booksFor(testament: Testament): List<Book> = BOOKS.filter { it.testament == testament }

    /** Returns books for a testament with names localized to [lang] ("E" or "M"). */
    fun booksFor(testament: Testament, lang: String): List<Book> {
        val books = BOOKS.filter { it.testament == testament }
        if (lang != "M") return books
        return books.map { book ->
            val ro = RO_NAMES[book.number] ?: return@map book
            book.copy(abbreviation = ro.first, title = ro.second)
        }
    }

    fun findByName(text: String): Book? {
        val normalized = text.lowercase()
        return BOOKS.firstOrNull {
            normalized.contains(it.title.lowercase()) ||
                normalized.contains(it.abbreviation.lowercase())
        }
    }
}
