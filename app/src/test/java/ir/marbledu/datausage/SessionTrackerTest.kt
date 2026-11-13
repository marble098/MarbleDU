package ir.marbledu.datausage.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SessionTrackerTest {
    private val mobile = SessionTracker.Route(UsageLedger.MOBILE, "mci", SimIdentity(10, 0, "Personal"))
    private val wifi = SessionTracker.Route(UsageLedger.WIFI, OperatorCatalog.ID_WIFI, networkName = "Home")
    private val start = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 13, 12, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test fun samples_and_transitions_do_not_assign_ambiguous_bytes() {
        val a = SessionTracker.advance(null, mobile, start, Traffic(100, 20), 1).open!!
        assertEquals(0L, a.totalBytes)
        val b = SessionTracker.advance(a, mobile, start + 1000, Traffic(200, 30), 2).open!!
        assertEquals(230L, b.totalBytes)
        val switch = SessionTracker.advance(b, wifi, start + 2000, Traffic(999, 999), 2)
        assertEquals(b.lastSampleMillis, switch.closed!!.endMillis)
        assertTrue(switch.closed.endEstimated)
        assertEquals(0L, switch.open!!.totalBytes)
        val connected = SessionTracker.advance(switch.open, wifi, start + 3000, Traffic(10, 5), 3).open!!
        val disconnected = SessionTracker.advance(connected, null, start + 4000, Traffic(20, 10), 3)
        assertNull(disconnected.open)
        assertEquals(15L, disconnected.closed!!.totalBytes)
    }

    @Test fun wifi_resolution_updates_identity_without_splitting_but_name_change_splits() {
        val a = SessionTracker.advance(null, wifi, start, Traffic(), 1).open!!
        val resolved = SessionTracker.advance(a, wifi.copy(operatorId = "tci"), start + 1000, Traffic(10, 2), 2).open!!
        assertEquals(1L, resolved.id)
        assertEquals("tci", resolved.operatorId)
        val changed = SessionTracker.advance(resolved, wifi.copy(networkName = "Office"), start + 2000, Traffic(10, 2), 2)
        assertNotNull(changed.closed)
        assertEquals(2L, changed.open!!.id)
    }

    @Test fun gaps_and_midnight_are_explicit() {
        val a = SessionTracker.advance(null, mobile, start, Traffic(), 1).open!!
        val b = SessionTracker.advance(a, mobile, start + 20_000, Traffic(12, 2), 2).open!!
        assertEquals(1, b.samplingGaps)
        val tomorrow = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val next = SessionTracker.advance(b, mobile, tomorrow, Traffic(100, 50), 2)
        assertNotNull(next.closed)
        assertEquals(0L, next.open!!.totalBytes)
    }

    @Test fun old_and_new_session_files_round_trip() {
        val legacy = "1\t1000\t2000\t1900\t100\t20\tmci\t-1\t-1\t-\ttrue\ttrue\t0\n"
        val old = UsageLedger.parseSessions(legacy).single()
        assertEquals(UsageLedger.MOBILE, old.transport)
        val new = old.copy(transport = UsageLedger.WIFI, networkName = "خانه Wi-Fi")
        assertEquals(listOf(old, new), UsageLedger.parseSessions(UsageLedger.serializeSessions(listOf(old, new))))
    }
}
