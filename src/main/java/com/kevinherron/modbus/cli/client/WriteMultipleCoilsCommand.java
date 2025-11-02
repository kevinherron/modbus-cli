package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsRequest;
import com.digitalpetri.modbus.pdu.WriteMultipleCoilsResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "wmc", description = "Write Multiple Coils")
class WriteMultipleCoilsCommand implements Runnable {

  @Parameters(index = "0", description = "starting address")
  int address;

  @Parameters(index = "1", description = "quantity of coils")
  int quantity;

  @Parameters(index = "2", description = "coil values (comma-separated, true/false or 1/0)")
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
