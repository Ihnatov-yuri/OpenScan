package com.openscan.scanner.data

/** A labelled piece of structured data detected in OCR text. */
data class DetectedItem(val type: DataType, val value: String)

enum class DataType(val label: String) {
    EMAIL("Emails"),
    PHONE("Phone numbers"),
    LINK("Links"),
    DATE("Dates"),
    AMOUNT("Amounts")
}

/**
 * Pulls structured data (emails, phones, links, dates, money amounts) out of
 * recognized text using simple, dependency-free regexes.
 */
object DataExtractor {

    private val EMAIL = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val LINK = Regex("""\b(?:https?://|www\.)[^\s]+""", RegexOption.IGNORE_CASE)
    // 7+ digit sequences allowing spaces, dashes, parens and a leading +
    private val PHONE = Regex("""(?<!\d)(\+?\d[\d\s().\-]{6,}\d)(?!\d)""")
    private val DATE = Regex(
        """\b(\d{1,2}[/.\-]\d{1,2}[/.\-]\d{2,4}|\d{4}[/.\-]\d{1,2}[/.\-]\d{1,2}|""" +
            """(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4})\b""",
        RegexOption.IGNORE_CASE
    )
    private val AMOUNT = Regex("""[$€£¥]\s?\d{1,3}(?:[,\s]?\d{3})*(?:\.\d{2})?""")

    fun extract(text: String): List<DetectedItem> {
        if (text.isBlank()) return emptyList()
        val items = LinkedHashSet<DetectedItem>()

        EMAIL.findAll(text).forEach { items += DetectedItem(DataType.EMAIL, it.value.trim()) }
        LINK.findAll(text).forEach { items += DetectedItem(DataType.LINK, it.value.trim().trimEnd('.', ',', ')')) }
        AMOUNT.findAll(text).forEach { items += DetectedItem(DataType.AMOUNT, it.value.trim()) }
        DATE.findAll(text).forEach { items += DetectedItem(DataType.DATE, it.value.trim()) }
        PHONE.findAll(text).forEach { m ->
            val digits = m.value.count { it.isDigit() }
            // Avoid matching long numeric strings that are really dates/ids.
            if (digits in 7..15) items += DetectedItem(DataType.PHONE, m.value.trim())
        }

        return items.toList()
    }

    /** Group detected items by type, preserving type order. */
    fun grouped(text: String): Map<DataType, List<String>> =
        extract(text)
            .groupBy({ it.type }, { it.value })
            .toSortedMap(compareBy { it.ordinal })
}
