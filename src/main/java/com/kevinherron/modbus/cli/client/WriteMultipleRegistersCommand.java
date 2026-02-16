package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 16 (Write Multiple Registers) operation.
 *
 * <p>This command writes multiple consecutive holding registers in a single transaction. Holding
 * registers are 16-bit read/write values commonly used for configuration settings, setpoints, and
 * other numeric data.
 *
 * <p>This command is invoked using {@code wmr} (e.g., {@code modbus client wmr 0 3 100,0x64,200}).
 *
 * <p>Register values are encoded in big-endian byte order per the Modbus protocol: the high byte is
 * transmitted first, followed by the low byte. Each 16-bit register value is converted to 2 bytes
 * using {@code (value >> 8) & 0xFF} for the high byte and {@code value & 0xFF} for the low byte.
 *
 * @see WriteSingleRegisterCommand for writing a single register (function code 06)
 * @see ReadHoldingRegistersCommand for reading holding registers (function code 03)
 */
@Command(name = "wmr", description = "Write Multiple Registers")
class WriteMultipleRegistersCommand implements Runnable {

  /** The starting address for writing registers. Addressing is typically 0-based. */
  @Parameters(index = "0", description = "starting address")
  int address;

  /** The number of consecutive registers to write. Must match the number of values provided. */
  @Parameters(index = "1", description = "quantity of registers")
  int quantity;

  /**
   * Comma-separated register values to write. Accepts decimal (e.g., {@code 1234}) or hexadecimal
   * (e.g., {@code 0x04D2}) formats parsed by {@link ValueParser#parseRegisterValue(String)}. The
   * number of values must exactly match the {@code quantity} parameter.
   */
  @Parameters(
      index = "2",
      description = "register values (comma-separated, decimal or hex, e.g., 100,0x64,200)")
  String values;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusClient client, int unitId, OutputContext output) -> {
          String[] valueStrings = values.split(",");
          if (valueStrings.length != quantity) {
            throw new IllegalArgumentException(
                "number of values (%d) does not match quantity (%d)"
                    .formatted(valueStrings.length, quantity));
          }

          byte[] registerValues = new byte[quantity * 2];
          for (int i = 0; i < quantity; i++) {
            int value = ValueParser.parseRegisterValue(valueStrings[i].trim());
            registerValues[i * 2] = (byte) ((value >> 8) & 0xFF);
            registerValues[i * 2 + 1] = (byte) (value & 0xFF);
          }

          var request = new WriteMultipleRegistersRequest(address, quantity, registerValues);

          output.protocol(request, Direction.SENT, null);

          WriteMultipleRegistersResponse response = client.writeMultipleRegisters(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
