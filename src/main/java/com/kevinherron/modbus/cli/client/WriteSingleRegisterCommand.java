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

@Command(name = "wsr", description = "Write Single Register")
class WriteSingleRegisterCommand implements Runnable {

  @Parameters(index = "0", description = "register address")
  int address;

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
