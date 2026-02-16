package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.client.ModbusClientConfig;
import com.digitalpetri.modbus.client.ModbusRtuClient;
import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.serial.client.SerialPortClientTransport;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.fazecast.jSerialComm.SerialPort;
import com.kevinherron.modbus.cli.ModbusCommand;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.EndpointParser;
import com.kevinherron.modbus.cli.util.EndpointParser.Endpoint;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Locale;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Abstract parent command for all Modbus client operations.
 *
 * <p>This command provides shared connection parameters for TCP and RTU transports, plus client
 * lifecycle management for its subcommands. It serves as the entry point for the "client" command
 * group, which includes:
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

  @Parameters(
      index = "0",
      description =
          "endpoint (hostname, tcp:hostname[:port], tcp://hostname[:port], rtu:/dev/ttyUSB0, rtu:COM3)")
  String endpoint;

  @Option(
      names = {"-p", "--port"},
      description = "TCP port number (default: 502)")
  Integer port;

  @Option(
      names = {"--unit-id"},
      description = "unit/slave id")
  int unitId = 1;

  @Option(
      names = {"-t", "--timeout"},
      description = "request timeout in milliseconds (default: 5000ms)")
  int timeout = 5000;

  @Option(
      names = {"--baud"},
      description = "serial baud rate (default: 9600)")
  int baudRate = 9600;

  @Option(
      names = {"--data-bits"},
      description = "serial data bits (5, 6, 7, 8) (default: 8)")
  int dataBits = 8;

  @Option(
      names = {"--parity"},
      description = "serial parity (N, E, O) (default: N)")
  String parity = "N";

  @Option(
      names = {"--stop-bits"},
      description = "serial stop bits (1, 2) (default: 1)")
  int stopBits = 1;

  @Option(
      names = {"--rs485"},
      description = "enable RS-485 mode")
  boolean rs485;

  @Option(
      names = {"--rs485-rts-high"},
      description = "RS-485 RTS active high (default: false)")
  boolean rs485RtsActiveHigh;

  @Option(
      names = {"--rs485-termination"},
      description = "enable RS-485 bus termination (default: false)")
  boolean rs485Termination;

  @Option(
      names = {"--rs485-rx-during-tx"},
      description = "enable receiving during transmission (default: false)")
  boolean rs485RxDuringTx;

  @Option(
      names = {"--rs485-delay-before"},
      description = "RS-485 delay before send in microseconds (default: 0)")
  int rs485DelayBefore;

  @Option(
      names = {"--rs485-delay-after"},
      description = "RS-485 delay after send in microseconds (default: 0)")
  int rs485DelayAfter;

  /**
   * Creates a new Modbus TCP client configured with the resolved connection parameters.
   *
   * <p>The client uses {@link NettyTcpClientTransport} with non-persistent connections, meaning
   * each connect/disconnect cycle establishes a new TCP connection.
   *
   * @return a configured but not yet connected {@link ModbusTcpClient}.
   */
  public ModbusTcpClient createTcpClient(String hostname, int tcpPort) {
    var transport =
        NettyTcpClientTransport.create(
            cfg -> {
              cfg.hostname = hostname;
              cfg.port = tcpPort;
              cfg.connectPersistent = false;
            });

    ModbusClientConfig config =
        ModbusClientConfig.create(cfg -> cfg.requestTimeout = Duration.ofMillis(timeout));

    return new ModbusTcpClient(config, transport);
  }

  public ModbusRtuClient createRtuClient(String serialPort) {
    int resolvedDataBits = resolveDataBits();
    int resolvedStopBits = resolveStopBits();
    int resolvedParity = resolveParity();

    var transport =
        SerialPortClientTransport.create(
            cfg -> {
              cfg.serialPort = serialPort;
              cfg.baudRate = baudRate;
              cfg.dataBits = resolvedDataBits;
              cfg.stopBits = resolvedStopBits;
              cfg.parity = resolvedParity;
            });

    if (rs485) {
      transport
          .getSerialPort()
          .setRs485ModeParameters(
              true,
              rs485RtsActiveHigh,
              rs485Termination,
              rs485RxDuringTx,
              rs485DelayBefore,
              rs485DelayAfter);
    }

    ModbusClientConfig config =
        ModbusClientConfig.create(cfg -> cfg.requestTimeout = Duration.ofMillis(timeout));

    return new ModbusRtuClient(config, transport);
  }

  public ModbusClient createClient(Endpoint resolvedEndpoint) {
    return switch (resolvedEndpoint) {
      case Endpoint.Tcp tcp -> createTcpClient(tcp.hostname(), tcp.port());
      case Endpoint.Rtu rtu -> createRtuClient(rtu.serialPort());
    };
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
    executeWithClient((client, output) -> command.run(client, unitId, output));
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
    executeWithClient(
        (client, output) -> {
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
        });
  }

  /**
   * Handles client setup, connection lifecycle, and error handling for all Modbus operations.
   *
   * <p>This method consolidates the shared logic: creating the output context, parsing the
   * endpoint, creating and connecting the client, and ensuring proper disconnection. The provided
   * action is invoked with the connected client and output context.
   *
   * @param action the operation to execute with the connected client.
   */
  private void executeWithClient(ClientAction action) {
    OutputContext output = parent.createOutputContext();

    Endpoint resolvedEndpoint;
    try {
      resolvedEndpoint = EndpointParser.parse(endpoint, port);
    } catch (Exception e) {
      handleException(e, output);
      return;
    }

    ModbusClient client;
    try {
      client = createClient(resolvedEndpoint);
    } catch (Exception e) {
      handleException(e, output);
      return;
    }

    outputEndpointInfo(output, resolvedEndpoint);
    try {
      client.connect();
      action.execute(client, output);
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
   * Internal callback for operations executed within the client lifecycle managed by {@link
   * #executeWithClient(ClientAction)}.
   */
  private interface ClientAction {

    /**
     * Executes an operation with a connected client.
     *
     * @param client the connected Modbus client.
     * @param output the output context for displaying results.
     * @throws Exception if the operation fails.
     */
    void execute(ModbusClient client, OutputContext output) throws Exception;
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

  private void outputEndpointInfo(OutputContext output, Endpoint resolvedEndpoint) {
    switch (resolvedEndpoint) {
      case Endpoint.Tcp tcp ->
          output.info("Hostname: %s:%d, Unit ID: %d", tcp.hostname(), tcp.port(), unitId);
      case Endpoint.Rtu rtu -> {
        if (rs485) {
          output.info("Serial Port: %s, Unit ID: %d, RS-485 mode", rtu.serialPort(), unitId);
        } else {
          output.info("Serial Port: %s, Unit ID: %d", rtu.serialPort(), unitId);
        }
      }
    }
  }

  private int resolveDataBits() {
    if (dataBits < 5 || dataBits > 8) {
      throw new IllegalArgumentException("data bits must be 5, 6, 7, or 8");
    }
    return dataBits;
  }

  private int resolveStopBits() {
    return switch (stopBits) {
      case 1 -> SerialPort.ONE_STOP_BIT;
      case 2 -> SerialPort.TWO_STOP_BITS;
      default -> throw new IllegalArgumentException("stop bits must be 1 or 2");
    };
  }

  private int resolveParity() {
    String normalized = parity.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "N" -> SerialPort.NO_PARITY;
      case "E" -> SerialPort.EVEN_PARITY;
      case "O" -> SerialPort.ODD_PARITY;
      default -> throw new IllegalArgumentException("parity must be N, E, or O");
    };
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
