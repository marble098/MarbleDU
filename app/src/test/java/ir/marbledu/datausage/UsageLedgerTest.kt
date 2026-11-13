package ir.marbledu.datausage

import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.OperatorTraffic
import ir.marbledu.datausage.data.SimIdentity
import ir.marbledu.datausage.data.Traffic
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageLedgerTest {

    private fun List<OperatorTraffic>.sum(transport: String): Traffic =
        filter { it.transport == transport }.fold(Traffic()) { acc, row -> acc + row.traffic }

    @Test
    fun unobserved_mobile_usage_is_unassigned_not_guessed_as_default_sim() {
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

        val unknownMobile = rows.first { it.operatorId == OperatorCatalog.ID_UNKNOWN }
        assertEquals(Traffic(500, 50), unknownMobile.traffic)
        assertTrue(unknownMobile.estimated)
        assertFalse(rows.any { it.operatorId == "mci" })

        // بیشترین مصرف اول
        assertEquals(OperatorCatalog.ID_WIFI, rows.first().operatorId)
    }

    @Test
    fun identical_carriers_on_two_sim_slots_remain_separate() {
        val simOne = SimIdentity(subscriptionId = 10, slotIndex = 0, displayName = "Personal")
        val simTwo = SimIdentity(subscriptionId = 11, slotIndex = 1, displayName = "Work")
        val rows = UsageLedger.reconcile(
            recorded = listOf(
                OperatorTraffic("mci", UsageLedger.MOBILE, Traffic(30, 3), sim = simOne),
                OperatorTraffic("mci", UsageLedger.MOBILE, Traffic(70, 7), sim = simTwo),
            ),
            systemMobile = Traffic(100, 10),
            systemWifi = Traffic(),
            defaultMobileId = "mci",
        )
        assertEquals(2, rows.size)
        assertEquals(Traffic(30, 3), rows.single { it.sim == simOne }.traffic)
        assertEquals(Traffic(70, 7), rows.single { it.sim == simTwo }.traffic)
    }

    @Test
    fun overcounted_samples_are_preserved_for_auditing() {
        val rows = UsageLedger.reconcile(
            recorded = listOf(OperatorTraffic("irancell", UsageLedger.WIFI, Traffic(2000, 200))),
            systemMobile = Traffic(),
            systemWifi = Traffic(1000, 100),
            defaultMobileId = "mci",
        )
        assertEquals(1, rows.size)
        assertEquals("irancell", rows[0].operatorId)
        assertEquals(Traffic(2000, 200), rows[0].traffic)
        assertFalse(rows[0].estimated)
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
    fun new_hourly_ledger_rows_preserve_sim_identity_on_round_trip() {
        val key = "20261113\tmobile\tmci\t18\t1\tV29yay0x\t14"
        val source = "$key\t50\t60"
        val parsed = UsageLedger.parse(source)
        assertEquals(longArrayOf(50, 60).toList(), parsed.getValue(key).toList())
        assertEquals(source, UsageLedger.serialize(parsed).trim())
    }

    @Test
    fun session_file_round_trips_open_and_closed_sessions() {
        val sessions = listOf(
            UsageSession(
                id = 9,
                startMillis = 1_800_000_000_000L,
                endMillis = null,
                lastSampleMillis = 1_800_000_001_000L,
                rxBytes = 1_024,
                txBytes = 256,
                operatorId = "mci",
                sim = SimIdentity(subscriptionId = 10, slotIndex = 0, displayName = "Personal SIM"),
                startEstimated = true,
                samplingGaps = 2,
            ),
            UsageSession(
                id = 10,
                startMillis = 1_800_000_100_000L,
                endMillis = 1_800_000_120_000L,
                lastSampleMillis = 1_800_000_119_000L,
                rxBytes = 4_096,
                txBytes = 512,
                operatorId = "irancell",
                endEstimated = true,
            ),
        )
        assertEquals(sessions, UsageLedger.parseSessions(UsageLedger.serializeSessions(sessions)))
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
