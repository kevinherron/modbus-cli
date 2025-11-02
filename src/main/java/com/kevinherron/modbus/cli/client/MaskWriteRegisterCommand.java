package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterRequest;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "mwr", description = "Mask Write Register")
class MaskWriteRegisterCommand implements Runnable {

  @Parameters(index = "0", description = "register address")
  int address;

  @Parameters(index = "1", description = "AND mask (hex, e.g., 0xFFFF or FFFF)")
  String andMask;

  @Parameters(index = "2", description = "OR mask (hex, e.g., 0x0000 or 0000)")
  String orMask;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
          int andMaskValue = ValueParser.parseHexValue(andMask);
          int orMaskValue = ValueParser.parseHexValue(orMask);

          var request = new MaskWriteRegisterRequest(address, andMaskValue, orMaskValue);

          output.protocol(request, Direction.SENT, null);

          MaskWriteRegisterResponse response = client.maskWriteRegister(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
