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
}
