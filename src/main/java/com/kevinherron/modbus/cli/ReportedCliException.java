package com.kevinherron.modbus.cli;

public final class ReportedCliException extends RuntimeException {

  public ReportedCliException(Throwable cause) {
    super(cause.getMessage(), cause);
  }
}
