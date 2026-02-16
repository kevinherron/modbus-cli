package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 05 (Write Single Coil) operation.
 *
 * <p>This command writes a single coil (discrete output) to either ON or OFF state. Coils are
 * read/write boolean values typically representing physical outputs like relays or actuators.
 *
 * <p>This command is invoked using {@code wsc} (e.g., {@code modbus client wsc 0 true}).
 *
 * @see WriteMultipleCoilsCommand for writing multiple coils (function code 15)
 * @see ReadCoilsCommand for reading coil values (function code 01)
 */
@Command(name = "wsc", description = "Write Single Coil")
class WriteSingleCoilCommand implements Runnable {

  /** The address of the coil to write. Addressing is typically 0-based. */
  @Parameters(index = "0", description = "coil address")
  int address;

  /**
   * The value to write to the coil. Accepts flexible boolean formats parsed by {@link
   * ValueParser#parseCoilValue(String)}: {@code true}/{@code false}, {@code 1}/{@code 0}, or {@code
   * on}/{@code off} (case-insensitive).
   */
  @Parameters(index = "1", description = "coil value (true/false, 1/0, on/off)")
  String value;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusClient client, int unitId, OutputContext output) -> {
          boolean coilValue = ValueParser.parseCoilValue(value);

          var request = new WriteSingleCoilRequest(address, coilValue);

          output.protocol(request, Direction.SENT, null);

          WriteSingleCoilResponse response = client.writeSingleCoil(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
