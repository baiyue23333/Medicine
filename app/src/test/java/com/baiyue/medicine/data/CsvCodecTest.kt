package com.baiyue.medicine.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {

    @Test
    fun `parse simple rows`() {
        assertEquals(
            listOf(listOf("a", "b"), listOf("c", "d")),
            CsvCodec.parse("a,b\nc,d\n")
        )
    }

    @Test
    fun `quoted fields survive commas quotes and newlines`() {
        val text = "id,note\n1,\"hello, \"\"world\"\"\nsecond line\"\n"
        assertEquals(
            listOf(
                listOf("id", "note"),
                listOf("1", "hello, \"world\"\nsecond line")
            ),
            CsvCodec.parse(text)
        )
    }

    @Test
    fun `crlf and utf8 bom are handled`() {
        val rows = CsvCodec.parse("\uFEFFa,b\r\nc,d\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), rows)
    }

    @Test
    fun `encode and parse round trip`() {
        val row = listOf("1", "药,名", "line1\nline2", "say \"hi\"")
        val parsed = CsvCodec.parse(CsvCodec.encodeRow(row))
        assertEquals(listOf(row), parsed)
    }

    @Test
    fun `blank rows are detectable`() {
        val rows = CsvCodec.parse("a,b\n\nc,d")
        assertEquals(3, rows.size)
        assertTrue(CsvCodec.isBlankRow(rows[1]))
    }
}
