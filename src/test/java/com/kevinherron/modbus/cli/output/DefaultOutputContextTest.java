package com.kevinherron.modbus.cli.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DefaultOutputContextTest {

  @Test
  void jsonWarningsAndErrorsUseStdout() {
    var stdoutCapture = new ByteArrayOutputStream();
    var stderrCapture = new ByteArrayOutputStream();
    var output =
        new DefaultOutputContext(
            new JsonFormatter(),
            OutputOptions.json(),
            new PrintStream(stdoutCapture, true, StandardCharsets.UTF_8),
            new PrintStream(stderrCapture, true, StandardCharsets.UTF_8));

    output.warning("warning message");
    output.error("error message");

    String stdout = stdoutCapture.toString(StandardCharsets.UTF_8);
    String stderr = stderrCapture.toString(StandardCharsets.UTF_8);

    assertTrue(stdout.contains("\"kind\":\"event\""));
    assertTrue(stdout.contains("\"message\":\"warning message\""));
    assertTrue(stdout.contains("\"kind\":\"error\""));
    assertTrue(stdout.contains("\"message\":\"error message\""));
    assertEquals("", stderr);
  }

  @Test
  void humanWarningsAndErrorsUseStderr() {
    var stdoutCapture = new ByteArrayOutputStream();
    var stderrCapture = new ByteArrayOutputStream();
    var output =
        new DefaultOutputContext(
            new HumanFormatter(),
            OutputOptions.human(false, false, false),
            new PrintStream(stdoutCapture, true, StandardCharsets.UTF_8),
            new PrintStream(stderrCapture, true, StandardCharsets.UTF_8));

    output.warning("warning message");
    output.error("error message");

    String stdout = stdoutCapture.toString(StandardCharsets.UTF_8);
    String stderr = stderrCapture.toString(StandardCharsets.UTF_8);

    assertEquals("", stdout);
    assertTrue(stderr.contains("warning message"));
    assertTrue(stderr.contains("error message"));
  }
}
