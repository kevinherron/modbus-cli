package com.kevinherron.modbus.cli.test;

import com.kevinherron.modbus.cli.ModbusCommand;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine;

/**
 * A utility for executing CLI commands programmatically in tests using picocli best practices.
 *
 * <p>This class supports both black box testing (verifying exit codes and output) and white box
 * testing (accessing command object state after execution).
 *
 * <p>Example black box testing:
 *
 * <pre>{@code
 * CliTestRunner.Result result = CliTestRunner.execute(
 *     "client", "localhost", "-p", "15502", "read-holding-registers", "100", "10");
 *
 * assertEquals(0, result.exitCode());
 * assertTrue(result.stdout().contains("100"));
 * }</pre>
 *
 * <p>Example white box testing:
 *
 * <pre>{@code
 * ModbusCommand cmd = new ModbusCommand();
 * CliTestRunner.Result result = CliTestRunner.execute(cmd,
 *     "client", "localhost", "-p", "15502", "read-holding-registers", "100", "10");
 *
 * assertEquals(0, result.exitCode());
 * // Can access cmd internal state here
 * }</pre>
 */
public class CliTestRunner {

  /**
   * Execute a CLI command with the given arguments (black box testing).
   *
   * <p>This method captures stdout and stderr during execution and returns them along with the exit
   * code.
   *
   * @param args the command-line arguments (without the "modbus" prefix).
   * @return the result of the command execution.
   */
  public static Result execute(String... args) {
    return execute(new ModbusCommand(), args);
  }

  /**
   * Execute a CLI command using the provided command object (white box testing).
   *
   * <p>This allows tests to inspect the command object's state after execution.
   *
   * @param command the ModbusCommand instance to use.
   * @param args the command-line arguments (without the "modbus" prefix).
   * @return the result of the command execution.
   */
  public static Result execute(ModbusCommand command, String... args) {
    var stdoutCapture = new ByteArrayOutputStream();
    var stderrCapture = new ByteArrayOutputStream();

    var originalStdout = System.out;
    var originalStderr = System.err;

    int exitCode;
    try {
      // Capture both System.out/err and picocli's output streams
      System.setOut(new PrintStream(stdoutCapture, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(stderrCapture, true, StandardCharsets.UTF_8));

      var cmd = new CommandLine(command);
      exitCode = cmd.execute(args);
    } finally {
      System.setOut(originalStdout);
      System.setErr(originalStderr);
    }

    return new Result(
        exitCode,
        stdoutCapture.toString(StandardCharsets.UTF_8),
        stderrCapture.toString(StandardCharsets.UTF_8),
        command,
        null);
  }

  /**
   * The result of a CLI command execution.
   *
   * @param exitCode the exit code returned by the command.
   * @param stdout the content written to stdout during execution.
   * @param stderr the content written to stderr during execution.
   * @param command the ModbusCommand instance that was executed (for white box testing).
   * @param commandLine the CommandLine instance that was used (for advanced inspection).
   */
  public record Result(
      int exitCode, String stdout, String stderr, ModbusCommand command, CommandLine commandLine) {

    /**
     * Check if the command executed successfully (exit code 0).
     *
     * @return true if the exit code is 0, false otherwise.
     */
    public boolean isSuccess() {
      return exitCode == 0;
    }

    /**
     * Get the combined output from stdout and stderr.
     *
     * @return the combined output.
     */
    public String getOutput() {
      return stdout + stderr;
    }
  }
}
