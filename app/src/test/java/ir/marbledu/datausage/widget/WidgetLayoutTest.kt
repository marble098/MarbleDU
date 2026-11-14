package ir.marbledu.datausage.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    private val base = WidgetSnapshot(
        persian = true,
        mobileBytes = 120L * 1024 * 1024,
        wifiBytes = 80L * 1024 * 1024,
        operatorName = "ایرانسل",
        capUsedBytes = 4L * 1024 * 1024 * 1024,
        capTotalBytes = 20L * 1024 * 1024 * 1024,
        monitoring = true,
        hasUsageAccess = true,
        updatedAtMillis = 1_700_000_000_000L,
    )

    @Test
    fun `small widget shows only header and hero`() {
        val v = WidgetLayout.visibility(base, widthDp = 200, heightDp = 120)
        assertTrue(v.compact)
        assertFalse(v.splitRow)
        assertFalse(v.capRow)
        assertFalse(v.actionsRow)
        assertFalse(v.detailRow)
        assertEquals(22f, v.heroTextSizeSp, 0.01f)
    }

    @Test
    fun `two row widget fits the mobile and wifi cards`() {
        val v = WidgetLayout.visibility(base, widthDp = 300, heightDp = WidgetLayout.SPLIT_HEIGHT_DP)
        assertFalse(v.compact)
        assertTrue(v.splitRow)
        assertFalse(v.capRow)
    }

    @Test
    fun `cap bar appears before the quick actions`() {
        val v = WidgetLayout.visibility(base, widthDp = 300, heightDp = WidgetLayout.CAP_HEIGHT_DP)
        assertTrue(v.splitRow)
        assertTrue(v.capRow)
        assertTrue(v.capPercent)
        assertFalse(v.actionsRow)
    }

    @Test
    fun `tall widget shows quick actions too`() {
        val v = WidgetLayout.visibility(base, widthDp = 320, heightDp = WidgetLayout.ACTIONS_HEIGHT_DP)
        assertTrue(v.capRow)
        assertTrue(v.actionsRow)
        assertFalse(v.detailRow)
    }

    @Test
    fun `download and upload row only in detailed mode and tall widget`() {
        val detailed = base.copy(detailed = true)
        assertFalse(WidgetLayout.visibility(detailed, 320, WidgetLayout.ACTIONS_HEIGHT_DP).detailRow)
        assertTrue(WidgetLayout.visibility(detailed, 320, WidgetLayout.DETAIL_HEIGHT_DP).detailRow)
        assertFalse(
            WidgetLayout.visibility(base, 320, WidgetLayout.DETAIL_HEIGHT_DP).detailRow,
        )
    }

    @Test
    fun `cap row respects the setting and the cap value`() {
        assertFalse(
            WidgetLayout.visibility(base.copy(showCap = false), 320, 240).capRow,
        )
        assertFalse(
            WidgetLayout.visibility(base.copy(capTotalBytes = 0L), 320, 240).capRow,
        )
        assertFalse(
            WidgetLayout.visibility(base.copy(capTotalBytes = 0L), 320, 240).capPercent,
        )
    }

    @Test
    fun `quick actions respect the setting`() {
        assertFalse(
            WidgetLayout.visibility(base.copy(quickActions = false), 320, 260).actionsRow,
        )
    }

    @Test
    fun `narrow widget drops the split cards`() {
        val v = WidgetLayout.visibility(base, widthDp = 120, heightDp = 260)
        assertFalse(v.splitRow)
        assertFalse(v.actionsRow)
        assertFalse(v.capRow)
    }

    @Test
    fun `operator row needs a name and the setting`() {
        assertTrue(WidgetLayout.visibility(base, 300, 200).operator)
        assertFalse(WidgetLayout.visibility(base.copy(showOperator = false), 300, 200).operator)
        assertFalse(WidgetLayout.visibility(base.copy(operatorName = ""), 300, 200).operator)
    }

    @Test
    fun `status chip is always visible when monitoring is off`() {
        assertTrue(WidgetLayout.visibility(base.copy(monitoring = false), 120, 120).statusChip)
        assertFalse(WidgetLayout.visibility(base, 120, 120).statusChip)
        assertTrue(WidgetLayout.visibility(base, 260, 120).statusChip)
    }

    @Test
    fun `big text setting grows the hero number`() {
        val normal = WidgetLayout.visibility(base, 300, 200).heroTextSizeSp
        val big = WidgetLayout.visibility(base.copy(bigText = true), 300, 200).heroTextSizeSp
        assertTrue(big > normal)
    }
}
