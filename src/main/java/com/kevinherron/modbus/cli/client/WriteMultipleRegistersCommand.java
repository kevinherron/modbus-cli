package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleRegistersResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "wmr", description = "Write Multiple Registers")
class WriteMultipleRegistersCommand implements Runnable {

  @Parameters(index = "0", description = "starting address")
  int address;

  @Parameters(index = "1", description = "quantity of registers")
  int quantity;

  @Parameters(
      index = "2",
      description = "register values (comma-separated, decimal or hex, e.g., 100,0x64,200)")
  String values;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
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
