package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 01 (Read Coils) operation.
 *
 * <p>Coils are discrete outputs representing read/write boolean values. They typically correspond
 * to physical outputs like relays or actuators, or internal flags that can be both read and
 * written.
 *
 * <p>This command is invoked using {@code rc} (e.g., {@code modbus client rc 0 10}).
 *
 * @see ReadDiscreteInputsCommand for read-only discrete inputs (function code 02)
 */
@Command(name = "rc", description = "Read Coils")
class ReadCoilsCommand implements Runnable {

  /** Starting coil address. Addressing is typically 0-based, though some devices use 1-based. */
  @Parameters(index = "0", description = "starting address")
  int address;

  /** Number of coils to read, starting from the specified address. */
  @Parameters(index = "1", description = "quantity of coils")
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
   * Executes the Read Coils request and renders the results as a coil table.
   *
   * @param client the connected Modbus TCP client.
   * @param unitId the target unit identifier.
   * @param output the output context for rendering protocol messages and results.
   * @throws ModbusException if the Modbus operation fails.
   */
  private void executeRead(ModbusClient client, int unitId, OutputContext output)
      throws ModbusException {

    var request = new ReadCoilsRequest(address, quantity);

    output.protocol(request, Direction.SENT, null);

    ReadCoilsResponse response = client.readCoils(unitId, request);
    Instant responseTime = Instant.now();

    output.protocol(response, Direction.RECEIVED, responseTime);

    byte[] coils = response.coils();

    output
        .coilTable()
        .data(coils)
        .startAddress(address)
        .quantity(quantity)
        .timestamp(responseTime)
        .render();
  }
}
