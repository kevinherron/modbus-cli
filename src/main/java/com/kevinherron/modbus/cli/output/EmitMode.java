package com.kevinherron.modbus.cli.output;

/** Controls which kinds of records are emitted in the output stream. */
public enum EmitMode {

  /** Emit everything: results, protocol, log, and errors. */
  ALL,

  /** Emit results, protocol, and errors (no log messages). Default for JSON output. */
  DATA,

  /** Emit only the final business result (and errors). */
  RESULT
}
