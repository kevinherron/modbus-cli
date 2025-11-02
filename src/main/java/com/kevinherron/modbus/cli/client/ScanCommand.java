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

@Command(name = "scan", description = "scan a range of registers using a sliding window")
public class ScanCommand implements Runnable {

  @Parameters(index = "0", description = "start address (inclusive)")
  int start;

  @Parameters(index = "1", description = "end address (exclusive)")
  int end;

  @Option(
      names = "--size",
      description = "window size, i.e. number of registers to read in each window")
  Integer size;

  @Option(
      names = "--step",
      description =
          "step size, i.e. number of registers to move the window forward by on each step")
  Integer step;

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

  public record ScanResult(int address, byte[] registers) {}
}
