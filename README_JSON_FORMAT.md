# JSON Output Format (Schema 2.0)

The modbus-cli tool supports JSON output format via the `--format=json` flag. This format is
designed for machine parsing and automation.

## Usage

```bash
modbus --format=json client <hostname> <command> [args...]
```

## Schema Version 2.0 Envelope

All JSON output is emitted as newline-delimited JSON objects (NDJSON). Every record uses a common
envelope structure:

```json
{
  "schema_version": "2.0",
  "kind": "result | event | error",
  "command": "client.rhr",
  "invocation": {
    "id": "UUID",
    "sequence": 1,
    "iteration": 1
  },
  "timestamp": "ISO8601",
  "data": { ... },
  "error": { ... }
}
```

### Envelope Fields

| Field              | Type   | Description                                                           |
|--------------------|--------|-----------------------------------------------------------------------|
| `schema_version`   | string | Always `"2.0"`.                                                       |
| `kind`             | string | Record type: `"result"`, `"event"`, or `"error"`.                     |
| `command`          | string | The command that produced this record (e.g., `"client.rhr"`).         |
| `invocation.id`    | string | UUID unique to this CLI invocation.                                   |
| `invocation.sequence` | int | Monotonically increasing sequence number within this invocation.      |
| `invocation.iteration` | int | Present when polling with `-c`; the current iteration number.        |
| `timestamp`        | string | ISO 8601 timestamp (e.g., `"2025-11-02T23:07:57.618695Z"`).          |
| `data`             | object | The payload for `"result"` and `"event"` records. Absent on errors.   |
| `error`            | object | The error payload for `"error"` records. Absent on non-errors.        |

### Record Kinds

- **`result`**: Final business data (register tables, coil tables, scan results).
- **`event`**: Protocol and lifecycle events (info messages, PDU sent/received).
- **`error`**: Structured error objects (see below).

## Output Shaping (`--emit`)

The `--emit` option controls which record kinds are emitted:

| Value              | Emits                                    |
|--------------------|------------------------------------------|
| `--emit=all`       | Everything: events, results, and errors. |
| `--emit=result`    | Only results and errors.                 |
| `--emit=events`    | Only events.                             |

Default is `all`. The `--quiet` flag suppresses info events and protocol events.

## Structured Error Objects

Errors in JSON mode are structured objects, not plain strings:

```json
{
  "schema_version": "2.0",
  "kind": "error",
  "command": "client.rhr",
  "invocation": { "id": "...", "sequence": 3 },
  "timestamp": "2025-11-02T23:07:58.123Z",
  "error": {
    "code": "TIMEOUT",
    "category": "timeout",
    "message": "Request timed out after 5000ms",
    "retryable": true
  }
}
```

### Error Fields

| Field       | Type    | Description                                         |
|-------------|---------|-----------------------------------------------------|
| `code`      | string  | Stable symbolic identifier (see table below).       |
| `category`  | string  | `usage`, `connection`, `protocol`, `timeout`, or `internal`. |
| `message`   | string  | Human-readable error summary.                       |
| `retryable` | boolean | Whether the operation may succeed if retried.       |
| `details`   | object  | Optional command-specific context.                  |

### Error Codes

| Code                        | Category     | Retryable | Trigger                              |
|-----------------------------|--------------|-----------|--------------------------------------|
| `USAGE_ERROR`               | `usage`      | no        | Invalid CLI arguments.               |
| `INVALID_ARGUMENT`          | `usage`      | no        | Invalid parameter value.             |
| `CONNECTION_FAILED`         | `connection` | yes       | Cannot connect to Modbus device.     |
| `CONNECTION_REFUSED`        | `connection` | yes       | TCP connection refused.              |
| `NO_ROUTE_TO_HOST`          | `connection` | no        | No route to target host.             |
| `UNKNOWN_HOST`              | `connection` | no        | DNS resolution failed.               |
| `MODBUS_EXCEPTION_RESPONSE` | `protocol`   | no        | Device returned a Modbus exception.  |
| `MODBUS_CRC_ERROR`          | `protocol`   | yes       | CRC mismatch (RTU).                  |
| `MODBUS_ERROR`              | `protocol`   | yes       | Other Modbus protocol error.         |
| `TIMEOUT`                   | `timeout`    | yes       | Request timed out.                   |
| `INTERRUPTED`               | `timeout`    | no        | Operation was interrupted.           |
| `INTERNAL_ERROR`            | `internal`   | no        | Unexpected internal error.           |
| `UNKNOWN_ERROR`             | `internal`   | no        | Unclassified error.                  |

For `MODBUS_EXCEPTION_RESPONSE`, the `details` field contains:

```json
{
  "details": {
    "function_code": 3,
    "exception_code": 2
  }
}
```

## Stream Routing

In JSON mode, **all records** (including errors) are written to `stdout`. Only non-JSON launcher
or runtime failures are written to `stderr`.

## Data Outputs

### Register Table

Output from register read operations (holding registers, input registers).

**Commands:** `rhr` / `read-holding-registers`, `rir` / `read-input-registers`, `rwmr` / `read-write-multiple-registers`

```json
{
  "schema_version": "2.0",
  "kind": "result",
  "command": "client.rhr",
  "invocation": { "id": "...", "sequence": 3 },
  "timestamp": "...",
  "data": {
    "type": "register_table",
    "start_address": 0,
    "quantity": 5,
    "bytes": "00000001000200030004",
    "registers": [0, 1, 2, 3, 4]
  }
}
```

**Data fields:**

- `type`: Always `"register_table"`
- `start_address`: Starting register address
- `quantity`: Number of registers read
- `bytes`: Hex-encoded raw register bytes (2 bytes per register, big-endian)
- `registers`: Array of unsigned 16-bit register values (0-65535)

### Coil Table

Output from coil/discrete input read operations.

**Commands:** `rc` / `read-coils`, `rdi` / `read-discrete-inputs`

```json
{
  "schema_version": "2.0",
  "kind": "result",
  "command": "client.rc",
  "invocation": { "id": "...", "sequence": 3 },
  "timestamp": "...",
  "data": {
    "type": "coil_table",
    "start_address": 0,
    "quantity": 8,
    "bytes": "05",
    "coils": [true, false, true, false, false, false, false, false]
  }
}
```

**Data fields:**

- `type`: Always `"coil_table"`
- `start_address`: Starting coil/discrete input address
- `quantity`: Number of coils/discrete inputs
- `bytes`: Hex-encoded raw coil bytes as received on the wire
- `coils`: Array of boolean values, in LSB-first order per Modbus protocol

### Scan Results

Output from scan operations that read multiple register windows.

**Command:** `scan`

```json
{
  "schema_version": "2.0",
  "kind": "result",
  "command": "client.scan",
  "invocation": { "id": "...", "sequence": 5 },
  "timestamp": "...",
  "data": {
    "type": "scan_results",
    "windows": [
      {
        "start_address": 0,
        "quantity": 10,
        "bytes": "00000001000200030004000500060007000800090",
        "registers": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
      },
      {
        "start_address": 10,
        "quantity": 10,
        "bytes": "000a000b000c000d000e000f00100011001200130",
        "registers": [10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
      }
    ]
  }
}
```

**Data fields:**

- `type`: Always `"scan_results"`
- `windows`: Array of scan window objects, one per sliding window read
    - `start_address`: Starting register address of this window
    - `quantity`: Number of registers in this window
    - `bytes`: Hex-encoded raw register bytes (2 bytes per register, big-endian)
    - `registers`: Array of unsigned 16-bit register values (0-65535)

### Protocol Events

Modbus request and response messages with raw PDU bytes.

```json
{
  "schema_version": "2.0",
  "kind": "event",
  "command": "client.rhr",
  "invocation": { "id": "...", "sequence": 1 },
  "timestamp": "...",
  "data": {
    "type": "protocol",
    "direction": "sent",
    "function_code": 3,
    "pdu": "030000000A"
  }
}
```

**Data fields:**

- `type`: Always `"protocol"`
- `direction`: Either `"sent"` or `"received"`
- `function_code`: Integer Modbus function code (1-127)
- `pdu`: Hex-encoded PDU bytes (uppercase, no spaces or separators)

**Common Function Codes:**

| Code | Function                     |
|------|------------------------------|
| 1    | Read Coils                   |
| 2    | Read Discrete Inputs         |
| 3    | Read Holding Registers       |
| 4    | Read Input Registers         |
| 5    | Write Single Coil            |
| 6    | Write Single Register        |
| 15   | Write Multiple Coils         |
| 16   | Write Multiple Registers     |
| 22   | Mask Write Register          |
| 23   | Read/Write Multiple Registers|

### Info Events

Connection and status information.

```json
{
  "schema_version": "2.0",
  "kind": "event",
  "command": "client.rhr",
  "invocation": { "id": "...", "sequence": 1 },
  "timestamp": "...",
  "data": {
    "type": "info",
    "message": "Hostname: localhost:502, Unit ID: 1"
  }
}
```

## Command Output Reference

### Read Commands

| Command | Alias                          | Data Output      |
|---------|--------------------------------|------------------|
| `rc`    | `read-coils`                   | `coil_table`     |
| `rdi`   | `read-discrete-inputs`         | `coil_table`     |
| `rhr`   | `read-holding-registers`       | `register_table` |
| `rir`   | `read-input-registers`         | `register_table` |
| `rwmr`  | `read-write-multiple-registers`| `register_table` |
| `scan`  |                                | `scan_results`   |

### Write Commands

| Command | Alias                       | Data Output                   |
|---------|-----------------------------|-------------------------------|
| `wsc`   | `write-single-coil`        | None (protocol messages only) |
| `wmc`   | `write-multiple-coils`      | None (protocol messages only) |
| `wsr`   | `write-single-register`     | None (protocol messages only) |
| `wmr`   | `write-multiple-registers`  | None (protocol messages only) |
| `mwr`   | `mask-write-register`       | None (protocol messages only) |

## Parsing Examples

### Extract register values with jq

```bash
$ modbus --format=json --emit=result client localhost rhr 0 5 | jq '.data.registers'
[0, 1, 2, 3, 4]
```

### Extract raw bytes

```bash
$ modbus --format=json --emit=result client localhost rhr 0 5 | jq -r '.data.bytes'
00000001000200030004
```

### Filter by record kind

```bash
$ modbus --format=json client localhost rhr 0 5 | jq -c 'select(.kind == "result")'
```

### Poll and extract register values

```bash
$ modbus --format=json --emit=result client localhost rhr 0 10 -c 5 | \
  jq -c '{timestamp, registers: .data.registers}'
```

## Notes

- All JSON output uses double quotes for strings
- Newlines in messages are represented as `\n`
- Special characters in strings are properly escaped
- Register values are unsigned 16-bit integers (0-65535)
- Boolean values are lowercase `true` and `false`
- Numbers are never quoted
- Hex-encoded `bytes` and `pdu` fields use lowercase hex with no spaces or separators
- All multibyte values use big-endian byte order
