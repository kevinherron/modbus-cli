package com.kevinherron.modbus.cli.output;

/** Controls which kinds of records are emitted in the output stream. */
public enum EmitMode {

  /** Emit everything: events, results, and errors. */
  ALL,

  /** Emit only the final business result (and errors). */
  RESULT,

  /** Emit only protocol and lifecycle events. */
  EVENTS
}
