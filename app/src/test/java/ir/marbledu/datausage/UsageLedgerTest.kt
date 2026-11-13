package ir.marbledu.datausage

import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.OperatorTraffic
import ir.marbledu.datausage.data.Traffic
import ir.marbledu.datausage.data.UsageLedger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageLedgerTest {

    private fun List<OperatorTraffic>.sum(transport: String): Traffic =
        filter { it.transport == transport }.fold(Traffic()) { acc, row -> acc + row.traffic }

    @Test
    fun unrecorded_usage_goes_to_default_operator() {
        val rows = UsageLedger.reconcile(
            recorded = listOf(OperatorTraffic("tci", UsageLedger.WIFI, Traffic(300, 30))),
            systemMobile = Traffic(500, 50),
            systemWifi = Traffic(1000, 100),
            defaultMobileId = "mci",
        )
        assertEquals(3, rows.size)
        // مجموع هر اتصال دقیقاً همان عدد سیستم است.
        assertEquals(Traffic(500, 50), rows.sum(UsageLedger.MOBILE))
        assertEquals(Traffic(1000, 100), rows.sum(UsageLedger.WIFI))

        val unknownWifi = rows.first { it.operatorId == OperatorCatalog.ID_WIFI }
        assertEquals(Traffic(700, 70), unknownWifi.traffic)
        assertTrue(unknownWifi.estimated)

        val mci = rows.first { it.operatorId == "mci" }
        assertEquals(Traffic(500, 50), mci.traffic)
        assertFalse(mci.estimated)

        // بیشترین مصرف اول
        assertEquals(OperatorCatalog.ID_WIFI, rows.first().operatorId)
    }

    @Test
    fun overcounted_ledger_is_scaled_down_to_system_total() {
        val rows = UsageLedger.reconcile(
            recorded = listOf(OperatorTraffic("irancell", UsageLedger.WIFI, Traffic(2000, 200))),
            systemMobile = Traffic(),
            systemWifi = Traffic(1000, 100),
            defaultMobileId = "mci",
        )
        assertEquals(1, rows.size)
        assertEquals("irancell", rows[0].operatorId)
        assertEquals(Traffic(1000, 100), rows[0].traffic)
    }

    @Test
    fun unidentified_wifi_rows_merge_with_the_remainder() {
        val rows = UsageLedger.reconcile(
            recorded = listOf(
                OperatorTraffic(OperatorCatalog.ID_WIFI, UsageLedger.WIFI, Traffic(100, 10)),
                OperatorTraffic("tci", UsageLedger.WIFI, Traffic(200, 20)),
            ),
            systemMobile = Traffic(),
            systemWifi = Traffic(1000, 100),
            defaultMobileId = OperatorCatalog.ID_MOBILE,
        )
        assertEquals(2, rows.size)
        assertEquals(Traffic(800, 80), rows.first { it.operatorId == OperatorCatalog.ID_WIFI }.traffic)
        assertEquals(Traffic(200, 20), rows.first { it.operatorId == "tci" }.traffic)
    }

    @Test
    fun ledger_file_round_trips() {
        val map = mapOf(
            "20261113\twifi\ttci" to longArrayOf(10, 20),
            "20261113\tmobile\tx:Some Carrier" to longArrayOf(30, 40),
        )
        val parsed = UsageLedger.parse(UsageLedger.serialize(map) + "garbage line\n\n")
        assertEquals(2, parsed.size)
        assertEquals(20L, parsed.getValue("20261113\twifi\ttci")[1])
        assertEquals(30L, parsed.getValue("20261113\tmobile\tx:Some Carrier")[0])
    }

    @Test
    fun parses_asn_and_cleans_isp_names() {
        assertEquals(44244, IspResolver.parseAsn("AS44244 Iran Cell Service and Communication Company"))
        assertEquals(44244, IspResolver.parseAsn("44244"))
        assertEquals(0, IspResolver.parseAsn(""))
        assertEquals(0, IspResolver.parseAsn("Irancell"))
        assertEquals("Aria Shatel", IspResolver.cleanIspName("AS31549 Aria Shatel PJSC"))
    }
}
