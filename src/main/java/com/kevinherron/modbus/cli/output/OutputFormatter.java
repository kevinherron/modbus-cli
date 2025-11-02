package com.kevinherron.modbus.cli.output;

import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.kevinherron.modbus.cli.client.ScanCommand.ScanResult;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for formatting output in different styles (human, JSON, etc.).
 *
 * <p>Implementations handle the actual rendering of output based on format requirements.
 */
public interface OutputFormatter {

  /**
   * Sets the current iteration number for polling operations.
   *
   * @param iteration the iteration number, or null for single operations
   */
  void setIteration(Integer iteration);

  /**
   * Formats a protocol message.
   *
   * @param out the output stream
   * @param pdu the message object
   * @param direction whether sent or received
   * @param timestamp the timestamp when the message was sent/received, or null to use the current
   *     time.
   * @param options output options
   */
  void formatProtocol(
      PrintStream out,
      ModbusPdu pdu,
      Direction direction,
      @Nullable Instant timestamp,
      OutputOptions options);

  /**
   * Formats a message with a specific type.
   *
   * @param out the output stream (stdout or stderr depending on type)
   * @param type the semantic type of the message
   * @param message the message text
   * @param options output options
   */
  void formatMessage(PrintStream out, OutputType type, String message, OutputOptions options);

  /**
   * Formats register data as a table.
   *
   * @param out the output stream
   * @param registers the register byte array
   * @param startAddress the starting address
   * @param timestamp the timestamp when the data was received, or null to use current time
   * @param options output options
   */
  void formatRegisterTable(
      PrintStream out,
      byte[] registers,
      int startAddress,
      @Nullable Instant timestamp,
      OutputOptions options);

  /**
   * Formats coil/discrete input data as a table.
   *
   * @param out the output stream
   * @param coilBytes the coil byte array
   * @param startAddress the starting address
   * @param quantity the number of coils
   * @param timestamp the timestamp when the data was received, or null to use current time
   * @param options output options
   */
  void formatCoilTable(
      PrintStream out,
      byte[] coilBytes,
      int startAddress,
      int quantity,
      @Nullable Instant timestamp,
      OutputOptions options);

  /**
   * Formats scan results.
   *
   * @param out the output stream
   * @param results the list of scan results
   * @param options output options
   */
  void formatScanResults(PrintStream out, List<ScanResult> results, OutputOptions options);
}
