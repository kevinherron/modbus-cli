package com.kevinherron.modbus.cli.server;

import com.digitalpetri.modbus.serial.server.SerialPortServerTransport;
import com.digitalpetri.modbus.server.ModbusRtuServer;
import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.kevinherron.modbus.cli.ModbusCommand;
import com.kevinherron.modbus.cli.SerialPortOptions;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.EndpointParser;
import com.kevinherron.modbus.cli.util.EndpointParser.Endpoint;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "server")
public class ServerCommand implements Runnable {

  @ParentCommand ModbusCommand parent;

  @Parameters(
      index = "0",
      defaultValue = "tcp:0.0.0.0",
      description =
          "endpoint (tcp:hostname[:port], tcp://hostname[:port], rtu:/dev/ttyUSB0, rtu:COM3)")
  String endpoint;

  @Option(
      names = {"-p", "--port"},
      description = "TCP port number (default: 502)")
  Integer port;

  @Option(
      names = {"--separate-units"},
      description = "treat each unit ID as a separate device with its own process image")
  boolean separateUnits = false;

  @Mixin SerialPortOptions serialOptions;

  @Override
  public void run() {
    OutputContext output = parent.createOutputContext();

    Endpoint resolvedEndpoint;
    try {
      resolvedEndpoint = EndpointParser.parse(endpoint, port);
    } catch (Exception e) {
      handleException(e, output);
      return;
    }

    var services = createServices();

    try {
      switch (resolvedEndpoint) {
        case Endpoint.Tcp tcp -> runTcpServer(tcp, services, output);
        case Endpoint.Rtu rtu -> runRtuServer(rtu, services, output);
      }
    } catch (ExecutionException | InterruptedException e) {
      handleException(e, output);
    }
  }

  private void runTcpServer(
      Endpoint.Tcp tcp, ReadWriteModbusServices services, OutputContext output)
      throws ExecutionException, InterruptedException {

    NettyTcpServerTransport transport =
        NettyTcpServerTransport.create(
            cfg -> {
              cfg.bindAddress = tcp.hostname();
              cfg.port = tcp.port();
            });

    var server = ModbusTcpServer.create(transport, services);
    server.start();

    output.success("Modbus TCP server started on %s:%d", tcp.hostname(), tcp.port());

    Thread.sleep(Long.MAX_VALUE);
  }

  private void runRtuServer(
      Endpoint.Rtu rtu, ReadWriteModbusServices services, OutputContext output)
      throws ExecutionException, InterruptedException {

    int resolvedDataBits = serialOptions.resolveDataBits();
    int resolvedStopBits = serialOptions.resolveStopBits();
    int resolvedParity = serialOptions.resolveParity();

    var transport =
        SerialPortServerTransport.create(
            cfg -> {
              cfg.serialPort = rtu.serialPort();
              cfg.baudRate = serialOptions.baudRate;
              cfg.dataBits = resolvedDataBits;
              cfg.stopBits = resolvedStopBits;
              cfg.parity = resolvedParity;
            });

    serialOptions.configureRs485(transport.getSerialPort());

    var server = ModbusRtuServer.create(transport, services);
    server.start();

    if (serialOptions.rs485) {
      output.success("Modbus RTU server started on %s (RS-485 mode)", rtu.serialPort());
    } else {
      output.success("Modbus RTU server started on %s", rtu.serialPort());
    }

    Thread.sleep(Long.MAX_VALUE);
  }

  private ReadWriteModbusServices createServices() {
    return new ReadWriteModbusServices() {
      private static final int SHARED_KEY = 0;
      private final ConcurrentMap<Integer, ProcessImage> processImages = new ConcurrentHashMap<>();

      @Override
      protected Optional<ProcessImage> getProcessImage(int unitId) {
        int key = separateUnits ? unitId : SHARED_KEY;
        return Optional.of(processImages.computeIfAbsent(key, _ -> createProcessImage()));
      }
    };
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

  private static ProcessImage createProcessImage() {
    var processImage = new ProcessImage();
    processImage.with(
        tx -> {
          tx.writeHoldingRegisters(ServerCommand::initializeRegisters);
          tx.writeInputRegisters(ServerCommand::initializeRegisters);
          tx.writeCoils(ServerCommand::initializeBooleans);
          tx.writeDiscreteInputs(ServerCommand::initializeBooleans);
        });
    return processImage;
  }

  private static void initializeRegisters(java.util.Map<Integer, byte[]> registerMap) {
    for (int i = 0; i < 65536; i++) {
      byte[] bs = new byte[2];
      bs[0] = (byte) ((i >> 8) & 0xFF);
      bs[1] = (byte) (i & 0xFF);
      registerMap.put(i, bs);
    }
  }

  private static void initializeBooleans(java.util.Map<Integer, Boolean> booleanMap) {
    for (int i = 0; i < 65536; i++) {
      booleanMap.put(i, i % 2 == 0);
    }
  }
}
