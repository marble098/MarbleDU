package ir.marbledu.datausage

import ir.marbledu.datausage.data.ConnectionQuality
import ir.marbledu.datausage.data.ConnectionQuality.Grade
import ir.marbledu.datausage.data.PingEngine
import ir.marbledu.datausage.data.PingRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingQualityTest {

    private val target = PingEngine.targets.first()

    @Test
    fun summarize_computes_average_min_max_and_loss() {
        val r = PingEngine.summarize(target, PingRoute.DIRECT, listOf(100.0, 120.0, -1.0, 80.0))
        assertTrue(r.ok)
        assertEquals(4, r.sent)
        assertEquals(100.0, r.averageMs, 0.001)
        assertEquals(80.0, r.minMs, 0.001)
        assertEquals(120.0, r.maxMs, 0.001)
        assertEquals(25, r.lossPercent)
        // |120-100| و |80-120| → میانگین ۳۰
        assertEquals(30.0, r.jitterMs, 0.001)
    }

    @Test
    fun summarize_all_failed_is_not_ok() {
        val r = PingEngine.summarize(target, PingRoute.VPN, listOf(-1.0, -1.0, -1.0))
        assertFalse(r.ok)
        assertEquals(100, r.lossPercent)
        assertEquals(-1.0, r.averageMs, 0.0)
        assertNull(ConnectionQuality.evaluate(listOf(r)))
    }

    @Test
    fun jitter_of_single_sample_is_zero() {
        assertEquals(0.0, PingEngine.jitter(listOf(42.0)), 0.0)
        assertEquals(0.0, PingEngine.jitter(emptyList()), 0.0)
    }

    @Test
    fun targets_cover_domestic_and_global() {
        assertTrue(PingEngine.targets.any { it.scope == ir.marbledu.datausage.data.PingScope.DOMESTIC })
        assertTrue(PingEngine.targets.any { it.scope == ir.marbledu.datausage.data.PingScope.GLOBAL })
        assertEquals(PingEngine.targets.size, PingEngine.targets.map { it.id }.toSet().size)
    }

    @Test
    fun score_is_bounded_and_monotonic() {
        assertEquals(0, ConnectionQuality.score(-1.0, 0.0, 0))
        assertEquals(100, ConnectionQuality.score(20.0, 0.0, 0))
        val good = ConnectionQuality.score(60.0, 5.0, 0)
        val slow = ConnectionQuality.score(300.0, 5.0, 0)
        val lossy = ConnectionQuality.score(60.0, 5.0, 20)
        assertTrue(good > slow)
        assertTrue(good > lossy)
        assertTrue(ConnectionQuality.score(5000.0, 500.0, 100) in 0..100)
    }

    @Test
    fun verdict_grades_use_cases() {
        val fast = ConnectionQuality.evaluate(45.0, 4.0, 0)
        assertEquals(Grade.EXCELLENT, fast.gaming)
        assertEquals(Grade.EXCELLENT, fast.videoCall)
        assertEquals(Grade.EXCELLENT, fast.overall)

        val vpn = ConnectionQuality.evaluate(280.0, 35.0, 0)
        assertEquals(Grade.POOR, vpn.gaming)
        assertEquals(Grade.FAIR, vpn.videoCall)
        assertEquals(Grade.EXCELLENT, vpn.streaming)
        assertEquals(Grade.GOOD, vpn.browsing)

        val dead = ConnectionQuality.evaluate(-1.0, 0.0, 100)
        assertEquals(Grade.POOR, dead.overall)
        assertEquals(Grade.POOR, dead.browsing)
    }
}
