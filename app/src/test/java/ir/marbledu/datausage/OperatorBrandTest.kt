package ir.marbledu.datausage

import ir.marbledu.datausage.data.OperatorBrand
import ir.marbledu.datausage.data.OperatorCatalog
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatorBrandTest {

    /** همه‌ی اپراتورهای فهرست، به‌علاوه‌ی حالت‌های عمومی و ISPهای ناشناخته. */
    private val everyOperator = OperatorCatalog.all + listOf(
        OperatorCatalog.byId(OperatorCatalog.ID_WIFI),
        OperatorCatalog.byId(OperatorCatalog.ID_MOBILE),
        OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN),
        OperatorCatalog.custom("Foo Net"),
        OperatorCatalog.custom("Abadan Net", iranianFixed = true),
    )

    @Test fun `major operators use their official brand colours`() {
        val mci = OperatorBrand.gradient(OperatorCatalog.byId("mci"))
        val irancell = OperatorBrand.gradient(OperatorCatalog.byId("irancell"))

        // همراه اول: آبی تیره‌ی هویت ۱۴۰۳ در میانه، با متن سفید
        assertEquals(0xFF295CAB.toInt(), mci.middle)
        assertFalse(mci.darkInk)
        // ایرانسل: زرد MTN با متن مشکی (نه نسخه‌ی قهوه‌ای‌شده‌ی زرد)
        assertEquals(0xFFFFCC00.toInt(), irancell.middle)
        assertTrue(irancell.darkInk)
        assertNotEquals(mci.middle, irancell.middle)
    }

    @Test fun `text stays readable on every stop and under the glow`() {
        everyOperator.forEach { op ->
            val g = OperatorBrand.gradient(op)
            g.stops.forEach { stop ->
                assertTrue(
                    "${op.id}: متن روی گرادیان خوانا نیست",
                    OperatorBrand.contrast(g.ink, stop) >= OperatorBrand.MIN_TEXT_CONTRAST,
                )
                val glowed = OperatorBrand.blend(stop, g.glow, g.glowAlpha)
                assertTrue(
                    "${op.id}: متن زیر درخشش خوانا نیست",
                    OperatorBrand.contrast(g.ink, glowed) >= OperatorBrand.MIN_TEXT_CONTRAST,
                )
            }
        }
    }

    @Test fun `every palette colour is opaque`() {
        (everyOperator.map { OperatorBrand.gradient(it) } + OperatorBrand.offline).forEach { g ->
            listOf(g.start, g.middle, g.end, g.glow, g.accent, g.ink).forEach { c ->
                assertEquals(0xFF, c ushr 24)
            }
            assertTrue(g.glowAlpha in 0f..1f)
        }
    }

    @Test fun `gradients are never flat`() {
        everyOperator.forEach { op ->
            val g = OperatorBrand.gradient(op)
            assertNotEquals("${op.id}: شروع و پایان یکی است", g.start, g.end)
            val spread = abs(OperatorBrand.luminance(g.start) - OperatorBrand.luminance(g.end))
            assertTrue("${op.id}: گرادیان تخت است", spread >= 0.015)
        }
    }

    @Test fun `light brands keep their colour and get dark text`() {
        listOf("irancell", "arintel", "lotustel", "datak", "mabna", "mobinnet").forEach { id ->
            assertTrue("$id باید متن تیره داشته باشد", OperatorBrand.gradient(OperatorCatalog.byId(id)).darkInk)
        }
        listOf("mci", "rightel", "shatel_mobile", "fanapmobile", "taliya", "tci", "telekish").forEach { id ->
            assertFalse("$id باید متن سفید داشته باشد", OperatorBrand.gradient(OperatorCatalog.byId(id)).darkInk)
        }
    }

    @Test fun `offline palette is neutral and readable`() {
        val g = OperatorBrand.offline
        assertFalse(g.darkInk)
        g.stops.forEach { assertTrue(OperatorBrand.contrast(g.ink, it) >= OperatorBrand.MIN_TEXT_CONTRAST) }
    }

    @Test fun `hsl conversion round-trips`() {
        listOf(0xFF295CAB, 0xFFFFCC00, 0xFF9B2776, 0xFF50E827, 0xFF1671B7, 0xFF000000, 0xFFFFFFFF, 0xFF808080)
            .map { it.toInt() }
            .forEach { c ->
                val (h, s, l) = OperatorBrand.toHsl(c)
                val back = OperatorBrand.fromHsl(h, s, l)
                listOf(16, 8, 0).forEach { shift ->
                    val delta = abs(((c shr shift) and 0xFF) - ((back shr shift) and 0xFF))
                    assertTrue("رنگ ${Integer.toHexString(c)} پس از HSL عوض شد", delta <= 1)
                }
            }
    }
}
