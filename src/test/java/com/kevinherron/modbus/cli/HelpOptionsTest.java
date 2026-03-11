package com.kevinherron.modbus.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kevinherron.modbus.cli.test.CliTestRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;

class HelpOptionsTest {

  @Test
  void rootHelpOptionPrintsUsage() {
    var result = CliTestRunner.execute("--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Modbus CLI"));
    assertTrue(
        result
            .stdout()
            .contains("Command-line interface for Modbus TCP and RTU serial operations."));
    assertTrue(result.stdout().contains("Usage: modbus"));
    assertTrue(result.stdout().contains("-h, --help"));
    assertTrue(result.stdout().contains("-V, --version"));
    assertFalse(result.stdout().isBlank());
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void rootVersionOptionPrintsVersion() {
    var result = CliTestRunner.execute("--version");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("modbus-cli "));
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void clientHelpOptionPrintsUsage() {
    var result = CliTestRunner.execute("client", "--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Connect to a Modbus endpoint"));
    assertTrue(result.stdout().contains("required <endpoint> positional parameter"));
    assertTrue(result.stdout().contains("Usage: modbus client"));
    assertTrue(result.stdout().contains("-h, --help"));
    assertTrue(result.stdout().contains("-V, --version"));
    assertTrue(result.stdout().contains("<endpoint>"));
    assertTrue(result.stdout().contains("required connection target before the subcommand"));
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void clientVersionOptionPrintsVersion() {
    var result = CliTestRunner.execute("client", "--version");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("modbus-cli "));
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void subcommandHelpOptionPrintsUsage() {
    var result = CliTestRunner.execute("client", "localhost", "read-holding-registers", "--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Modbus function code 03 (Read Holding Registers)."));
    assertTrue(result.stdout().contains("Read-only and idempotent; safe to repeat"));
    assertTrue(
        result.stdout().contains("Example: modbus client <endpoint> read-holding-registers 0 10"));
    assertTrue(result.stdout().contains("Usage: modbus client"));
    assertTrue(result.stdout().contains("read-holding-registers"));
    assertTrue(result.stdout().contains("-h, --help"));
    assertTrue(result.stdout().contains("-V, --version"));
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void subcommandVersionOptionPrintsVersion() {
    var result =
        CliTestRunner.execute("client", "localhost", "read-holding-registers", "--version");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("modbus-cli "));
    assertTrue(result.stderr().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "rc, Read Coils",
    "rdi, Read Discrete Inputs",
    "rhr, Read Holding Registers",
    "rir, Read Input Registers",
    "wsc, Write Single Coil",
    "wmc, Write Multiple Coils",
    "wsr, Write Single Register",
    "wmr, Write Multiple Registers",
    "mwr, Mask Write Register",
    "rwmr, Read/Write Multiple Registers"
  })
  void shortFormSubcommandAliasPrintsUsage(String alias, String description) {
    var result = CliTestRunner.execute("client", "localhost", alias, "--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Usage: modbus client"));
    assertTrue(result.stdout().contains(description));
    assertTrue(result.stdout().contains("-h, --help"));
    assertTrue(result.stdout().contains("-V, --version"));
    assertTrue(result.stderr().isEmpty());
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "read-coils|Modbus function code 01 (Read Coils).|Read-only and idempotent; safe to repeat without changing device state.|Example: modbus client <endpoint> read-coils 0 10",
        "read-discrete-inputs|Modbus function code 02 (Read Discrete Inputs).|Read-only and idempotent; safe to repeat without changing device state.|Example: modbus client <endpoint> read-discrete-inputs 0 10",
        "read-holding-registers|Modbus function code 03 (Read Holding Registers).|Read-only and idempotent; safe to repeat without changing device state.|Example: modbus client <endpoint> read-holding-registers 0 10",
        "read-input-registers|Modbus function code 04 (Read Input Registers).|Read-only and idempotent; safe to repeat without changing device state.|Example: modbus client <endpoint> read-input-registers 0 10",
        "write-single-coil|Modbus function code 05 (Write Single Coil).|Mutating and idempotent when repeated with the same value.|Example: modbus client <endpoint> write-single-coil 0 true",
        "write-single-register|Modbus function code 06 (Write Single Register).|Mutating and idempotent when repeated with the same value.|Example: modbus client <endpoint> write-single-register 0 1234",
        "write-multiple-coils|Modbus function code 15 (Write Multiple Coils).|Mutating and idempotent when repeated with the same values.|Example: modbus client <endpoint> write-multiple-coils 0 4 true,false,1,0",
        "write-multiple-registers|Modbus function code 16 (Write Multiple Registers).|Mutating and idempotent when repeated with the same values.|Example: modbus client <endpoint> write-multiple-registers 0 3 100,0x64,200",
        "mask-write-register|Modbus function code 22 (Mask Write Register).|Mutating and idempotent when repeated with the same masks.|Example: modbus client <endpoint> mask-write-register 0 0xFF00 0x00FF",
        "read-write-multiple-registers|Modbus function code 23 (Read/Write Multiple Registers).|Mutating and idempotent when repeated with the same write payload.|Example: modbus client <endpoint> read-write-multiple-registers",
        "scan|Uses repeated Modbus function code 03 (Read Holding Registers) requests.|Read-only and idempotent; safe to repeat without changing device state.|Example: modbus client <endpoint> scan 0 100 --size 10"
      },
      delimiter = '|')
  void subcommandHelpIncludesFunctionCodeMutabilityAndExample(
      String commandName, String functionCodeText, String mutabilityText, String exampleText) {
    var result = CliTestRunner.execute("client", "localhost", commandName, "--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains(functionCodeText));
    assertTrue(result.stdout().contains(mutabilityText));
    assertTrue(result.stdout().contains(exampleText));
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void invalidLongFormSubcommandAliasFails() {
    var clientSubcommands = new CommandLine(new ModbusCommand()).getSubcommands().get("client");

    assertFalse(clientSubcommands.getSubcommands().containsKey("read-holding-registers-extra"));
  }

  @Test
  void serverHelpOptionPrintsUsage() {
    var result = CliTestRunner.execute("server", "--help");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Usage: modbus server"));
    assertTrue(result.stdout().contains("-h, --help"));
    assertTrue(result.stdout().contains("-V, --version"));
    assertTrue(result.stderr().isEmpty());
  }

  @Test
  void serverVersionOptionPrintsVersion() {
    var result = CliTestRunner.execute("server", "--version");

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("modbus-cli "));
    assertTrue(result.stderr().isEmpty());
  }
}
