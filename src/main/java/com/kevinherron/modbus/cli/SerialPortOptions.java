package com.kevinherron.modbus.cli;

import com.fazecast.jSerialComm.SerialPort;
import java.util.Locale;
import picocli.CommandLine.Option;

/**
 * Picocli mixin providing serial port configuration options shared by client and server commands.
 *
 * <p>This mixin defines CLI options for serial communication parameters (baud rate, data bits,
 * parity, stop bits) and RS-485 mode settings. It also provides resolution methods that validate
 * and convert the raw option values into the constants expected by {@link SerialPort}.
 */
public class SerialPortOptions {

  @Option(
      names = {"--baud"},
      description = "serial baud rate (default: 9600)")
  public int baudRate = 9600;

  @Option(
      names = {"--data-bits"},
      description = "serial data bits (5, 6, 7, 8) (default: 8)")
  public int dataBits = 8;

  @Option(
      names = {"--parity"},
      description = "serial parity (N, E, O) (default: N)")
  public String parity = "N";

  @Option(
      names = {"--stop-bits"},
      description = "serial stop bits (1, 2) (default: 1)")
  public int stopBits = 1;

  @Option(
      names = {"--rs485"},
      description = "enable RS-485 mode")
  public boolean rs485;

  @Option(
      names = {"--rs485-rts-high"},
      description = "RS-485 RTS active high (default: false)")
  public boolean rs485RtsActiveHigh;

  @Option(
      names = {"--rs485-termination"},
      description = "enable RS-485 bus termination (default: false)")
  public boolean rs485Termination;

  @Option(
      names = {"--rs485-rx-during-tx"},
      description = "enable receiving during transmission (default: false)")
  public boolean rs485RxDuringTx;

  @Option(
      names = {"--rs485-delay-before"},
      description = "RS-485 delay before send in microseconds (default: 0)")
  public int rs485DelayBefore;

  @Option(
      names = {"--rs485-delay-after"},
      description = "RS-485 delay after send in microseconds (default: 0)")
  public int rs485DelayAfter;

  /**
   * Validates and returns the data bits value.
   *
   * @return the validated data bits value (5-8).
   * @throws IllegalArgumentException if the value is outside the valid range.
   */
  public int resolveDataBits() {
    if (dataBits < 5 || dataBits > 8) {
      throw new IllegalArgumentException("data bits must be 5, 6, 7, or 8");
    }
    return dataBits;
  }

  /**
   * Resolves the stop bits value to the corresponding {@link SerialPort} constant.
   *
   * @return {@link SerialPort#ONE_STOP_BIT} or {@link SerialPort#TWO_STOP_BITS}.
   * @throws IllegalArgumentException if the value is not 1 or 2.
   */
  public int resolveStopBits() {
    return switch (stopBits) {
      case 1 -> SerialPort.ONE_STOP_BIT;
      case 2 -> SerialPort.TWO_STOP_BITS;
      default -> throw new IllegalArgumentException("stop bits must be 1 or 2");
    };
  }

  /**
   * Resolves the parity string to the corresponding {@link SerialPort} constant.
   *
   * @return {@link SerialPort#NO_PARITY}, {@link SerialPort#EVEN_PARITY}, or {@link
   *     SerialPort#ODD_PARITY}.
   * @throws IllegalArgumentException if the value is not N, E, or O.
   */
  public int resolveParity() {
    String normalized = parity.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "N" -> SerialPort.NO_PARITY;
      case "E" -> SerialPort.EVEN_PARITY;
      case "O" -> SerialPort.ODD_PARITY;
      default -> throw new IllegalArgumentException("parity must be N, E, or O");
    };
  }

  /**
   * Configures RS-485 mode on the given serial port if the {@code --rs485} option is enabled.
   *
   * @param serialPort the serial port to configure.
   */
  public void configureRs485(SerialPort serialPort) {
    if (rs485) {
      serialPort.setRs485ModeParameters(
          true,
          rs485RtsActiveHigh,
          rs485Termination,
          rs485RxDuringTx,
          rs485DelayBefore,
          rs485DelayAfter);
    }
  }
}
