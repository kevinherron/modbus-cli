package com.kevinherron.modbus.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    var result = CliTestRunner.execute("client", "127.0.0.1", "-p", "1", "rhr", "0", "1");

    assertEquals(3, result.exitCode());
    assertFalse(result.stderr().isBlank());
    assertTrue(
        result.stderr().toLowerCase(Locale.ROOT).contains("connect")
            || result.stderr().toLowerCase(Locale.ROOT).contains("refused"));
  }

  @Test
  void jsonConnectionFailureWritesErrorRecordToStdout() {
    var result =
        CliTestRunner.execute(
            "--format", "json", "client", "127.0.0.1", "-p", "1", "rhr", "0", "1");

    assertEquals(3, result.exitCode());
    assertTrue(result.stderr().isBlank());
    assertTrue(result.stdout().contains("\"kind\":\"error\""));
    assertTrue(result.stdout().contains("\"message\":"));
  }
}
