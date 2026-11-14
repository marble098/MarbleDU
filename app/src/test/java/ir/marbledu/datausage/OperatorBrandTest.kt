package ir.marbledu.datausage

import ir.marbledu.datausage.data.OperatorBrand
import ir.marbledu.datausage.data.OperatorCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OperatorBrandTest {
    @Test fun `major operator gradients keep their expected endpoints`() {
        val mci = OperatorBrand.gradient(OperatorCatalog.byId("mci"))
        val irancell = OperatorBrand.gradient(OperatorCatalog.byId("irancell"))

        assertEquals(0xFF006AAE.toInt(), mci.start)
        assertEquals(0xFFFFC107.toInt(), irancell.middle)
        assertNotEquals(mci.middle, irancell.middle)
    }

    @Test fun `catalog fallback always remains opaque`() {
        val fallback = OperatorBrand.gradient(OperatorCatalog.byId("afranet"))
        assertEquals(0xFF, fallback.start ushr 24)
        assertEquals(0xFF, fallback.middle ushr 24)
        assertEquals(0xFF, fallback.end ushr 24)
    }
}
