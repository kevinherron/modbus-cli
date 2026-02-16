package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
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

/**
 * Implements the Modbus function code 02 (Read Discrete Inputs) operation.
 *
 * <p>Discrete inputs are read-only boolean values representing physical inputs such as sensors,
 * switches, or status indicators. Unlike coils (function code 01), discrete inputs cannot be
 * written to—they reflect the state of external signals.
 *
 * <p>This command is invoked using {@code rdi} (e.g., {@code modbus client rdi 0 10}).
 *
 * @see ReadCoilsCommand for read/write discrete outputs (function code 01)
 */
@Command(name = "rdi", description = "Read Discrete Inputs")
class ReadDiscreteInputsCommand implements Runnable {

  /**
   * Starting discrete input address. Addressing is typically 0-based, though some devices use
   * 1-based.
   */
  @Parameters(index = "0", description = "starting address")
  int address;

  /** Number of discrete inputs to read, starting from the specified address. */
  @Parameters(index = "1", description = "quantity of discrete inputs")
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
   * Executes the Read Discrete Inputs request and renders the results as a coil table.
   *
   * @param client the connected Modbus TCP client.
   * @param unitId the target unit identifier.
   * @param output the output context for rendering protocol messages and results.
   * @throws ModbusException if the Modbus operation fails.
   */
  private void executeRead(ModbusClient client, int unitId, OutputContext output)
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
