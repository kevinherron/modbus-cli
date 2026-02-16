package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 15 (Write Multiple Coils) operation.
 *
 * <p>This command writes multiple coils (discrete outputs) in a single transaction. Coils are
 * read/write boolean values typically representing physical outputs like relays or actuators.
 *
 * <p>This command is invoked using {@code wmc} (e.g., {@code modbus client wmc 0 4
 * true,false,1,0}).
 *
 * <p>Coil values are packed into bytes using LSB-first bit ordering per the Modbus protocol. The
 * byte count is calculated as {@code (quantity + 7) / 8}, and each coil value is set as a bit
 * within the appropriate byte position.
 *
 * @see WriteSingleCoilCommand for writing a single coil (function code 05)
 * @see ReadCoilsCommand for reading coil values (function code 01)
 */
@Command(name = "wmc", description = "Write Multiple Coils")
class WriteMultipleCoilsCommand implements Runnable {

  /** The starting address for writing coils. Addressing is typically 0-based. */
  @Parameters(index = "0", description = "starting address")
  int address;

  /** The number of consecutive coils to write. Must match the number of values provided. */
  @Parameters(index = "1", description = "quantity of coils")
  int quantity;

  /**
   * Comma-separated coil values to write. Accepts flexible boolean formats parsed by {@link
   * ValueParser#parseCoilValue(String)}: {@code true}/{@code false}, {@code 1}/{@code 0}, or {@code
   * on}/{@code off} (case-insensitive). The number of values must exactly match the {@code
   * quantity} parameter.
   */
  @Parameters(index = "2", description = "coil values (comma-separated, true/false or 1/0)")
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

          // Parse coil values
          boolean[] coilValues = new boolean[quantity];
          for (int i = 0; i < quantity; i++) {
            coilValues[i] = ValueParser.parseCoilValue(valueStrings[i].trim());
          }

          // Pack coil values into bytes (LSB-first per Modbus protocol)
          int byteCount = (quantity + 7) / 8;
          byte[] coilBytes = new byte[byteCount];

          for (int i = 0; i < quantity; i++) {
            if (coilValues[i]) {
              int byteIndex = i / 8;
              int bitIndex = i % 8;
              coilBytes[byteIndex] |= (byte) (1 << bitIndex);
            }
          }

          var request = new WriteMultipleCoilsRequest(address, quantity, coilBytes);

          output.protocol(request, Direction.SENT, null);

          WriteMultipleCoilsResponse response = client.writeMultipleCoils(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
