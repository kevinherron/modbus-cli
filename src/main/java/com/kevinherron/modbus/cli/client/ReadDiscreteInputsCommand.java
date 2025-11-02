package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsRequest;
import com.digitalpetri.modbus.pdu.ReadDiscreteInputsResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "rdi", description = "Read Discrete Inputs")
class ReadDiscreteInputsCommand implements Runnable {

  @Parameters(index = "0", description = "starting address")
  int address;

  @Parameters(index = "1", description = "quantity of discrete inputs")
  int quantity;

  @Option(
      names = {"-c", "--count"},
      description = "number of times to repeat (default: 1, 0 = indefinite)")
  int count = 1;

  @Option(
      names = {"-i", "--interval"},
      description = "interval between reads in milliseconds (default: 1000)")
  int interval = 1000;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    if (count == 1) {
      // Single read
      clientCommand.runWithClient(this::executeRead);
    } else {
      // Polling mode
      clientCommand.runWithClientPolling(this::executeRead, count, interval);
    }
  }

  private void executeRead(ModbusTcpClient client, int unitId, OutputContext output)
      throws ModbusException {

    var request = new ReadDiscreteInputsRequest(address, quantity);

    output.protocol(request, Direction.SENT, null);

    ReadDiscreteInputsResponse response = client.readDiscreteInputs(unitId, request);
    Instant responseTime = Instant.now();

    output.protocol(response, Direction.RECEIVED, responseTime);

    byte[] discreteInputs = response.inputs();

    output
        .coilTable()
        .data(discreteInputs)
        .startAddress(address)
        .quantity(quantity)
        .timestamp(responseTime)
        .render();
  }
}
