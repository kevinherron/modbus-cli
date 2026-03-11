package com.kevinherron.modbus.cli;

import com.digitalpetri.modbus.exceptions.ModbusConnectException;
import com.digitalpetri.modbus.exceptions.ModbusCrcException;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public class Modbus {

  private static final CommandLine.IExitCodeExceptionMapper EXIT_CODE_EXCEPTION_MAPPER =
      Modbus::mapExitCode;

  public static CommandLine createCommandLine(Object command) {
    var commandLine = new CommandLine(command);

    commandLine.setExitCodeExceptionMapper(EXIT_CODE_EXCEPTION_MAPPER);
    commandLine.setExecutionExceptionHandler(
        (exception, cmd, parseResult) -> {
          if (!(exception instanceof ReportedCliException)
              && exception.getMessage() != null
              && !exception.getMessage().isBlank()) {
            cmd.getErr().println(exception.getMessage());
          }

          return EXIT_CODE_EXCEPTION_MAPPER.getExitCode(exception);
        });

    return commandLine;
  }

  static int mapExitCode(Throwable throwable) {
    Integer fallbackExitCode = null;

    for (Throwable error = throwable; error != null; error = error.getCause()) {
      Integer exitCode = mapDirectExitCode(error);

      if (exitCode == null) {
        continue;
      }

      if (exitCode == 1) {
        fallbackExitCode = 1;
        continue;
      }

      if (exitCode == 10) {
        fallbackExitCode = 10;
        continue;
      }

      return exitCode;
    }

    return fallbackExitCode != null ? fallbackExitCode : 10;
  }

  private static Integer mapDirectExitCode(Throwable error) {
    return switch (error) {
      case ParameterException ignored -> 2;
      case IllegalArgumentException ignored -> 2;
      case ModbusConnectException ignored -> 3;
      case ConnectException ignored -> 3;
      case NoRouteToHostException ignored -> 3;
      case UnknownHostException ignored -> 3;
      case ModbusResponseException ignored -> 4;
      case ModbusCrcException ignored -> 4;
      case InterruptedException ignored -> 5;
      case ModbusTimeoutException ignored -> 5;
      case SocketTimeoutException ignored -> 5;
      case TimeoutException ignored -> 5;
      case ModbusException ignored -> 1;
      default -> null;
    };
  }

  static void main(String[] args) {
    AnsiConsole.systemInstall();

    try {
      var cmd = createCommandLine(new ModbusCommand());

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
