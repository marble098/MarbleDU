package ir.marbledu.datausage

import ir.marbledu.datausage.data.OperatorCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OperatorCatalogTest {

    @Test
    fun detects_iranian_carriers_by_mcc_mnc() {
        assertEquals("همراه اول", OperatorCatalog.fromMccMnc("432", "11")?.fa)
        assertEquals("ایرانسل", OperatorCatalog.fromMccMnc("432", "35")?.fa)
        assertEquals("رایتل", OperatorCatalog.fromMccMnc("432", "20")?.fa)
    }

    @Test
    fun ignores_foreign_carriers() {
        assertNull(OperatorCatalog.fromMccMnc("310", "260"))
    }

    @Test
    fun normalizes_raw_names() {
        assertEquals("ایرانسل", OperatorCatalog.normalizeDisplay("Irancell"))
        assertEquals("همراه اول", OperatorCatalog.normalizeDisplay("MCI"))
        assertNotNull(OperatorCatalog.normalizeDisplay("Unknown Carrier"))
    }

    @Test
    fun detects_operators_from_isp_names() {
        assertEquals("irancell", OperatorCatalog.fromName("Iran Cell Service and Communication Company")?.id)
        assertEquals("mci", OperatorCatalog.fromName("Mobile Communication Company of Iran PLC")?.id)
        assertEquals("mci", OperatorCatalog.fromName("IR-MCI")?.id)
        assertEquals("tci", OperatorCatalog.fromName("Iran Telecommunication Company PJS")?.id)
        assertEquals("shatel", OperatorCatalog.fromName("Aria Shatel PJSC")?.id)
        assertEquals("shatel_mobile", OperatorCatalog.fromName("Shatel Mobile")?.id)
        assertEquals("asiatech", OperatorCatalog.fromName("Asiatech Data Transmission company")?.id)
        // کلیدواژه‌های کوتاه فقط به‌صورت کلمه‌ی کامل قبول می‌شوند.
        assertNull(OperatorCatalog.fromName("Netcity Broadband"))
        assertNull(OperatorCatalog.fromName(""))
    }

    @Test
    fun detects_wifi_operator_from_asn() {
        assertEquals("mci", OperatorCatalog.fromAsn(197207, "anything")?.id)
        assertEquals("irancell", OperatorCatalog.fromAsn(44244, null)?.id)
        assertEquals("rightel", OperatorCatalog.fromAsn(0, "Rightel Communication Service Company PJS")?.id)
        assertNull(OperatorCatalog.fromAsn(15169, "Google LLC"))
    }

    @Test
    fun numeric_and_id_round_trip() {
        assertEquals("irancell", OperatorCatalog.fromNumeric("43235")?.id)
        assertNull(OperatorCatalog.fromNumeric("310260"))
        val custom = OperatorCatalog.custom("Foo Net")
        assertEquals("Foo Net", OperatorCatalog.byId(custom.id).fa)
        assertEquals("mci", OperatorCatalog.byId("mci").id)
        assertEquals(OperatorCatalog.ID_WIFI, OperatorCatalog.byId(OperatorCatalog.ID_WIFI).id)
    }

    @Test
    fun normalizes_arabic_ye_kaf_and_zwnj_and_detects_expanded_isps() {
        // حروف عربی ي/ك و نیم‌فاصله در نام وای‌فای یا ISP
        assertEquals("irancell", OperatorCatalog.fromName("ايرانسل 5G")?.id)
        assertEquals("mobinnet", OperatorCatalog.fromName("مبين‌نت")?.id)
        assertEquals("shatel_mobile", OperatorCatalog.fromName("شاتل\u200Cموبایل")?.id)
        assertEquals("parsonline", OperatorCatalog.fromName("پارس آنلاین")?.id)
        assertEquals("fanava", OperatorCatalog.fromName("گروه فناوا")?.id)
        assertEquals("pishgaman", OperatorCatalog.fromName("Pishgaman Toseeh Ertebatat")?.id)
        assertEquals("samantel", OperatorCatalog.fromName("سامانتل")?.id)
        assertEquals("afranet", OperatorCatalog.fromAsn(25184, null)?.id)
    }

    @Test
    fun localized_special_uids() {
        assertEquals("سیستم اندروید", OperatorCatalog.specialUidLabel(1000, true))
        assertEquals("Android system", OperatorCatalog.specialUidLabel(1000, false))
        assertEquals(
            "هات‌اسپات و اشتراک‌گذاری",
            OperatorCatalog.specialUidLabel(android.app.usage.NetworkStats.Bucket.UID_TETHERING, true),
        )
        assertEquals(
            "Removed apps",
            OperatorCatalog.specialUidLabel(android.app.usage.NetworkStats.Bucket.UID_REMOVED, false),
        )
    }
}
