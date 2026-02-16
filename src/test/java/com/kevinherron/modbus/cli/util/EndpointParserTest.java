package com.kevinherron.modbus.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kevinherron.modbus.cli.util.EndpointParser.Endpoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EndpointParserTest {

  @Nested
  class BareHostname {

    @Test
    void bareHostname_defaultPort() {
      Endpoint endpoint = EndpointParser.parse("localhost", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("localhost", tcp.hostname());
      assertEquals(502, tcp.port());
    }

    @Test
    void bareHostname_withPortOverride() {
      Endpoint endpoint = EndpointParser.parse("localhost", 1502);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("localhost", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void bareIpAddress() {
      Endpoint endpoint = EndpointParser.parse("192.168.1.100", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("192.168.1.100", tcp.hostname());
      assertEquals(502, tcp.port());
    }

    @Test
    void bareHostname_trimmed() {
      Endpoint endpoint = EndpointParser.parse("  localhost  ", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("localhost", tcp.hostname());
    }
  }

  @Nested
  class TcpColonScheme {

    @Test
    void tcpColonHostname() {
      Endpoint endpoint = EndpointParser.parse("tcp:myhost", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
      assertEquals(502, tcp.port());
    }

    @Test
    void tcpColonHostnamePort() {
      Endpoint endpoint = EndpointParser.parse("tcp:myhost:1502", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpColonHostname_withPortOverride() {
      Endpoint endpoint = EndpointParser.parse("tcp:myhost", 1502);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpColonHostnamePort_matchingOverride() {
      Endpoint endpoint = EndpointParser.parse("tcp:myhost:1502", 1502);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpColonIpv6() {
      Endpoint endpoint = EndpointParser.parse("tcp:[::1]", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("::1", tcp.hostname());
      assertEquals(502, tcp.port());
    }

    @Test
    void tcpColonIpv6WithPort() {
      Endpoint endpoint = EndpointParser.parse("tcp:[::1]:1502", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("::1", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpColonBlankHostname() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("tcp:", null));
    }

    @Test
    void tcpColonSlashPrefix() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("tcp:/foo", null));
    }

    @Test
    void tcpColon_caseInsensitive() {
      Endpoint endpoint = EndpointParser.parse("TCP:myhost", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
    }
  }

  @Nested
  class TcpUriScheme {

    @Test
    void tcpUriHostname() {
      Endpoint endpoint = EndpointParser.parse("tcp://myhost", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
      assertEquals(502, tcp.port());
    }

    @Test
    void tcpUriHostnamePort() {
      Endpoint endpoint = EndpointParser.parse("tcp://myhost:1502", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpUriIpv6() {
      Endpoint endpoint = EndpointParser.parse("tcp://[::1]", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("::1", tcp.hostname());
      assertEquals(502, tcp.port());
    }

    @Test
    void tcpUriIpv6WithPort() {
      Endpoint endpoint = EndpointParser.parse("tcp://[::1]:1502", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("::1", tcp.hostname());
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpUri_withPortOverride() {
      Endpoint endpoint = EndpointParser.parse("tcp://myhost", 1502);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpUri_matchingOverride() {
      Endpoint endpoint = EndpointParser.parse("tcp://myhost:1502", 1502);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals(1502, tcp.port());
    }

    @Test
    void tcpUri_noHostname() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("tcp://", null));
    }

    @Test
    void tcpUri_withPath() {
      assertThrows(
          IllegalArgumentException.class, () -> EndpointParser.parse("tcp://myhost/path", null));
    }

    @Test
    void tcpUri_caseInsensitive() {
      Endpoint endpoint = EndpointParser.parse("TCP://myhost", null);

      Endpoint.Tcp tcp = assertInstanceOf(Endpoint.Tcp.class, endpoint);
      assertEquals("myhost", tcp.hostname());
    }
  }

  @Nested
  class RtuColonScheme {

    @Test
    void rtuColonLinuxPort() {
      Endpoint endpoint = EndpointParser.parse("rtu:/dev/ttyUSB0", null);

      Endpoint.Rtu rtu = assertInstanceOf(Endpoint.Rtu.class, endpoint);
      assertEquals("/dev/ttyUSB0", rtu.serialPort());
    }

    @Test
    void rtuColonWindowsPort() {
      Endpoint endpoint = EndpointParser.parse("rtu:COM3", null);

      Endpoint.Rtu rtu = assertInstanceOf(Endpoint.Rtu.class, endpoint);
      assertEquals("COM3", rtu.serialPort());
    }

    @Test
    void rtuColonBlankPort() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("rtu:", null));
    }

    @Test
    void rtuColon_caseInsensitive() {
      Endpoint endpoint = EndpointParser.parse("RTU:/dev/ttyUSB0", null);

      Endpoint.Rtu rtu = assertInstanceOf(Endpoint.Rtu.class, endpoint);
      assertEquals("/dev/ttyUSB0", rtu.serialPort());
    }
  }

  @Nested
  class RtuUriScheme {

    @Test
    void rtuUriLinuxPort() {
      Endpoint endpoint = EndpointParser.parse("rtu:///dev/ttyUSB0", null);

      Endpoint.Rtu rtu = assertInstanceOf(Endpoint.Rtu.class, endpoint);
      assertEquals("/dev/ttyUSB0", rtu.serialPort());
    }

    @Test
    void rtuUri_blankPort() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("rtu://", null));
    }

    @Test
    void rtuUri_caseInsensitive() {
      Endpoint endpoint = EndpointParser.parse("RTU:///dev/ttyUSB0", null);

      Endpoint.Rtu rtu = assertInstanceOf(Endpoint.Rtu.class, endpoint);
      assertEquals("/dev/ttyUSB0", rtu.serialPort());
    }
  }

  @Nested
  class PortConflicts {

    @Test
    void tcpEndpointPort_conflictsWithOverride() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> EndpointParser.parse("tcp:myhost:502", 1502));
      assertTrue(ex.getMessage().contains("port mismatch"));
    }

    @Test
    void tcpUriPort_conflictsWithOverride() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> EndpointParser.parse("tcp://myhost:502", 1502));
      assertTrue(ex.getMessage().contains("port mismatch"));
    }

    @Test
    void rtuWithPortOverride() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> EndpointParser.parse("rtu:/dev/ttyUSB0", 502));
      assertTrue(ex.getMessage().contains("--port is only valid for TCP"));
    }

    @Test
    void rtuUriWithPortOverride() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> EndpointParser.parse("rtu:///dev/ttyUSB0", 502));
      assertTrue(ex.getMessage().contains("--port is only valid for TCP"));
    }
  }

  @Nested
  class InvalidEndpoints {

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t"})
    void blankEndpoint(String value) {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse(value, null));
    }

    @Test
    void nullEndpoint() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse(null, null));
    }

    @Test
    void unsupportedScheme() {
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> EndpointParser.parse("http://foo", null));
      assertTrue(ex.getMessage().contains("unsupported endpoint scheme"));
    }

    @Test
    void invalidIpv6_missingBracket() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("tcp:[::1", null));
    }

    @Test
    void ipv6_unexpectedCharsAfterBracket() {
      assertThrows(
          IllegalArgumentException.class, () -> EndpointParser.parse("tcp:[::1]abc", null));
    }

    @Test
    void ipv6_emptyBrackets() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("tcp:[]", null));
    }

    @Test
    void ipv6_portMissing() {
      assertThrows(IllegalArgumentException.class, () -> EndpointParser.parse("tcp:[::1]:", null));
    }

    @Test
    void ipv6_portNotNumeric() {
      assertThrows(
          IllegalArgumentException.class, () -> EndpointParser.parse("tcp:[::1]:abc", null));
    }
  }
}
