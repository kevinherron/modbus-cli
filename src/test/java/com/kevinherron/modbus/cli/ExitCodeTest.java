package com.kevinherron.modbus.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalpetri.modbus.exceptions.ModbusCrcException;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.fazecast.jSerialComm.SerialPortIOException;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import com.kevinherron.modbus.cli.test.CliTestRunner;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ExitCodeTest {

  @Test
  void missingClientSubcommandReturnsUsageExitCode() {
    var result = CliTestRunner.execute("client", "localhost");

    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("Missing required subcommand"));
  }

  @Test
  void connectionFailureReturnsConnectionExitCode() {
    var result =
        CliTestRunner.execute("client", "127.0.0.1", "-p", "1", "read-holding-registers", "0", "1");

    assertEquals(3, result.exitCode());
    assertFalse(result.stderr().isBlank());
    assertTrue(
        result.stderr().toLowerCase(Locale.ROOT).contains("connect")
            || result.stderr().toLowerCase(Locale.ROOT).contains("refused"));
  }

  @Test
  void serialPortInvalidPortReturnsConnectionExitCode() {
    assertEquals(3, Modbus.mapExitCode(new SerialPortInvalidPortException("fake", null)));
  }

  @Test
  void serialPortIOExceptionReturnsConnectionExitCode() {
    assertEquals(3, Modbus.mapExitCode(new SerialPortIOException("fake")));
  }

  @Test
  void serialPortTimeoutReturnsTimeoutExitCode() {
    assertEquals(5, Modbus.mapExitCode(new SerialPortTimeoutException("fake")));
  }

  @Test
  void genericModbusExceptionReturnsFallbackFailureExitCode() {
    assertEquals(1, Modbus.mapExitCode(new ModbusException("fake")));
  }

  @Test
  void modbusResponseExceptionReturnsProtocolFailureExitCode() {
    assertEquals(4, Modbus.mapExitCode(new ModbusResponseException(3, 2)));
  }

  @Test
  void modbusCrcExceptionReturnsProtocolFailureExitCode() {
    assertEquals(4, Modbus.mapExitCode(new ModbusCrcException(null)));
  }

  @Test
  void wrappedTimeoutReturnsSpecificTimeoutExitCode() {
    var exception =
        new RuntimeException(new ModbusException("outer", new ModbusTimeoutException("fake")));

    assertEquals(5, Modbus.mapExitCode(exception));
  }

  @Test
  void unexpectedExceptionReturnsInternalErrorExitCode() {
    assertEquals(10, Modbus.mapExitCode(new RuntimeException("fake")));
  }

  @Test
  void invalidSerialPortEndpointReturnsConnectionExitCode() {
    var result =
        CliTestRunner.execute("client", "rtu:/dev/ttyFAKE0", "read-holding-registers", "0", "1");

    assertEquals(3, result.exitCode());
  }

  @Test
  void jsonConnectionFailureWritesErrorRecordToStdout() {
    var result =
        CliTestRunner.execute(
            "--json", "client", "127.0.0.1", "-p", "1", "read-holding-registers", "0", "1");

    assertEquals(3, result.exitCode());
    assertTrue(result.stderr().isBlank());
    assertTrue(result.stdout().contains("\"kind\":\"error\""));
    assertTrue(result.stdout().contains("\"message\":"));
  }
}
