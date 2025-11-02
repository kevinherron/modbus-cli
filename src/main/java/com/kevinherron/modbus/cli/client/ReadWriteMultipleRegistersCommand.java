package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "rwmr", description = "Read/Write Multiple Registers")
class ReadWriteMultipleRegistersCommand implements Runnable {

  @Parameters(index = "0", description = "read starting address")
  int readAddress;

  @Parameters(index = "1", description = "read quantity")
  int readQuantity;

  @Parameters(index = "2", description = "write starting address")
  int writeAddress;

  @Parameters(index = "3", description = "write quantity")
  int writeQuantity;

  @Parameters(index = "4", description = "write values (comma-separated)")
  String writeValues;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
          String[] valueStrings = writeValues.split(",");
          if (valueStrings.length != writeQuantity) {
            throw new IllegalArgumentException(
                "number of write values (%d) does not match write quantity (%d)"
                    .formatted(valueStrings.length, writeQuantity));
          }

          // Convert register values to bytes (2 bytes per register, big-endian)
          byte[] registerBytes = new byte[writeQuantity * 2];
          for (int i = 0; i < writeQuantity; i++) {
            int value = Integer.parseInt(valueStrings[i].trim());
            registerBytes[i * 2] = (byte) ((value >> 8) & 0xFF);
            registerBytes[i * 2 + 1] = (byte) (value & 0xFF);
          }

          var request =
              new ReadWriteMultipleRegistersRequest(
                  readAddress, readQuantity, writeAddress, writeQuantity, registerBytes);

          output.protocol(request, Direction.SENT, null);

          ReadWriteMultipleRegistersResponse response =
              client.readWriteMultipleRegisters(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);

          byte[] registers = response.registers();

          output
              .registerTable()
              .data(registers)
              .startAddress(readAddress)
              .timestamp(responseTime)
              .render();
        });
  }
}
