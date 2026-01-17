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

/**
 * Implements the Modbus function code 04 (Read Input Registers) operation.
 *
 * <p>Input registers are 16-bit read-only values. Unlike holding registers, input registers cannot
 * be modified by the Modbus master. They typically represent measurement data, sensor readings, or
 * other values that are produced by the device.
 *
 * <p>This command is invoked using {@code rir} (e.g., {@code modbus client rir 0 10}).
 *
 * @see ReadHoldingRegistersCommand for read/write holding registers (function code 03)
 */
@Command(name = "rir", description = "Read Input Registers")
class ReadInputRegistersCommand implements Runnable {

  /**
   * Starting register address. Addressing is typically 0-based, though some devices use 1-based.
   */
  @Parameters(index = "0", description = "starting address")
  int address;

  /** Number of registers to read, starting from the specified address. */
  @Parameters(index = "1", description = "quantity of registers")
  int quantity;

  /**
   * Number of times to execute the read operation. A value of 0 means indefinite polling until
   * interrupted.
   */
  @Option(
      names = {"-c", "--count"},
      description = "number of times to repeat (default: 1, 0 = indefinite)")
  int count = 1;

  /** Delay between consecutive reads in polling mode, specified in milliseconds. */
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

  /**
   * Executes the Read Input Registers request and renders the results as a register table.
   *
   * @param client the connected Modbus TCP client.
   * @param unitId the target unit identifier.
   * @param output the output context for rendering protocol messages and results.
   * @throws ModbusException if the Modbus operation fails.
   */
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
