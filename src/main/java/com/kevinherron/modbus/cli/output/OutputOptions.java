package com.kevinherron.modbus.cli.output;

/**
 * Configuration options for output formatting.
 *
 * @param json whether to use JSON output format.
 * @param verbose whether to show verbose output.
 * @param colorsEnabled whether ANSI colors should be used.
 * @param emitMode controls which kinds of records are emitted.
 */
public record OutputOptions(
    boolean json, boolean verbose, boolean colorsEnabled, EmitMode emitMode) {

  /** Default output options: human format, normal verbosity, colors enabled, emit all. */
  public static OutputOptions defaults() {
    return new OutputOptions(false, false, true, EmitMode.ALL);
  }

  /** Creates options for human-readable output with specified verbosity. */
  public static OutputOptions human(boolean verbose, boolean colorsEnabled) {
    return new OutputOptions(false, verbose, colorsEnabled, EmitMode.ALL);
  }

  /** Creates options for JSON output. */
  public static OutputOptions jsonDefaults() {
    return new OutputOptions(true, false, false, EmitMode.DATA);
  }
}
