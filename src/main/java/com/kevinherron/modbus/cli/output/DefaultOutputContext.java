package com.kevinherron.modbus.cli.output;

import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.kevinherron.modbus.cli.client.ScanCommand.ScanResult;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Default implementation of OutputContext that delegates to an OutputFormatter. */
public record DefaultOutputContext(
    OutputFormatter formatter, OutputOptions options, PrintStream stdout, PrintStream stderr)
    implements OutputContext {

  public DefaultOutputContext(OutputFormatter formatter, OutputOptions options) {
    this(formatter, options, System.out, System.err);
  }

  @Override
  public void setIteration(Integer iteration) {
    formatter.setIteration(iteration);
  }

  @Override
  public void protocol(ModbusPdu pdu, Direction direction, @Nullable Instant timestamp) {
    formatter.formatProtocol(stdout, pdu, direction, timestamp, options);
  }

  @Override
  public void info(String format, Object... args) {
    String message = String.format(format, args);
    formatter.formatMessage(stdout, OutputType.INFO, message, options);
  }

  @Override
  public void success(String format, Object... args) {
    String message = String.format(format, args);
    formatter.formatMessage(stdout, OutputType.SUCCESS, message, options);
  }

  @Override
  public void warning(String format, Object... args) {
    String message = String.format(format, args);
    formatter.formatMessage(stderr, OutputType.WARNING, message, options);
  }

  @Override
  public void error(String format, Object... args) {
    String message = String.format(format, args);
    formatter.formatMessage(stderr, OutputType.ERROR, message, options);
  }

  @Override
  public RegisterTableBuilder registerTable() {
    return new RegisterTableBuilderImpl();
  }

  @Override
  public CoilTableBuilder coilTable() {
    return new CoilTableBuilderImpl();
  }

  @Override
  public ScanResultsBuilder scanResults() {
    return new ScanResultsBuilderImpl();
  }

  private class RegisterTableBuilderImpl implements RegisterTableBuilder {
    private byte[] registers;
    private int startAddress;
    private @Nullable Instant timestamp;

    @Override
    public RegisterTableBuilder data(byte[] registers) {
      this.registers = registers;
      return this;
    }

    @Override
    public RegisterTableBuilder startAddress(int address) {
      this.startAddress = address;
      return this;
    }

    @Override
    public RegisterTableBuilder timestamp(@Nullable Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    @Override
    public void render() {
      formatter.formatRegisterTable(stdout, registers, startAddress, timestamp, options);
    }
  }

  private class CoilTableBuilderImpl implements CoilTableBuilder {
    private byte[] coilBytes;
    private int startAddress;
    private int quantity;
    private @Nullable Instant timestamp;

    @Override
    public CoilTableBuilder data(byte[] coilBytes) {
      this.coilBytes = coilBytes;
      return this;
    }

    @Override
    public CoilTableBuilder startAddress(int address) {
      this.startAddress = address;
      return this;
    }

    @Override
    public CoilTableBuilder quantity(int quantity) {
      this.quantity = quantity;
      return this;
    }

    @Override
    public CoilTableBuilder timestamp(@Nullable Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    @Override
    public void render() {
      formatter.formatCoilTable(stdout, coilBytes, startAddress, quantity, timestamp, options);
    }
  }

  private class ScanResultsBuilderImpl implements ScanResultsBuilder {
    private List<ScanResult> results;

    @Override
    public ScanResultsBuilder results(List<ScanResult> results) {
      this.results = results;
      return this;
    }

    @Override
    public void render() {
      formatter.formatScanResults(stdout, results, options);
    }
  }
}
