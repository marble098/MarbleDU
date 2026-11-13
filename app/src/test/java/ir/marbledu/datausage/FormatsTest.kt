package ir.marbledu.datausage

import ir.marbledu.datausage.data.Formats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatsTest {

    @Test
    fun bytes_uses_binary_units() {
        assertEquals("0 B", Formats.bytes(0, false))
        assertEquals("۰ بایت", Formats.bytes(0, true))
        assertTrue(Formats.bytes(1024, false).startsWith("1"))
        assertTrue(Formats.bytes(1024L * 1024 * 1024, false).contains("GB"))
        assertTrue(Formats.bytes(1024L * 1024, true).contains("مگابایت"))
    }

    @Test
    fun persian_digits_are_converted_and_normalized() {
        assertEquals("۱۲۳", Formats.toPersianDigits("123"))
        assertEquals("123456", Formats.normalizeDigits("۱۲۳٤٥٦"))
        assertEquals("1,024", Formats.integer(1024, false))
        assertEquals("۱,۰۲۴", Formats.integer(1024, true))
    }

    @Test
    fun speed_and_compact_formats_are_localized() {
        assertTrue(Formats.speed(2048.0, false).contains("/s"))
        assertTrue(Formats.speed(2048.0, true).contains("بر ثانیه"))
        assertEquals("۰ ب/ث", Formats.compactSpeed(0.0, true))
        assertEquals("0 B/s", Formats.compactSpeed(0.0, false))
        assertTrue(Formats.compactBytes(2L * 1024 * 1024, true).contains("م.ب"))
        assertTrue(Formats.compactSpeed(512.0 * 1024, true).contains("ک.ب/ث"))
    }

    @Test
    fun percent_is_clamped() {
        assertEquals(0.5f, Formats.percentOf(500, 1000), 0.001f)
        assertEquals(1f, Formats.percentOf(2000, 1000), 0.001f)
        assertEquals(0f, Formats.percentOf(200, 0), 0.001f)
    }
}
