package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 06 (Write Single Register) operation.
 *
 * <p>This command writes a single 16-bit value to a holding register. Holding registers are
 * read/write values typically used for configuration settings, setpoints, or control parameters.
 *
 * <p>This command is invoked using {@code wsr} (e.g., {@code modbus client wsr 0 1234}).
 *
 * @see WriteMultipleRegistersCommand for writing multiple registers (function code 16)
 * @see ReadHoldingRegistersCommand for reading holding register values (function code 03)
 */
@Command(name = "wsr", description = "Write Single Register")
class WriteSingleRegisterCommand implements Runnable {

  /** The address of the register to write. Addressing is typically 0-based. */
  @Parameters(index = "0", description = "register address")
  int address;

  /**
   * The value to write to the register. Accepts decimal (e.g., {@code 1234}) or hexadecimal (e.g.,
   * {@code 0x04D2}) formats, parsed by {@link ValueParser#parseRegisterValue(String)}.
   */
  @Parameters(index = "1", description = "register value (decimal or hex, e.g., 1234 or 0x04D2)")
  String value;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
          int registerValue = ValueParser.parseRegisterValue(value);

          var request = new WriteSingleRegisterRequest(address, registerValue);

          output.protocol(request, Direction.SENT, null);

          WriteSingleRegisterResponse response = client.writeSingleRegister(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
