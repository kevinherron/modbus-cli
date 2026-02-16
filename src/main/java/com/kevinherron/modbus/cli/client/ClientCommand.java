package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusClientConfig;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.kevinherron.modbus.cli.ModbusCommand;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Abstract parent command for all Modbus TCP client operations.
 *
 * <p>This command provides shared connection parameters (hostname, port, unit-id, timeout) and
 * client lifecycle management for its subcommands. It serves as the entry point for the "client"
 * command group, which includes:
 *
 * <ul>
 *   <li>{@link ReadCoilsCommand} (rc) - Read coils (function code 01)
 *   <li>{@link ReadDiscreteInputsCommand} (rdi) - Read discrete inputs (function code 02)
 *   <li>{@link ReadHoldingRegistersCommand} (rhr) - Read holding registers (function code 03)
 *   <li>{@link ReadInputRegistersCommand} (rir) - Read input registers (function code 04)
 *   <li>{@link WriteSingleCoilCommand} (wsc) - Write single coil (function code 05)
 *   <li>{@link WriteMultipleCoilsCommand} (wmc) - Write multiple coils (function code 15)
 *   <li>{@link WriteSingleRegisterCommand} (wsr) - Write single register (function code 06)
 *   <li>{@link WriteMultipleRegistersCommand} (wmr) - Write multiple registers (function code 16)
 *   <li>{@link MaskWriteRegisterCommand} (mwr) - Mask write register (function code 22)
 *   <li>{@link ReadWriteMultipleRegistersCommand} (rwmr) - Read/write multiple registers (function
 *       code 23)
 *   <li>{@link ScanCommand} (scan) - Scan register ranges
 * </ul>
 */
@Command(
    name = "client",
    subcommands = {
      ReadCoilsCommand.class,
      ReadDiscreteInputsCommand.class,
      ReadHoldingRegistersCommand.class,
      ReadInputRegistersCommand.class,
      WriteSingleCoilCommand.class,
      WriteMultipleCoilsCommand.class,
      WriteSingleRegisterCommand.class,
      WriteMultipleRegistersCommand.class,
      MaskWriteRegisterCommand.class,
      ReadWriteMultipleRegistersCommand.class,
      ScanCommand.class
    })
public class ClientCommand {

  @ParentCommand ModbusCommand parent;

  @Parameters(index = "0", description = "hostname or IP address")
  String hostname;

  @Option(
      names = {"-p", "--port"},
      description = "port number")
  int port = 502;

  @Option(
      names = {"--unit-id"},
      description = "unit/slave id")
  int unitId = 1;

  @Option(
      names = {"-t", "--timeout"},
      description = "request timeout in milliseconds (default: 5000ms)")
  int timeout = 5000;

  /**
   * Creates a new Modbus TCP client configured with the command's connection parameters.
   *
   * <p>The client uses {@link NettyTcpClientTransport} with non-persistent connections, meaning
   * each connect/disconnect cycle establishes a new TCP connection.
   *
   * @return a configured but not yet connected {@link ModbusTcpClient}.
   */
  public ModbusTcpClient createTcpClient() {
    var transport =
        NettyTcpClientTransport.create(
            cfg -> {
              cfg.hostname = hostname;
              cfg.port = port;
              cfg.connectPersistent = false;
            });

    ModbusClientConfig config =
        ModbusClientConfig.create(cfg -> cfg.requestTimeout = Duration.ofMillis(timeout));

    return new ModbusTcpClient(config, transport);
  }

  /**
   * Executes a single Modbus operation with automatic client lifecycle management.
   *
   * <p>This method handles the full client lifecycle: connect, execute the provided command, and
   * disconnect. Any exceptions during execution are passed to {@link #handleException} for
   * appropriate error output based on verbose/quiet mode settings.
   *
   * @param command the Modbus operation to execute.
   */
  public void runWithClient(ClientRunnable command) {
    OutputContext output = parent.createOutputContext();

    output.info("Hostname: %s:%d, Unit ID: %d", hostname, port, unitId);

    ModbusClient client = createTcpClient();
    try {
      client.connect();
      command.run(client, unitId, output);
    } catch (Exception e) {
      handleException(e, output);
    } finally {
      try {
        client.disconnect();
      } catch (ModbusExecutionException ignored) {
      }
    }
  }

  /**
   * Executes a Modbus operation repeatedly with polling support.
   *
   * <p>This method maintains a single connection while executing the command multiple times at the
   * specified interval. The sleep duration between iterations is adjusted to account for the
   * operation's execution time, ensuring consistent polling intervals.
   *
   * <p>Iteration tracking is provided via {@link OutputContext#setIteration(Integer)}, allowing
   * output formatters to include iteration numbers in their output.
   *
   * @param command the Modbus operation to execute on each iteration.
   * @param count the number of iterations to execute; 0 for infinite polling until interrupted.
   * @param intervalMs the target delay in milliseconds between the start of each iteration.
   */
  public void runWithClientPolling(ClientRunnable command, int count, int intervalMs) {
    OutputContext output = parent.createOutputContext();

    output.info("Hostname: %s:%d, Unit ID: %d", hostname, port, unitId);

    ModbusClient client = createTcpClient();
    try {
      client.connect();

      int iteration = 0;
      while (count == 0 || iteration < count) {
        iteration++;
        output.setIteration(iteration);

        long start = System.nanoTime();
        try {
          command.run(client, unitId, output);
        } catch (Exception e) {
          handleException(e, output);
        }

        long duration = System.nanoTime() - start;

        // Sleep between iterations, but not after the last one
        if (count == 0 || iteration < count) {
          Thread.sleep(Math.max(0, intervalMs - Duration.ofNanos(duration).toMillis()));
        }
      }
    } catch (Exception e) {
      handleException(e, output);
    } finally {
      try {
        client.disconnect();
      } catch (ModbusExecutionException ignored) {
      }
    }
  }

  /**
   * Handles exceptions by outputting error information appropriate to the current verbosity level.
   *
   * <p>In verbose mode, the full stack trace is printed. Otherwise, only the exception message is
   * displayed.
   *
   * @param e the exception to handle.
   * @param output the output context for error display.
   */
  private void handleException(Exception e, OutputContext output) {
    if (parent.verbose) {
      var sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      output.error("%s", sw.toString());
    } else {
      output.error("%s", e.getMessage());
    }
  }

  /**
   * Callback interface for Modbus operations executed within the client lifecycle.
   *
   * <p>Implementations perform the actual Modbus read/write operations using the provided client.
   * The client is already connected when this callback is invoked and will be disconnected
   * afterward by the calling method.
   */
  public interface ClientRunnable {

    /**
     * Executes a Modbus operation.
     *
     * @param client the connected Modbus client.
     * @param unitId the unit/slave identifier for the request.
     * @param output the output context for displaying results.
     * @throws ModbusException if the Modbus operation fails.
     */
    void run(ModbusClient client, int unitId, OutputContext output) throws ModbusException;
  }
}
