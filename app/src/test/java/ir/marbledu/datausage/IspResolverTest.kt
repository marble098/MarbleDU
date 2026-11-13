package ir.marbledu.datausage

import ir.marbledu.datausage.data.IspResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IspResolverTest {

    @Test
    fun classifies_router_ethernet_and_hotspots() {
        assertEquals(
            IspResolver.WifiSourceKind.ETHERNET,
            IspResolver.classifySourceKind(isEthernet = true, isMetered = false, gatewayIp = "192.168.1.1"),
        )
        assertEquals(
            IspResolver.WifiSourceKind.HOTSPOT_IPHONE,
            IspResolver.classifySourceKind(isEthernet = false, isMetered = true, gatewayIp = "172.20.10.1"),
        )
        assertEquals(
            IspResolver.WifiSourceKind.HOTSPOT_ANDROID,
            IspResolver.classifySourceKind(isEthernet = false, isMetered = true, gatewayIp = "192.168.43.1"),
        )
        assertEquals(
            IspResolver.WifiSourceKind.HOTSPOT_WINDOWS,
            IspResolver.classifySourceKind(isEthernet = false, isMetered = false, gatewayIp = "192.168.137.1"),
        )
        assertEquals(
            IspResolver.WifiSourceKind.HOTSPOT_METERED,
            IspResolver.classifySourceKind(isEthernet = false, isMetered = true, gatewayIp = "192.168.199.1"),
        )
        assertEquals(
            IspResolver.WifiSourceKind.MODEM_ROUTER,
            IspResolver.classifySourceKind(isEthernet = false, isMetered = false, gatewayIp = "192.168.1.1"),
        )
    }

    @Test
    fun cleans_wifi_ssids() {
        assertEquals("Shatel-Home", IspResolver.cleanSsid("\"Shatel-Home\""))
        assertEquals("", IspResolver.cleanSsid("<unknown ssid>"))
        assertEquals("", IspResolver.cleanSsid("0x"))
        assertEquals("", IspResolver.cleanSsid(null))
    }

    @Test
    fun matches_operator_from_dhcp_domain_and_ssid() {
        val fromDomain = IspResolver.matchOperatorFromClues(
            IspResolver.NetworkClues(
                ssid = "Home_5G",
                dhcpDomain = "broadband.shatel.ir",
            ),
        )
        assertNotNull(fromDomain)
        assertEquals("shatel", fromDomain?.first?.id)
        assertEquals(IspResolver.DetectionMethod.DHCP_DOMAIN, fromDomain?.second)

        val fromSsid = IspResolver.matchOperatorFromClues(
            IspResolver.NetworkClues(
                ssid = "ايرانسل من",
                dhcpDomain = "lan",
            ),
        )
        assertNotNull(fromSsid)
        assertEquals("irancell", fromSsid?.first?.id)
        assertEquals(IspResolver.DetectionMethod.WIFI_SSID, fromSsid?.second)

        assertNull(
            IspResolver.matchOperatorFromClues(
                IspResolver.NetworkClues(ssid = "MyLivingRoom", dhcpDomain = "home.arpa"),
            ),
        )
    }

    @Test
    fun filters_private_cgnat_and_public_dns_resolvers() {
        // DNS عمومی و تحریم‌شکن‌ها نباید به اشتباه به‌عنوان ISP بررسی شوند.
        assertFalse(IspResolver.isInspectablePublicIp("8.8.8.8"))
        assertFalse(IspResolver.isInspectablePublicIp("1.1.1.1"))
        assertFalse(IspResolver.isInspectablePublicIp("178.22.122.100")) // شکن
        assertFalse(IspResolver.isInspectablePublicIp("10.202.10.202"))  // ۴۰۳
        assertFalse(IspResolver.isInspectablePublicIp("78.157.42.100"))  // الکترو
        // آدرس‌های خصوصی و CGNAT
        assertFalse(IspResolver.isInspectablePublicIp("192.168.1.1"))
        assertFalse(IspResolver.isInspectablePublicIp("10.0.0.1"))
        assertFalse(IspResolver.isInspectablePublicIp("172.20.10.1"))
        assertFalse(IspResolver.isInspectablePublicIp("100.64.0.5"))
        assertFalse(IspResolver.isInspectablePublicIp("fe80::1"))
        assertFalse(IspResolver.isInspectablePublicIp("fc00::1"))
        // IP عمومی معتبر (ایرانسل / مخابرات / IPv6 عمومی)
        assertTrue(IspResolver.isInspectablePublicIp("5.112.10.2"))
        assertTrue(IspResolver.isInspectablePublicIp("2.180.10.5"))
        assertTrue(IspResolver.isInspectablePublicIp("2a01:5ec0::1"))
    }

    @Test
    fun parses_plain_ip_and_cloudflare_trace() {
        assertEquals("5.112.42.10", IspResolver.parsePlainIp("5.112.42.10\n"))
        val trace = """
            fl=21f12
            h=1.1.1.1
            ip=2.180.15.99
            ts=1700000000.123
            loc=IR
        """.trimIndent()
        assertEquals("2.180.15.99", IspResolver.parsePlainIp(trace))
        assertNull(IspResolver.parsePlainIp("<html><body>403 Forbidden</body></html>"))
        assertNull(IspResolver.parsePlainIp("999.999.1.1"))
    }
}
