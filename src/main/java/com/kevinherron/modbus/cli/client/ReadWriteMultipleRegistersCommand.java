package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadWriteMultipleRegistersResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 23 (Read/Write Multiple Registers) operation.
 *
 * <p>This command performs both a read and a write operation in a single atomic transaction. The
 * write operation is performed before the read operation, but both are executed as part of the same
 * Modbus request/response cycle. This atomicity ensures that no other master can intervene between
 * the write and read operations.
 *
 * <p>This command is invoked using {@code rwmr} (e.g., {@code modbus client rwmr 0 5 10 3
 * 100,200,300} to read 5 registers starting at address 0 and write 3 registers starting at address
 * 10).
 *
 * <p>Write values are encoded in big-endian byte order per the Modbus protocol: the high byte is
 * transmitted first, followed by the low byte. Each 16-bit register value is converted to 2 bytes
 * using {@code (value >> 8) & 0xFF} for the high byte and {@code value & 0xFF} for the low byte.
 *
 * @see ReadHoldingRegistersCommand for reading registers only (function code 03)
 * @see WriteMultipleRegistersCommand for writing registers only (function code 16)
 */
@Command(name = "rwmr", description = "Read/Write Multiple Registers")
class ReadWriteMultipleRegistersCommand implements Runnable {

  /** The starting address for reading registers. Addressing is typically 0-based. */
  @Parameters(index = "0", description = "read starting address")
  int readAddress;

  /** The number of consecutive registers to read. */
  @Parameters(index = "1", description = "read quantity")
  int readQuantity;

  /** The starting address for writing registers. Addressing is typically 0-based. */
  @Parameters(index = "2", description = "write starting address")
  int writeAddress;

  /** The number of consecutive registers to write. Must match the number of values provided. */
  @Parameters(index = "3", description = "write quantity")
  int writeQuantity;

  /**
   * Comma-separated register values to write. Accepts decimal values (e.g., {@code 100,200,300}).
   * The number of values must exactly match the {@code writeQuantity} parameter.
   */
  @Parameters(index = "4", description = "write values (comma-separated)")
  String writeValues;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusClient client, int unitId, OutputContext output) -> {
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
