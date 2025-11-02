package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.ReadInputRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadInputRegistersResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "rir", description = "Read Input Registers")
class ReadInputRegistersCommand implements Runnable {

  @Parameters(index = "0", description = "starting address")
  int address;

  @Parameters(index = "1", description = "quantity of registers")
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
    var request = new ReadInputRegistersRequest(address, quantity);

    output.protocol(request, Direction.SENT, null);

    ReadInputRegistersResponse response = client.readInputRegisters(unitId, request);
    Instant responseTime = Instant.now();

    output.protocol(response, Direction.RECEIVED, responseTime);

    byte[] registers = response.registers();

    output.registerTable().data(registers).startAddress(address).timestamp(responseTime).render();
  }
}
