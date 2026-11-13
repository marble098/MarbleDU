package ir.marbledu.datausage

import ir.marbledu.datausage.data.Formats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatsTest {

    @Test
    fun bytes_uses_binary_units() {
        assertEquals("0 B", Formats.bytes(0, false))
        assertTrue(Formats.bytes(1024, false).startsWith("1"))
        assertTrue(Formats.bytes(1024L * 1024 * 1024, false).contains("GB"))
    }

    @Test
    fun persian_digits_are_converted() {
        assertEquals("۱۲۳", Formats.toPersianDigits("123"))
        assertEquals("1,024", Formats.integer(1024, false))
        assertEquals("۱,۰۲۴", Formats.integer(1024, true))
    }

    @Test
    fun speed_is_formatted() {
        assertTrue(Formats.speed(2048.0, false).contains("/s"))
        assertTrue(Formats.speed(2048.0, true).contains("بر ثانیه"))
    }

    @Test
    fun percent_is_clamped() {
        assertEquals(0.5f, Formats.percentOf(500, 1000), 0.001f)
        assertEquals(1f, Formats.percentOf(2000, 1000), 0.001f)
        assertEquals(0f, Formats.percentOf(200, 0), 0.001f)
    }
}
