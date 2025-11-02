package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.WriteSingleCoilRequest;
import com.digitalpetri.modbus.pdu.WriteSingleCoilResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "wsc", description = "Write Single Coil")
class WriteSingleCoilCommand implements Runnable {

  @Parameters(index = "0", description = "coil address")
  int address;

  @Parameters(index = "1", description = "coil value (true/false, 1/0, on/off)")
  String value;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
          boolean coilValue = ValueParser.parseCoilValue(value);

          var request = new WriteSingleCoilRequest(address, coilValue);

          output.protocol(request, Direction.SENT, null);

          WriteSingleCoilResponse response = client.writeSingleCoil(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
