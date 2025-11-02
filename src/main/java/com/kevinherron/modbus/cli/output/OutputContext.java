package com.kevinherron.modbus.cli.output;

import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.kevinherron.modbus.cli.client.ScanCommand.ScanResult;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Main interface for command output operations.
 *
 * <p>Commands use this interface to emit semantic output events (data, errors, status) which are
 * rendered by the underlying formatter according to user preferences.
 */
public interface OutputContext {

  /**
   * Sets the current iteration number for polling operations.
   *
   * @param iteration the iteration number, or null for single operations
   */
  void setIteration(Integer iteration);

  /**
   * Outputs a protocol message (request or response).
   *
   * @param pdu the message object
   * @param direction whether it was sent or received
   * @param timestamp the timestamp when the message was sent/received, or null to use the current
   *     time.
   */
  void protocol(ModbusPdu pdu, Direction direction, @Nullable Instant timestamp);

  /**
   * Outputs an informational message.
   *
   * @param format the format string
   * @param args format arguments
   */
  void info(String format, Object... args);

  /**
   * Outputs a success message.
   *
   * @param format the format string
   * @param args format arguments
   */
  void success(String format, Object... args);

  /**
   * Outputs a warning message.
   *
   * @param format the format string
   * @param args format arguments
   */
  void warning(String format, Object... args);

  /**
   * Outputs an error message.
   *
   * @param format the format string
   * @param args format arguments
   */
  void error(String format, Object... args);

  /**
   * Creates a builder for outputting register data as a table.
   *
   * @return a register table builder
   */
  RegisterTableBuilder registerTable();

  /**
   * Creates a builder for outputting coil/discrete input data as a table.
   *
   * @return a coil table builder
   */
  CoilTableBuilder coilTable();

  /**
   * Creates a builder for outputting scan results.
   *
   * @return a scan results builder
   */
  ScanResultsBuilder scanResults();

  /** Builder for register table output. */
  interface RegisterTableBuilder {
    RegisterTableBuilder data(byte[] registers);

    RegisterTableBuilder startAddress(int address);

    RegisterTableBuilder timestamp(@Nullable Instant timestamp);

    void render();
  }

  /** Builder for coil/discrete input table output. */
  interface CoilTableBuilder {
    CoilTableBuilder data(byte[] coilBytes);

    CoilTableBuilder startAddress(int address);

    CoilTableBuilder quantity(int quantity);

    CoilTableBuilder timestamp(@Nullable Instant timestamp);

    void render();
  }

  /** Builder for scan results output. */
  interface ScanResultsBuilder {
    ScanResultsBuilder results(List<ScanResult> results);

    void render();
  }
}
