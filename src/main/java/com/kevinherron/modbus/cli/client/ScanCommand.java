package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.util.ArrayList;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Scans a range of holding registers using a sliding window approach.
 *
 * <p>This command is useful for discovering active register ranges on unknown devices. It reads
 * registers in configurable windows, stepping through the specified address range and reporting
 * results for each successful read.
 *
 * <p>The sliding window algorithm works as follows:
 *
 * <ul>
 *   <li>Start at the {@code start} address and read {@code size} registers
 *   <li>Move forward by {@code step} registers (defaults to {@code size} for non-overlapping
 *       windows)
 *   <li>Continue until reaching or exceeding the {@code end} address
 *   <li>Optionally include partial windows at the end if the remaining range is smaller than {@code
 *       size}
 * </ul>
 *
 * <p>This command is invoked using {@code scan} (e.g., {@code modbus client scan 0 100 --size 10}).
 *
 * @see ReadHoldingRegistersCommand for reading a specific range of holding registers
 */
@Command(name = "scan", description = "scan a range of registers using a sliding window")
public class ScanCommand implements Runnable {

  /** Starting address (inclusive) for the scan range. */
  @Parameters(index = "0", description = "start address (inclusive)")
  int start;

  /**
   * Ending address (exclusive) for the scan range. The scan will read registers from {@code start}
   * up to but not including {@code end}.
   */
  @Parameters(index = "1", description = "end address (exclusive)")
  int end;

  /** Window size specifying the number of registers to read in each scan iteration. */
  @Option(
      names = "--size",
      description = "window size, i.e. number of registers to read in each window")
  Integer size;

  /**
   * Step size specifying how many registers to advance the window between iterations. When not
   * specified, defaults to the window size for non-overlapping windows. Use a smaller step than
   * size for overlapping windows.
   */
  @Option(
      names = "--step",
      description =
          "step size, i.e. number of registers to move the window forward by on each step")
  Integer step;

  /**
   * Whether to include partial windows at the end of the range. When true (default), the final
   * window may be smaller than {@code size} if the remaining range is insufficient. When false,
   * partial windows are skipped.
   */
  @Option(
      names = "--partial",
      defaultValue = "true",
      description = "read partial windows if the last window is smaller than the window size")
  boolean partial;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    if (size == null) {
      size = 10;
    }
    if (step == null) {
      step = size;
    }

    int quantity = end - start;

    var results = new ArrayList<ScanResult>();

    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
          for (int i = start; i < start + quantity; i += step) {
            int windowSize = Math.min(size, start + quantity - i);
            if (windowSize <= 0) {
              break;
            }

            // Skip partial windows if partial is false
            if (!partial && windowSize < size) {
              continue;
            }

            var request = new ReadHoldingRegistersRequest(i, windowSize);

            ReadHoldingRegistersResponse response = client.readHoldingRegisters(unitId, request);

            results.add(new ScanResult(i, response.registers()));
          }

          output.scanResults().results(results).render();
        });
  }

  /**
   * Container for scan results, holding the starting address and register data for a single window.
   *
   * @param address the starting address of this scan window.
   * @param registers the raw register data read from this window (2 bytes per register,
   *     big-endian).
   */
  public record ScanResult(int address, byte[] registers) {}
}
