package com.kevinherron.modbus.cli.util;

import java.net.URI;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Parses Modbus endpoint strings into resolved {@link Endpoint} instances.
 *
 * <p>
 * Supported endpoint formats:
 *
 * <ul>
 * <li>{@code hostname} — bare hostname, TCP with default port
 * <li>{@code tcp:hostname[:port]} — TCP with optional port
 * <li>{@code tcp://hostname[:port]} — TCP URI format
 * <li>{@code rtu:/dev/ttyUSB0} — RTU with serial port path
 * <li>{@code rtu:COM3} — RTU with serial port name
 * <li>{@code rtu:///dev/ttyUSB0} — RTU URI format
 * </ul>
 */
public final class EndpointParser {

  /** The default Modbus TCP port. */
  public static final int DEFAULT_TCP_PORT = 502;

  private EndpointParser() {
    // Utility class - prevent instantiation
  }

  /**
   * Parses a raw endpoint string and resolves it into an {@link Endpoint}.
   *
   * <p>
   * The {@code portOverride} parameter corresponds to the {@code --port} CLI
   * flag. When
   * provided, it overrides the default TCP port. If both the endpoint string and
   * {@code
   * portOverride} specify a port and they disagree, an
   * {@link IllegalArgumentException} is thrown.
   *
   * @param rawEndpoint  the raw endpoint string to parse.
   * @param portOverride an optional port override from the {@code --port} CLI
   *                     flag.
   * @return the resolved endpoint.
   * @throws IllegalArgumentException if the endpoint is invalid or has
   *                                  conflicting port values.
   */
  public static Endpoint parse(String rawEndpoint, @Nullable Integer portOverride) {
    if (rawEndpoint == null || rawEndpoint.isBlank()) {
      throw new IllegalArgumentException("endpoint must not be blank");
    }

    String trimmed = rawEndpoint.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);

    if (lower.startsWith("tcp://")) {
      return resolveTcp(parseTcpUri(trimmed), portOverride);
    }
    if (lower.startsWith("rtu://")) {
      return resolveRtu(parseRtuUri(trimmed), portOverride);
    }
    if (lower.startsWith("tcp:")) {
      return resolveTcp(parseTcpScheme(trimmed.substring("tcp:".length())), portOverride);
    }
    if (lower.startsWith("rtu:")) {
      return resolveRtu(parseRtuScheme(trimmed.substring("rtu:".length())), portOverride);
    }
    if (lower.contains("://")) {
      throw new IllegalArgumentException(
          "unsupported endpoint scheme (expected tcp or rtu): %s".formatted(trimmed));
    }

    return resolveTcp(new ParsedTcp(trimmed, null), portOverride);
  }

  /**
   * Resolves a parsed TCP endpoint and an optional port override into a concrete
   * {@link Endpoint.Tcp} instance.
   *
   * @param parsed       the parsed TCP endpoint.
   * @param portOverride an optional port override from the CLI flag.
   * @return the resolved TCP endpoint.
   * @throws IllegalArgumentException if there's a mismatch between the parsed and
   *                                  overridden ports.
   */
  private static Endpoint.Tcp resolveTcp(ParsedTcp parsed, @Nullable Integer portOverride) {
    int resolvedPort = DEFAULT_TCP_PORT;

    if (parsed.port() != null) {
      if (portOverride != null && !portOverride.equals(parsed.port())) {
        throw new IllegalArgumentException(
            "TCP port mismatch: endpoint specifies %d but --port specifies %d"
                .formatted(parsed.port(), portOverride));
      }
      resolvedPort = parsed.port();
    } else if (portOverride != null) {
      resolvedPort = portOverride;
    }

    return new Endpoint.Tcp(parsed.hostname(), resolvedPort);
  }

  /**
   * Parses an RTU serial port endpoint and resolves it into an
   * {@link Endpoint.Rtu}.
   *
   * <p>
   * The {@code portOverride} parameter should be null for RTU endpoints.
   *
   * @param serialPort   the serial port to parse.
   * @param portOverride an optional port override, which must be null for RTU
   *                     endpoints.
   * @return the resolved RTU endpoint.
   * @throws IllegalArgumentException if {@code portOverride} is not null.
   */
  private static Endpoint.Rtu resolveRtu(String serialPort, @Nullable Integer portOverride) {
    if (portOverride != null) {
      throw new IllegalArgumentException("--port is only valid for TCP endpoints");
    }
    return new Endpoint.Rtu(serialPort);
  }

  /**
   * Parses a TCP endpoint URI and returns its components.
   *
   * @param endpoint The TCP endpoint URI to parse.
   * @return A ParsedTcp object containing the hostname and port (if present).
   * @throws IllegalArgumentException if the endpoint is invalid or contains
   *                                  unsupported elements.
   */
  private static ParsedTcp parseTcpUri(String endpoint) {
    URI uri;
    try {
      uri = URI.create(endpoint);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("invalid tcp endpoint: %s".formatted(e.getMessage()), e);
    }

    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("tcp endpoint must include a hostname");
    }
    // URI.getHost() may return brackets around IPv6 addresses (e.g. "[::1]")
    if (host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }

    String path = uri.getRawPath();
    if (path != null && !path.isEmpty() && !"/".equals(path)) {
      throw new IllegalArgumentException("tcp endpoint must not include a path");
    }

    @Nullable
    Integer endpointPort = uri.getPort() >= 0 ? uri.getPort() : null;
    return new ParsedTcp(host, endpointPort);
  }

  /**
   * Parses the RTU URI endpoint and extracts the serial port.
   *
   * @param endpoint the RTU URI string to parse.
   * @return the resolved serial port path.
   * @throws IllegalArgumentException if the endpoint is invalid or does not
   *                                  contain a serial port.
   */
  private static String parseRtuUri(String endpoint) {
    String serialPort = endpoint.substring("rtu://".length());
    if (serialPort.isBlank()) {
      throw new IllegalArgumentException("rtu endpoint must include a serial port");
    }
    return serialPort;
  }

  /**
   * Parses TCP endpoints in the format "hostname[:port]" or "tcp:hostname[:port]"
   * into a {@link ParsedTcp} object.
   *
   * @param endpoint The raw TCP endpoint string to parse.
   * @return A {@link ParsedTcp} object representing the parsed hostname and port.
   * @throws IllegalArgumentException If the input is not a valid TCP endpoint in
   *                                  the expected format.
   */
  private static ParsedTcp parseTcpScheme(String endpoint) {
    if (endpoint.isBlank()) {
      throw new IllegalArgumentException("tcp endpoint must include a hostname");
    }
    if (endpoint.startsWith("/")) {
      throw new IllegalArgumentException("tcp endpoint must not start with '/'");
    }

    return parseHostPort(endpoint);
  }

  /**
   * Parses RTU endpoints in the format "/dev/ttyUSB0" or "COM3" into a serial
   * port string.
   * 
   * @param endpoint The raw RTU endpoint string to parse.
   * @return A string representing the parsed serial port.
   * @throws IllegalArgumentException If the input is not a valid RTU endpoint in
   *                                  the expected format.
   */
  private static String parseRtuScheme(String endpoint) {
    if (endpoint.isBlank()) {
      throw new IllegalArgumentException("rtu endpoint must include a serial port");
    }
    return endpoint;
  }

  /**
   * Parses host and port from an endpoint string in the format "hostname[:port]".
   *
   * @param endpoint The raw endpoint string to parse.
   * @return A {@link ParsedTcp} object representing the parsed hostname and port.
   * @throws IllegalArgumentException If the input is not a valid endpoint in the
   *                                  expected format.
   */
  private static ParsedTcp parseHostPort(String endpoint) {
    if (endpoint.startsWith("[")) {
      int closing = endpoint.indexOf(']');
      if (closing < 0) {
        throw new IllegalArgumentException("tcp endpoint has an invalid IPv6 host");
      }

      String host = endpoint.substring(1, closing);
      if (host.isBlank()) {
        throw new IllegalArgumentException("tcp endpoint must include a hostname");
      }

      String remainder = endpoint.substring(closing + 1);
      if (remainder.isEmpty()) {
        return new ParsedTcp(host, null);
      }
      if (!remainder.startsWith(":")) {
        throw new IllegalArgumentException("tcp endpoint has unexpected characters after host");
      }

      String portValue = remainder.substring(1);
      if (portValue.isEmpty()) {
        throw new IllegalArgumentException("tcp endpoint port is missing");
      }
      if (!portValue.chars().allMatch(Character::isDigit)) {
        throw new IllegalArgumentException("tcp endpoint port must be numeric");
      }

      return new ParsedTcp(host, Integer.parseInt(portValue));
    }

    int firstColon = endpoint.indexOf(':');
    int lastColon = endpoint.lastIndexOf(':');
    if (firstColon > 0 && firstColon == lastColon && lastColon < endpoint.length() - 1) {
      String portValue = endpoint.substring(lastColon + 1);
      if (portValue.chars().allMatch(Character::isDigit)) {
        String host = endpoint.substring(0, lastColon);
        if (host.isBlank()) {
          throw new IllegalArgumentException("tcp endpoint must include a hostname");
        }
        return new ParsedTcp(host, Integer.parseInt(portValue));
      }
    }

    return new ParsedTcp(endpoint, null);
  }

  /**
   * Intermediate result from parsing a TCP endpoint string, before port
   * resolution. The port may be
   * null if not specified in the endpoint string.
   */
  private record ParsedTcp(String hostname, @Nullable Integer port) {

    ParsedTcp {
      if (hostname == null || hostname.isBlank()) {
        throw new IllegalArgumentException("tcp endpoint must include a hostname");
      }
    }
  }

  /** A resolved Modbus endpoint ready for client creation. */
  public sealed interface Endpoint {

    /**
     * A resolved Modbus TCP endpoint.
     *
     * @param hostname the hostname or IP address.
     * @param port     the TCP port number.
     */
    record Tcp(String hostname, int port) implements Endpoint {
    }

    /**
     * A resolved Modbus RTU endpoint.
     *
     * @param serialPort the serial port path or name.
     */
    record Rtu(String serialPort) implements Endpoint {
    }
  }
}
