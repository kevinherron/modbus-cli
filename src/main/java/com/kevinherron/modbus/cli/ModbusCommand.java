package com.kevinherron.modbus.cli;

import com.kevinherron.modbus.cli.client.ClientCommand;
import com.kevinherron.modbus.cli.output.DefaultOutputContext;
import com.kevinherron.modbus.cli.output.EmitMode;
import com.kevinherron.modbus.cli.output.EmitModeConverter;
import com.kevinherron.modbus.cli.output.HumanFormatter;
import com.kevinherron.modbus.cli.output.JsonFormatter;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.output.OutputFormatter;
import com.kevinherron.modbus.cli.output.OutputOptions;
import com.kevinherron.modbus.cli.server.ServerCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "modbus",
    header = "Modbus CLI",
    description = "Command-line interface for Modbus TCP and RTU serial operations.",
    mixinStandardHelpOptions = true,
    versionProvider = ModbusVersionProvider.class,
    subcommands = {ClientCommand.class, ServerCommand.class})
public class ModbusCommand {

  @Option(
      names = {"--json"},
      description = "use JSON output format")
  boolean json = false;

  @Option(
      names = {"-v", "--verbose"},
      description = "verbose mode - detailed output")
  public boolean verbose = false;

  @Option(
      names = {"--no-color"},
      description = "disable ANSI color output")
  boolean noColor = false;

  @Option(
      names = {"--emit"},
      description =
          "control JSON output: all, data, result (default: data). Only applies with --json.",
      converter = EmitModeConverter.class)
  @SuppressWarnings("NullAway")
  EmitMode emitMode = null;

  /** Creates an OutputContext based on the command-line options. */
  public OutputContext createOutputContext() {
    OutputFormatter formatter = json ? new JsonFormatter() : new HumanFormatter();

    EmitMode effectiveEmitMode = emitMode;
    if (effectiveEmitMode == null) {
      effectiveEmitMode = json ? EmitMode.DATA : EmitMode.ALL;
    }

    var options = new OutputOptions(json, verbose, !noColor, effectiveEmitMode);

    return new DefaultOutputContext(formatter, options);
  }
}
