package com.kevinherron.modbus.cli;

import com.kevinherron.modbus.cli.client.ClientCommand;
import com.kevinherron.modbus.cli.output.DefaultOutputContext;
import com.kevinherron.modbus.cli.output.EmitMode;
import com.kevinherron.modbus.cli.output.EmitModeConverter;
import com.kevinherron.modbus.cli.output.HumanFormatter;
import com.kevinherron.modbus.cli.output.JsonFormatter;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.output.OutputFormat;
import com.kevinherron.modbus.cli.output.OutputFormatConverter;
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
      names = {"--format"},
      description = "output format: human, json (default: human)",
      converter = OutputFormatConverter.class)
  OutputFormat format = OutputFormat.HUMAN;

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
      description = "control output: all, result, events (default: all)",
      converter = EmitModeConverter.class)
  EmitMode emitMode = EmitMode.ALL;

  /** Creates an OutputContext based on the command-line options. */
  public OutputContext createOutputContext() {
    OutputFormatter formatter =
        switch (format) {
          case HUMAN -> new HumanFormatter();
          case JSON -> new JsonFormatter();
        };

    var options = new OutputOptions(format, verbose, !noColor, emitMode);

    return new DefaultOutputContext(formatter, options);
  }
}
