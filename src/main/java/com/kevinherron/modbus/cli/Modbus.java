package com.kevinherron.modbus.cli;

import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

public class Modbus {

  static void main(String[] args) {
    AnsiConsole.systemInstall();

    try {
      var cmd = new CommandLine(new ModbusCommand());

      if (args.length == 0) {
        cmd.usage(System.out);
      } else {
        int result = cmd.execute(args);

        System.exit(result);
      }
    } finally {
      AnsiConsole.systemUninstall();
    }
  }
}
