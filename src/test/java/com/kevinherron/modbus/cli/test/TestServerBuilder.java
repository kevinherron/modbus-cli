package com.kevinherron.modbus.cli.test;

import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * A builder for creating test Modbus TCP servers with configurable ports and process images.
 *
 * <p>This class supports both manual lifecycle control (via {@link #start()} and {@link #stop()})
 * and automatic lifecycle management via try-with-resources (implements {@link AutoCloseable}).
 *
 * <p>By default, a random available port in the range 10000-65535 is selected. You can specify a
 * port explicitly using {@link #withPort(int)}, or use port 0 to let the OS assign an available
 * port.
 *
 * <p>Example usage with try-with-resources:
 *
 * <pre>{@code
 * try (var server = new TestServerBuilder().build()) {
 *   server.start();
 *   int port = server.getPort();
 *   // use the server in tests
 * }
 * }</pre>
 *
 * <p>Example usage with manual control:
 *
 * <pre>{@code
 * var server = new TestServerBuilder().withPort(15502).build();
 * server.start();
 * try {
 *   // use the server in tests
 * } finally {
 *   server.stop();
 * }
 * }</pre>
 */
public class TestServerBuilder implements AutoCloseable {

  private static final Random RANDOM = new Random();
  private static final int MIN_PORT = 10000;
  private static final int MAX_PORT = 65535;

  private String bindAddress = "127.0.0.1";
  private Integer port;
  private ProcessImage processImage;
  private boolean separateUnits = false;
  private ModbusTcpServer server;
  private int actualPort = -1;
  private ConcurrentHashMap<Integer, ProcessImage> unitProcessImages;

  /**
   * Set the bind address for the server.
   *
   * @param bindAddress the address to bind to (default: 127.0.0.1).
   * @return this builder.
   */
  public TestServerBuilder withBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
    return this;
  }

  /**
   * Set the port for the server.
   *
   * <p>If not set, a random available port in the range 10000-65535 will be selected.
   *
   * <p>Use port 0 to let the OS assign an available port.
   *
   * @param port the port to listen on.
   * @return this builder.
   */
  public TestServerBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Set a custom ProcessImage for the server.
   *
   * <p>If not set, a {@link TestProcessImage} with default initialization (holding registers set to
   * their addresses) will be used.
   *
   * <p>This option is mutually exclusive with {@link #withSeparateUnits(boolean)}.
   *
   * @param processImage the process image to use.
   * @return this builder.
   */
  public TestServerBuilder withProcessImage(ProcessImage processImage) {
    this.processImage = processImage;
    return this;
  }

  /**
   * Enable separate ProcessImage per unit ID.
   *
   * <p>When enabled, each Modbus unit/slave ID will have its own isolated ProcessImage.
   * ProcessImages are created lazily on first access and initialized with default values (holding
   * registers set to their addresses).
   *
   * <p>This option is mutually exclusive with {@link #withProcessImage(ProcessImage)}.
   *
   * @param separateUnits true to enable separate ProcessImages per unit ID.
   * @return this builder.
   */
  public TestServerBuilder withSeparateUnits(boolean separateUnits) {
    this.separateUnits = separateUnits;
    return this;
  }

  /**
   * Build and return this TestServerBuilder instance.
   *
   * <p>This method is provided for fluent API consistency but doesn't actually construct a new
   * object.
   *
   * @return this builder.
   */
  public TestServerBuilder build() {
    return this;
  }

  /**
   * Start the Modbus TCP server.
   *
   * <p>This method will block until the server is fully started. After starting, you can retrieve
   * the actual port the server is listening on via {@link #getPort()}.
   *
   * @throws ExecutionException if the server fails to start.
   */
  public void start() throws ExecutionException {
    if (server != null) {
      throw new IllegalStateException("Server already started");
    }

    if (separateUnits) {
      unitProcessImages = new ConcurrentHashMap<>();
    } else if (processImage == null) {
      processImage = createDefaultProcessImage();
    }

    int serverPort = port != null ? port : findAvailablePort();

    NettyTcpServerTransport transport =
        NettyTcpServerTransport.create(
            cfg -> {
              cfg.bindAddress = bindAddress;
              cfg.port = serverPort;
            });

    var services =
        new ReadWriteModbusServices() {
          @Override
          protected Optional<ProcessImage> getProcessImage(int unitId) {
            if (separateUnits) {
              return Optional.of(
                  unitProcessImages.computeIfAbsent(unitId, id -> createDefaultProcessImage()));
            }
            return Optional.of(processImage);
          }
        };

    server = ModbusTcpServer.create(transport, services);

    try {
      server.start();
      actualPort = serverPort;
    } catch (ExecutionException | InterruptedException e) {
      server = null;
      actualPort = -1;
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new ExecutionException("Failed to start server", e);
    }
  }

  /**
   * Stop the Modbus TCP server.
   *
   * <p>This method will block until the server is fully stopped.
   */
  public void stop() {
    if (server != null) {
      try {
        server.stop();
      } catch (ExecutionException | InterruptedException e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new RuntimeException("Failed to stop server", e);
      } finally {
        server = null;
        actualPort = -1;
      }
    }
  }

  /**
   * Get the port the server is listening on.
   *
   * @return the port number, or -1 if the server has not been started.
   */
  public int getPort() {
    return actualPort;
  }

  /**
   * Get the bind address the server is listening on.
   *
   * @return the bind address.
   */
  public String getBindAddress() {
    return bindAddress;
  }

  /**
   * Get the ProcessImage being used by the server (shared mode only).
   *
   * <p>This allows tests to modify the ProcessImage after the server has started.
   *
   * @return the ProcessImage, or null if the server has not been started or is in separate units
   *     mode.
   */
  public ProcessImage getProcessImage() {
    return processImage;
  }

  /**
   * Get the ProcessImage for a specific unit ID (separate units mode only).
   *
   * <p>This allows tests to access and verify the ProcessImage for a specific unit.
   *
   * @param unitId the unit ID to get the ProcessImage for.
   * @return the ProcessImage for the given unit, or null if not in separate units mode or the unit
   *     has not been accessed yet.
   */
  public ProcessImage getProcessImage(int unitId) {
    if (unitProcessImages == null) {
      return null;
    }
    return unitProcessImages.get(unitId);
  }

  /**
   * Check if the server is currently running.
   *
   * @return true if the server is running, false otherwise.
   */
  public boolean isRunning() {
    return server != null;
  }

  @Override
  public void close() {
    stop();
  }

  private static ProcessImage createDefaultProcessImage() {
    var processImage = new TestProcessImage();
    processImage.with(
        tx ->
            tx.writeHoldingRegisters(
                registerMap -> {
                  for (int i = 0; i < 65536; i++) {
                    byte[] bs = new byte[2];
                    bs[0] = (byte) ((i >> 8) & 0xFF);
                    bs[1] = (byte) (i & 0xFF);
                    registerMap.put(i, bs);
                  }
                }));
    return processImage;
  }

  private static int findAvailablePort() {
    for (int attempt = 0; attempt < 100; attempt++) {
      int port = RANDOM.nextInt(MAX_PORT - MIN_PORT + 1) + MIN_PORT;
      if (isPortAvailable(port)) {
        return port;
      }
    }
    throw new RuntimeException("Failed to find an available port after 100 attempts");
  }

  private static boolean isPortAvailable(int port) {
    try (ServerSocket socket = new ServerSocket(port)) {
      socket.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
