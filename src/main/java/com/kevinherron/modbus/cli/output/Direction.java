package com.kevinherron.modbus.cli.output;

/** Direction of protocol messages (sent to or received from a device). */
public enum Direction {
  /** Message sent to a device. */
  SENT("→"),

  /** Message received from a device. */
  RECEIVED("←");

  private final String arrow;

  Direction(String arrow) {
    this.arrow = arrow;
  }

  public String arrow() {
    return arrow;
  }
}
