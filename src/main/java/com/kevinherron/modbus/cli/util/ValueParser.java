package com.kevinherron.modbus.cli.util;

/** Utility class for parsing command-line values into Modbus data types. */
public final class ValueParser {

  private ValueParser() {
    // Utility class - prevent instantiation
  }

  /**
   * Parses a coil value from a string.
   *
   * @param value the string value to parse (true/false, 1/0, on/off)
   * @return the parsed boolean value
   * @throws IllegalArgumentException if the value cannot be parsed
   */
  public static boolean parseCoilValue(String value) {
    String normalized = value.toLowerCase().trim();
    return switch (normalized) {
      case "true", "1", "on" -> true;
      case "false", "0", "off" -> false;
      default ->
          throw new IllegalArgumentException(
              "Invalid coil value: '%s'. Use true/false, 1/0, or on/off".formatted(value));
    };
  }

  /**
   * Parses a register value from a string, supporting both decimal and hexadecimal formats.
   *
   * @param value the string value to parse (decimal or hex with 0x prefix)
   * @return the parsed integer value
   * @throws IllegalArgumentException if the value cannot be parsed
   */
  public static int parseRegisterValue(String value) {
    try {
      String normalized = value.trim();
      // Check if hex format (starts with 0x or 0X)
      if (normalized.toLowerCase().startsWith("0x")) {
        return Integer.parseInt(normalized.substring(2), 16);
      } else {
        // Try decimal first
        return Integer.parseInt(normalized);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid value: '%s'. Use decimal (e.g., 1234) or hex (e.g., 0x04D2)".formatted(value),
          e);
    }
  }

  /**
   * Parses a hexadecimal value from a string, with or without 0x prefix.
   *
   * @param value the string value to parse (hex with optional 0x prefix)
   * @return the parsed integer value
   * @throws IllegalArgumentException if the value cannot be parsed
   */
  public static int parseHexValue(String value) {
    try {
      String normalized = value.trim();
      // Remove "0x" or "0X" prefix if present
      if (normalized.toLowerCase().startsWith("0x")) {
        normalized = normalized.substring(2);
      }
      return Integer.parseInt(normalized, 16);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid hex value: '%s'. Use format: 0xFFFF or FFFF".formatted(value), e);
    }
  }
}
