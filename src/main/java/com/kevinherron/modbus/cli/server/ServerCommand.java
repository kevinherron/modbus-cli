package com.kevinherron.modbus.cli.server;

import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.kevinherron.modbus.cli.ModbusCommand;
import com.kevinherron.modbus.cli.output.OutputContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "server")
public class ServerCommand implements Runnable {

  @ParentCommand ModbusCommand parent;

  @Option(
      names = {"-b", "--bind-address"},
      description = "address to bind to")
  String bindAddress = "0.0.0.0";

  @Option(
      names = {"-p", "--port"},
      description = "port to listen on")
  int port = 502;

  @Option(
      names = {"--separate-units"},
      description = "treat each unit ID as a separate device with its own process image")
  boolean separateUnits = false;

  @Override
  public void run() {
    OutputContext output = parent.createOutputContext();

    NettyTcpServerTransport transport =
        NettyTcpServerTransport.create(
            cfg -> {
              cfg.bindAddress = bindAddress;
              cfg.port = port;
            });

    var services =
        new ReadWriteModbusServices() {
          private static final int SHARED_KEY = 0;
          private final ConcurrentMap<Integer, ProcessImage> processImages =
              new ConcurrentHashMap<>();

          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            int key = separateUnits ? unitId : SHARED_KEY;
            return Optional.of(processImages.computeIfAbsent(key, _ -> createProcessImage()));
          }
        };

    var server = ModbusTcpServer.create(transport, services);

    try {
      server.start();

      output.success("Modbus server started on %s:%d", bindAddress, port);

      Thread.sleep(Long.MAX_VALUE);
    } catch (ExecutionException | InterruptedException e) {
      handleException(e, output);
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
