package com.kevinherron.modbus.cli.output;

/**
 * Semantic types for command output.
 *
 * <p>These types allow formatters to apply appropriate styling and routing (e.g., errors to stderr,
 * success in green, warnings in yellow).
 */
public enum OutputType {
  /** Regular data output (register values, coil states, etc.). */
  DATA,

  /** Error messages. */
  ERROR,

  /** Warning messages. */
  WARNING,

  /** Success/confirmation messages. */
  SUCCESS,

  /** Informational messages (connection details, status, etc.). */
  INFO,

  /** Protocol-level request/response messages. */
  PROTOCOL
}
