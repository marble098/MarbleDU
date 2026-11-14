package ir.marbledu.datausage

import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.ui.OperatorLogos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * پوشش «اینترنت ثابت» در فهرست اپراتورها.
 *
 * هدف این تست‌ها سه چیز است:
 *  ۱) ارائه‌دهنده‌های ثابت تازه (ایران‌تلکام، آرین‌نت، آسیانت، آفتاب‌نت، داتیس‌نت،
 *     ایران‌سرور، ابرآروان، پارس‌پک، هاست‌ایران، میهن‌وب‌هاست، میزبان‌فا و…) با
 *     نام انگلیسی، نام فارسی و دامنه‌ی DHCP شناسایی شوند،
 *  ۲) نام کامل هر اپراتور (نه فقط کلیدواژه) هم به تشخیص کمک کند،
 *  ۳) ISP ناشناخته‌ای که با اطمینان ایرانی است، به‌جای «نامشخص» با نام واقعی و
 *     نشان «ثابت داخلی» ذخیره و بعد از راه‌اندازی مجدد هم بازیابی شود.
 */
class FixedIspDetectionTest {

    @Test
    fun detects_new_fixed_line_providers_by_english_name() {
        val cases = mapOf(
            "Iran Telekom PJSC" to "irantelekom",
            "Aryan Net Co." to "aryanet",
            "Asia Net" to "asianet",
            "Aftab Net" to "aftabnet",
            "Datis Net" to "datisnet",
            "Iran Server" to "iranserver",
            "ArvanCloud" to "arvancloud",
            "ParsPack" to "parspack",
            "HostIran Network" to "hostiran",
            "Mihan Web Host" to "mihanwebhost",
            "MizbanFa" to "mizbanfa",
        )
        cases.forEach { (raw, expected) ->
            assertEquals("«$raw» باید $expected تشخیص داده شود", expected, OperatorCatalog.fromName(raw)?.id)
        }
    }

    @Test
    fun detects_new_fixed_line_providers_by_persian_name() {
        val cases = mapOf(
            "ایران تلکام" to "irantelekom",
            "آرین نت" to "aryanet",
            "آسیانت" to "asianet",
            "آفتاب نت" to "aftabnet",
            "داتیس نت" to "datisnet",
            "ایران سرور" to "iranserver",
            "ابر آروان" to "arvancloud",
            "پارس پک" to "parspack",
            "هاست ایران" to "hostiran",
            "میزبان فا" to "mizbanfa",
        )
        cases.forEach { (raw, expected) ->
            assertEquals("«$raw» باید $expected تشخیص داده شود", expected, OperatorCatalog.fromName(raw)?.id)
        }
    }

    @Test
    fun detects_fixed_providers_from_dhcp_domains_and_ssids() {
        assertEquals("shatel", OperatorCatalog.fromName("shatel.ir")?.id)
        assertEquals("asiatech", OperatorCatalog.fromName("asiatech.ir")?.id)
        assertEquals("parsonline", OperatorCatalog.fromName("parsonline.net")?.id)
        assertEquals("irantelekom", OperatorCatalog.fromName("irantelekom.ir")?.id)
        assertEquals("arvancloud", OperatorCatalog.fromName("arvancloud.ir")?.id)
        assertEquals("tci", OperatorCatalog.fromName("شرکت مخابرات ایران")?.id)
    }

    @Test
    fun full_operator_names_are_enough_without_keywords() {
        // نام کامل فارسی/انگلیسی خودِ اپراتور هم نشانه‌ی تشخیص است.
        assertEquals("tci", OperatorCatalog.fromName("Iran Telecommunication Company PJS")?.id)
        assertEquals("parsonline", OperatorCatalog.fromName("Pars Online")?.id)
        assertEquals("fanapmobile", OperatorCatalog.fromName("Fanap Mobile")?.id)
        // و هنوز نباید با متن‌های بی‌ربط اشتباه شود.
        assertNull(OperatorCatalog.fromName("Netcity Broadband"))
        assertNull(OperatorCatalog.fromName("Google LLC"))
    }

    @Test
    fun unknown_iranian_isp_keeps_its_real_name_and_stays_iranian() {
        val custom = OperatorCatalog.custom("Abadan Net", iranianFixed = true)
        assertTrue("شناسه‌ی ISP ایرانی باید با f: شروع شود", custom.id.startsWith("f:"))
        assertTrue(custom.isIranian)
        assertEquals("Abadan Net", custom.fa)
        val restored = OperatorCatalog.byId(custom.id)
        assertEquals("Abadan Net", restored.fa)
        assertTrue("بعد از بازیابی هم باید ایرانی بماند", restored.isIranian)
        assertFalse("ISP ناشناخته‌ی ایرانی موبایل نیست", restored.isMobile)
    }

    @Test
    fun unknown_foreign_isp_stays_foreign() {
        val custom = OperatorCatalog.custom("Comcast Cable")
        assertFalse(custom.isIranian)
        assertTrue(custom.id.startsWith("x:"))
        val restored = OperatorCatalog.byId(custom.id)
        assertEquals("Comcast Cable", restored.fa)
        assertFalse(restored.isIranian)
    }

    @Test
    fun recognises_iranian_names_country_and_generic_placeholders() {
        assertTrue(OperatorCatalog.looksIranian("Iran Telecommunication Company"))
        assertTrue(OperatorCatalog.looksIranian("Some ISP .ir"))
        assertTrue(OperatorCatalog.looksIranian("Asre Ertebat Kish"))
        assertTrue(OperatorCatalog.looksIranian("شرکت داده‌پردازی تهران"))
        assertFalse(OperatorCatalog.looksIranian("Google LLC"))
        assertFalse(OperatorCatalog.looksIranian(""))
        assertFalse(OperatorCatalog.looksIranian(null))
    }

    @Test
    fun cleans_organisation_suffixes_and_generic_names() {
        assertEquals("Aria Shatel", IspResolver.cleanIspName("AS31549 Aria Shatel PJSC"))
        assertEquals("Iran Telekom", IspResolver.cleanIspName("Iran Telekom (PJSC)"))
        assertEquals("Fanava", IspResolver.cleanIspName("Fanava Group"))
        assertEquals("مخابرات", IspResolver.cleanIspName("شرکت مخابرات"))
        assertEquals("", IspResolver.cleanIspName("Internet Service Provider"))
    }

    @Test
    fun catalog_is_consistent() {
        val ids = OperatorCatalog.all.map { it.id }
        assertEquals("شناسه‌ها باید یکتا باشند", ids.size, ids.distinct().size)
        OperatorCatalog.all.forEach { op ->
            assertTrue("id خالی", op.id.isNotBlank())
            assertTrue("نام فارسی خالی (${op.id})", op.fa.isNotBlank())
            assertTrue("نام انگلیسی خالی (${op.id})", op.en.isNotBlank())
            assertTrue("حروف نشان خالی (${op.id})", op.mark.isNotBlank())
            assertEquals(
                "رنگ برند باید مات باشد (${op.id})",
                0xFF,
                ((op.brandColor ushr 24) and 0xFF).toInt(),
            )
            // شناسه‌های سیستمی نباید با شناسه‌ی اپراتورها قاطی شوند.
            assertFalse(op.id in setOf(OperatorCatalog.ID_WIFI, OperatorCatalog.ID_MOBILE, OperatorCatalog.ID_UNKNOWN))
        }
        assertTrue("فهرست ثابت باید دست‌کم ۲۵ ارائه‌دهنده داشته باشد", OperatorCatalog.fixed.size >= 25)
    }

    @Test
    fun every_logo_belongs_to_a_known_operator() {
        val ids = OperatorCatalog.all.map { it.id }.toSet()
        OperatorLogos.idsWithLogo().forEach { id ->
            assertTrue("لوگوی «$id» به هیچ اپراتوری در فهرست وصل نیست", id in ids)
        }
        assertNotNull("لوگوی رایتل باید موجود باشد", OperatorLogos.logoFor("rightel"))
    }
}
