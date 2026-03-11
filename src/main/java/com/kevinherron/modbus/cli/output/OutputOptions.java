package com.kevinherron.modbus.cli.output;

/**
 * Configuration options for output formatting.
 *
 * @param format the output format to use.
 * @param verbose whether to show verbose output.
 * @param quiet whether to show minimal output.
 * @param colorsEnabled whether ANSI colors should be used.
 * @param emitMode controls which kinds of records are emitted.
 */
public record OutputOptions(
    OutputFormat format, boolean verbose, boolean quiet, boolean colorsEnabled, EmitMode emitMode) {

  /** Default output options: human format, normal verbosity, colors enabled, emit all. */
  public static OutputOptions defaults() {
    return new OutputOptions(OutputFormat.HUMAN, false, false, true, EmitMode.ALL);
  }

  /** Creates options for human-readable output with specified verbosity. */
  public static OutputOptions human(boolean verbose, boolean quiet, boolean colorsEnabled) {
    return new OutputOptions(OutputFormat.HUMAN, verbose, quiet, colorsEnabled, EmitMode.ALL);
  }

  /** Creates options for JSON output. */
  public static OutputOptions json() {
    return new OutputOptions(OutputFormat.JSON, false, false, false, EmitMode.ALL);
  }
}
