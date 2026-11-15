package ir.marbledu.datausage.service

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundPromotionTest {
    @Test
    fun `successful promotion continues monitoring`() {
        var called = false
        assertTrue(tryForegroundPromotion({ called = true }, { false }))
        assertTrue(called)
    }

    @Test
    fun `platform rejection stops startup without crashing`() {
        val rejection = IllegalStateException("start not allowed")
        assertFalse(tryForegroundPromotion({ throw rejection }, { it === rejection }))
    }

    @Test
    fun `unrelated failures are not swallowed`() {
        val failure = IllegalStateException("unexpected")
        try {
            tryForegroundPromotion({ throw failure }, { false })
            throw AssertionError("Expected the original failure")
        } catch (caught: IllegalStateException) {
            assertSame(failure, caught)
        }
    }

    @Test
    fun `monitor manifest declares continuous special use rather than timed data sync`() {
        val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml"))
            .first { it.isFile }
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val android = "http://schemas.android.com/apk/res/android"
        val services = doc.getElementsByTagName("service")
        val monitor = (0 until services.length).map { services.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(android, "name") == ".service.MonitorService" }
        assertEquals("specialUse", monitor.getAttributeNS(android, "foregroundServiceType"))
        val properties = monitor.getElementsByTagName("property")
        val subtype = (0 until properties.length).map { properties.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(android, "name") == "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" }
        assertTrue(subtype.getAttributeNS(android, "value").isNotBlank())
        val permissions = doc.getElementsByTagName("uses-permission")
        val names = (0 until permissions.length).map {
            (permissions.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name")
        }
        assertTrue("android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in names)
        assertFalse("android.permission.FOREGROUND_SERVICE_DATA_SYNC" in names)
    }
}
