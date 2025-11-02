package com.kevinherron.modbus.cli.client;

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

  public ModbusTcpClient createClient() {
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

  public void runWithClient(ClientRunnable command) {
    OutputContext output = parent.createOutputContext();

    output.info("Hostname: %s:%d, Unit ID: %d", hostname, port, unitId);

    ModbusTcpClient client = createClient();
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

  public void runWithClientPolling(ClientRunnable command, int count, int intervalMs) {
    OutputContext output = parent.createOutputContext();

    output.info("Hostname: %s:%d, Unit ID: %d", hostname, port, unitId);

    ModbusTcpClient client = createClient();
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

  private void handleException(Exception e, OutputContext output) {
    if (parent.verbose) {
      var sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      output.error("%s", sw.toString());
    } else {
      output.error("%s", e.getMessage());
    }
  }

  public interface ClientRunnable {
    void run(ModbusTcpClient client, int unitId, OutputContext output) throws ModbusException;
  }
}
