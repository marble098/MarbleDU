package ir.marbledu.datausage.widget

import ir.marbledu.datausage.data.OperatorBrand
import ir.marbledu.datausage.data.OperatorCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetThemeTest {

    private val darkSurface = 0xFF111A2E.toInt()
    private val lightSurface = 0xFFF7F9FF.toInt()
    private val amoledSurface = 0xFF000000.toInt()

    @Test
    fun `dark surface follows the app theme mode`() {
        assertTrue(WidgetTheme.isDarkSurface("DARK", systemNight = false))
        assertTrue(WidgetTheme.isDarkSurface("AMOLED", systemNight = false))
        assertFalse(WidgetTheme.isDarkSurface("LIGHT", systemNight = true))
        assertTrue(WidgetTheme.isDarkSurface("SYSTEM", systemNight = true))
        assertFalse(WidgetTheme.isDarkSurface("SYSTEM", systemNight = false))
    }

    @Test
    fun `only amoled uses the true black surface`() {
        assertTrue(WidgetTheme.isAmoled("AMOLED"))
        assertFalse(WidgetTheme.isAmoled("DARK"))
        assertFalse(WidgetTheme.isAmoled("SYSTEM"))
    }

    @Test
    fun `dark palette keeps numbers readable on the dark surface`() {
        val colors = WidgetTheme.colors(dark = true, primary = 0xFF2F6FED.toInt())
        assertEquals(0xFFFFFFFF.toInt(), colors.hero)
        assertTrue(WidgetTheme.contrast(colors.hero, darkSurface) >= 4.5)
        assertTrue(WidgetTheme.contrast(colors.body, darkSurface) >= 4.5)
        assertTrue(WidgetTheme.contrast(colors.muted, darkSurface) >= 3.0)
        assertTrue(WidgetTheme.contrast(colors.mobile, darkSurface) >= 3.0)
        assertTrue(WidgetTheme.contrast(colors.wifi, darkSurface) >= 3.0)
    }

    @Test
    fun `light palette darkens the accent for readability`() {
        // رنگ زردِ روشن روی سطح سفید خوانا نیست و باید تیره شود.
        val yellow = 0xFFFFE600.toInt()
        val colors = WidgetTheme.colors(dark = false, primary = yellow, secondary = yellow)
        assertFalse(colors.dark)
        assertTrue(WidgetTheme.contrast(colors.accent, lightSurface) >= 3.0)
        assertTrue(WidgetTheme.contrast(colors.mobile, lightSurface) >= 3.0)
        assertTrue(WidgetTheme.contrast(colors.hero, lightSurface) >= 4.5)
    }

    @Test
    fun `amoled surface is pure black`() {
        val colors = WidgetTheme.colors(dark = true, amoled = true)
        assertTrue(WidgetTheme.contrast(colors.hero, amoledSurface) >= 4.5)
    }

    @Test
    fun `cap color changes with the state`() {
        val colors = WidgetTheme.colors(dark = true)
        val calm = WidgetTheme.capColor(CapState.CALM, colors)
        val warning = WidgetTheme.capColor(CapState.WARNING, colors)
        val critical = WidgetTheme.capColor(CapState.CRITICAL, colors)
        assertNotEquals(calm, warning)
        assertNotEquals(warning, critical)
        assertEquals(calm, colors.accent)
        assertTrue(WidgetTheme.contrast(warning, darkSurface) >= 3.0)
        assertTrue(WidgetTheme.contrast(critical, darkSurface) >= 3.0)
    }

    @Test
    fun `blend hits both endpoints`() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        assertEquals(black, WidgetTheme.blend(black, white, 0f))
        assertEquals(white, WidgetTheme.blend(black, white, 1f))
        assertEquals(0xFF808080.toInt(), WidgetTheme.blend(black, white, 0.5f))
        assertEquals(white, WidgetTheme.blend(black, white, 4f))
    }

    @Test
    fun `withAlpha keeps the rgb channels`() {
        assertEquals(0x33AB12CD, WidgetTheme.withAlpha(0xFFAB12CD.toInt(), 0x33))
    }

    @Test
    fun `readable keeps already contrasty colors untouched`() {
        val white = 0xFFFFFFFF.toInt()
        assertEquals(white, WidgetTheme.readable(white, darkSurface))
    }

    @Test
    fun `operator gradient keeps every widget color readable on every stop`() {
        val operators = OperatorCatalog.all + listOf(
            OperatorCatalog.byId(OperatorCatalog.ID_WIFI),
            OperatorCatalog.byId(OperatorCatalog.ID_MOBILE),
            OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN),
        )
        val palettes = operators.map { it.id to OperatorBrand.gradient(it) } + ("offline" to OperatorBrand.offline)
        palettes.forEach { (id, brand) ->
            val colors = WidgetTheme.brandColors(brand)
            val warning = WidgetTheme.capColor(CapState.WARNING, colors)
            val critical = WidgetTheme.capColor(CapState.CRITICAL, colors)
            brand.stops.forEach { stop ->
                assertTrue("$id: عدد اصلی", WidgetTheme.contrast(colors.hero, stop) >= 4.5)
                assertTrue("$id: متن", WidgetTheme.contrast(colors.body, stop) >= 4.5)
                assertTrue("$id: متن کم‌رنگ", WidgetTheme.contrast(colors.muted, stop) >= 3.0)
                assertTrue("$id: تأکید", WidgetTheme.contrast(colors.accent, stop) >= 3.0)
                assertTrue("$id: موبایل", WidgetTheme.contrast(colors.mobile, stop) >= 3.0)
                assertTrue("$id: وای‌فای", WidgetTheme.contrast(colors.wifi, stop) >= 3.0)
                assertTrue("$id: هشدار بسته", WidgetTheme.contrast(warning, stop) >= 3.0)
                assertTrue("$id: پایان بسته", WidgetTheme.contrast(critical, stop) >= 3.0)
            }
        }
    }

    @Test
    fun `light brands get dark ink on the widget`() {
        val irancell = WidgetTheme.brandColors(OperatorBrand.gradient(OperatorCatalog.byId("irancell")))
        assertFalse("روی زرد ایرانسل متن باید تیره باشد", irancell.dark)
        assertTrue(WidgetTheme.luminance(irancell.hero) < 0.1)
        val mci = WidgetTheme.brandColors(OperatorBrand.gradient(OperatorCatalog.byId("mci")))
        assertTrue(mci.dark)
        assertEquals(0xFFFFFFFF.toInt(), mci.hero)
    }
}
