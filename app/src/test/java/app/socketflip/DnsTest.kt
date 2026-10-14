package app.socketflip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class DnsTest {
    private fun ips(vararg s: String) = s.map { InetAddress.getByName(it) }

    @Test fun keepsIpv4Servers() {
        assertEquals(ips("192.168.1.1", "8.8.8.8"), FlipVpnService.pickDns(ips("192.168.1.1", "8.8.8.8")))
    }

    @Test fun dropsIpv6AndLinkLocal() {
        val mixed = ips("fe80::1", "2001:4860:4860::8888", "169.254.1.1", "10.0.0.1")
        assertEquals(ips("10.0.0.1"), FlipVpnService.pickDns(mixed))
    }

    @Test fun fallsBackWhenNothingUsable() {
        val fallback = ips("1.1.1.1", "9.9.9.9")
        assertEquals(fallback, FlipVpnService.pickDns(emptyList()))
        assertEquals(fallback, FlipVpnService.pickDns(ips("fe80::1", "2001:db8::53")))
    }

    // Private DNS set to a name: no servers, so netd sends lookups over the real network (issue 1).
    @Test fun privateDnsNameGivesTheTunnelNoServers() {
        assertEquals(emptyList<InetAddress>(), FlipVpnService.tunnelDns(ips("192.168.1.1"), "dns.example"))
        assertEquals(emptyList<InetAddress>(), FlipVpnService.tunnelDns(emptyList(), "dns.example"))
    }

    @Test fun automaticOrOffCopiesTheNetworkServers() {
        assertEquals(ips("192.168.1.1"), FlipVpnService.tunnelDns(ips("192.168.1.1", "fe80::1"), null))
        assertEquals(ips("1.1.1.1", "9.9.9.9"), FlipVpnService.tunnelDns(emptyList(), null))
    }

    // Switching Private DNS to a name while the tunnel is up must rebuild it without servers.
    @Test fun switchingToAPrivateDnsNameIsAChange() {
        val before = FlipVpnService.tunnelDns(ips("192.168.1.1"), null)
        assertFalse(FlipVpnService.sameDns(before, FlipVpnService.tunnelDns(ips("192.168.1.1"), "dns.example")))
    }

    // A DHCP renew that only reorders the same resolvers must not rebuild the tunnel.
    @Test fun sameResolversInAnotherOrderAreTheSame() {
        assertTrue(FlipVpnService.sameDns(ips("192.168.1.1", "8.8.8.8"), ips("8.8.8.8", "192.168.1.1")))
        assertTrue(FlipVpnService.sameDns(ips("1.1.1.1"), ips("1.1.1.1")))
    }

    @Test fun differentResolversAreDifferent() {
        assertFalse(FlipVpnService.sameDns(ips("192.168.1.1"), ips("10.0.0.1")))
        assertFalse(FlipVpnService.sameDns(ips("192.168.1.1", "8.8.8.8"), ips("192.168.1.1")))
        assertFalse(FlipVpnService.sameDns(emptyList(), ips("1.1.1.1")))
    }
}
