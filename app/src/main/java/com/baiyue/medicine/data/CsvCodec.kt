package com.baiyue.medicine.data

/**
 * Small RFC-4180 style CSV codec used for user data backups.
 *
 * Supports quoted fields, escaped quotes, embedded commas and embedded newlines,
 * so multi-line medicine notes survive an export/import round trip.
 */
object CsvCodec {

    fun encodeRow(values: List<String>): String = values.joinToString(",") { escape(it) }

    fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    fun parse(text: String): List<List<String>> {
        val source = text.removePrefix("\uFEFF")
        if (source.isEmpty()) return emptyList()

        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun finishField() {
            row.add(field.toString())
            field.setLength(0)
        }

        fun finishRow() {
            finishField()
            rows.add(row)
            row = mutableListOf()
        }

        while (i < source.length) {
            val c = source[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < source.length && source[i + 1] == '"') {
                        field.append('"')
                        i += 2
                    } else {
                        inQuotes = false
                        i++
                    }
                } else {
                    field.append(c)
                    i++
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                        i++
                    }
                    ',' -> {
                        finishField()
                        i++
                    }
                    '\n' -> {
                        finishRow()
                        i++
                    }
                    '\r' -> {
                        finishRow()
                        i++
                        if (i < source.length && source[i] == '\n') i++
                    }
                    else -> {
                        field.append(c)
                        i++
                    }
                }
            }
        }

        // A final row without a trailing newline still needs to be emitted.
        if (field.isNotEmpty() || row.isNotEmpty()) finishRow()
        return rows
    }

    fun isBlankRow(row: List<String>): Boolean = row.all { it.isBlank() }
}
