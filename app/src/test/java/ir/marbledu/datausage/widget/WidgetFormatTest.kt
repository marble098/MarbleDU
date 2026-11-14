package ir.marbledu.datausage.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFormatTest {

    @Test
    fun `amount splits number and unit in english`() {
        val amount = WidgetFormat.amount(3L * 1024 * 1024 * 1024, persian = false)
        assertEquals("3.0", amount.value)
        assertEquals("GB", amount.unit)
        assertFalse(amount.isEmpty)
    }

    @Test
    fun `amount keeps persian unit separate`() {
        val amount = WidgetFormat.amount(512L * 1024 * 1024, persian = true)
        assertEquals("۵۱۲", amount.value)
        assertEquals("مگابایت", amount.unit)
    }

    @Test
    fun `amount shows placeholder without usage access`() {
        val amount = WidgetFormat.amount(1024L, persian = false, hasData = false)
        assertEquals(WidgetFormat.PLACEHOLDER, amount.value)
        assertEquals("", amount.unit)
    }

    @Test
    fun `percent is clamped between zero and hundred`() {
        assertEquals(0, WidgetFormat.percent(0, 1000))
        assertEquals(50, WidgetFormat.percent(500, 1000))
        assertEquals(100, WidgetFormat.percent(5000, 1000))
        assertEquals(0, WidgetFormat.percent(500, 0))
        assertEquals(0, WidgetFormat.percent(-10, 1000))
    }

    @Test
    fun `percent text follows the language`() {
        assertEquals("۴۲٪", WidgetFormat.percentText(42, persian = true))
        assertEquals("42%", WidgetFormat.percentText(42, persian = false))
        assertEquals("100%", WidgetFormat.percentText(140, persian = false))
    }

    @Test
    fun `cap state warns near the end and critical at the end`() {
        assertEquals(CapState.CALM, WidgetFormat.capState(0))
        assertEquals(CapState.CALM, WidgetFormat.capState(79))
        assertEquals(CapState.WARNING, WidgetFormat.capState(80))
        assertEquals(CapState.WARNING, WidgetFormat.capState(99))
        assertEquals(CapState.CRITICAL, WidgetFormat.capState(100))
    }

    @Test
    fun `remaining never goes negative`() {
        assertEquals(400L, WidgetFormat.remaining(600L, 1000L))
        assertEquals(0L, WidgetFormat.remaining(1400L, 1000L))
        assertEquals(0L, WidgetFormat.remaining(0L, 0L))
    }

    @Test
    fun `clock text is empty for unknown time and persian digits otherwise`() {
        assertEquals("", WidgetFormat.clockText(0L, persian = true))
        val persianClock = WidgetFormat.clockText(1_700_000_000_000L, persian = true)
        assertTrue(persianClock.isNotEmpty())
        assertTrue(persianClock.all { it in '۰'..'۹' || it == ':' })
        val englishClock = WidgetFormat.clockText(1_700_000_000_000L, persian = false)
        assertTrue(englishClock.all { it in '0'..'9' || it == ':' })
    }
}
