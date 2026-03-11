---
name: modbus-client
description: >-
  Interact with Modbus TCP/RTU devices using the CLI client. Read coils,
  discrete inputs, holding registers, and input registers. Write single or
  multiple coils and registers. Scan address ranges. Use when the user wants
  to communicate with a Modbus device or build modbus client commands.
compatibility: Requires the modbus CLI binary on $PATH (https://github.com/kevinherron/modbus-cli).
---

# Modbus Client Skill

Use this skill when the user wants to interact with a Modbus device using the Modbus CLI client
sub-command: reading coils/registers, writing values, or scanning address ranges.

## Agent Usage

Assume the `modbus` executable is available on `$PATH`.

**Always use `--format json --emit result`** when running commands programmatically or as an agent.
JSON output is machine-readable and structured, making it reliable to parse. The `--emit result`
flag filters out protocol events so you receive only the final result object. For write commands
(which produce no result object), use `--format json` without `--emit result` to see protocol
events confirming success.

## Command Structure

```
modbus [global-options] client <endpoint> [client-options] <subcommand> [args]
```

**Important**: Client options (e.g. `--timeout`, `--unit-id`) must appear BEFORE the subcommand.

## Endpoint Formats

TCP (all equivalent):

- `localhost` or `localhost:502`
- `tcp:localhost` or `tcp://localhost:502`
- `[::1]` (IPv6)

Serial RTU:

- `rtu:/dev/ttyUSB0` or `rtu:COM3`

## Global Options

| Option                       | Description                       | Default |
|------------------------------|-----------------------------------|---------|
| `--format human\|json`       | Output format                     | `human` |
| `--emit all\|result\|events` | Output filtering                  | `all`   |
| `-q, --quiet`                | Suppress INFO messages            | false   |
| `-v, --verbose`              | Verbose output, full stack traces | false   |
| `--no-color`                 | Disable ANSI colors               | false   |

## Client Options

| Option          | Description          | Default |
|-----------------|----------------------|---------|
| `-p, --port`    | TCP port             | 502     |
| `--unit-id`     | Modbus Unit/Slave ID | 1       |
| `-t, --timeout` | Request timeout (ms) | 5000    |

Serial-only options: `--baud` (9600), `--data-bits` (8), `--parity` (N), `--stop-bits` (1),
`--rs485`, `--rs485-rts-high`, `--rs485-rx-during-tx`, `--rs485-termination`,
`--rs485-delay-before` (0), `--rs485-delay-after` (0).

## Value Formats

- **Coils**: `true`/`false`, `1`/`0`, `on`/`off` (case-insensitive)
- **Registers**: decimal (`1234`) or hex (`0x04D2`)
- **Masks**: hex with optional `0x` prefix (`0xFFFF` or `FFFF`)
- **Multiple values**: comma-separated, no spaces (`100,200,300`)

## JSON Output Format

With `--format json`, output is NDJSON (one JSON object per line). Every line shares a common
envelope:

```json
{
  "kind": "event"
  |
  "result",
  "command": "client.<subcommand>",
  "invocation": {
    "id": "<uuid>",
    "sequence": <n>
  },
  "timestamp": "<ISO-8601>",
  "data": {
    ...
  }
}
```

When polling with `-c`, invocation also includes `"iteration": <n>`.

### Event Types

**Info event** — emitted once at the start of each invocation:

```json
{
  "kind": "event",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:02.064017Z",
  "data": {
    "type": "info",
    "message": "Hostname: localhost:502, Unit ID: 1"
  }
}
```

**Protocol event** — emitted for each request sent and response received:

```json
{
  "kind": "event",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:02.069065Z",
  "data": {
    "type": "protocol",
    "direction": "sent",
    "function_code": 3,
    "pdu": "030000000a"
  }
}
```

```json
{
  "kind": "event",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:02.069600Z",
  "data": {
    "type": "protocol",
    "direction": "received",
    "function_code": 3,
    "pdu": "0314..."
  }
}
```

### Result Types

**Register table** — returned by register read commands:

```json
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 4
  },
  "timestamp": "2026-03-11T20:51:02.069600Z",
  "data": {
    "type": "register_table",
    "start_address": 0,
    "quantity": 10,
    "bytes": "002a0000012c00030004...",
    "registers": [
      42,
      0,
      300,
      3,
      4,
      ...
    ]
  }
}
```

**Coil table** — returned by coil/discrete input read commands:

```json
{
  "kind": "result",
  "command": "client.read-coils",
  "invocation": {
    "id": "c6dfecdf-...",
    "sequence": 4
  },
  "timestamp": "2026-03-11T20:51:00.096305Z",
  "data": {
    "type": "coil_table",
    "start_address": 0,
    "quantity": 10,
    "bytes": "7401",
    "coils": [
      false,
      false,
      true,
      false,
      true,
      true,
      true,
      false,
      true,
      false
    ]
  }
}
```

**Scan results** — returned by the scan command:

```json
{
  "kind": "result",
  "command": "client.scan",
  "invocation": {
    "id": "5025c25f-...",
    "sequence": 22
  },
  "timestamp": "2026-03-11T20:51:12.593890Z",
  "data": {
    "type": "scan_results",
    "windows": [
      {
        "start_address": 0,
        "quantity": 10,
        "bytes": "...",
        "registers": [
          255,
          0,
          ...
        ]
      },
      {
        "start_address": 10,
        "quantity": 10,
        "bytes": "...",
        "registers": [
          100,
          200,
          ...
        ]
      }
    ]
  }
}
```

**Write commands** emit only protocol events (no result object). Using `--emit result` with a
write command produces no output.

### Emit Modes

| Mode     | Behavior                                               |
|----------|--------------------------------------------------------|
| `all`    | Events + results (default)                             |
| `result` | Results only (no protocol PDUs). No output for writes. |
| `events` | Protocol events only (no result tables)                |

## Commands

### Read Commands

All read commands support polling: `-c N` (repeat count, 0=indefinite) and `-i N` (interval ms,
default 1000).

| Command                  | Alias | FC | Returns        | Docs                                                         |
|--------------------------|-------|----|----------------|--------------------------------------------------------------|
| `read-coils`             | `rc`  | 01 | coil_table     | [read-coils](commands/read-coils.md)                         |
| `read-discrete-inputs`   | `rdi` | 02 | coil_table     | [read-discrete-inputs](commands/read-discrete-inputs.md)     |
| `read-holding-registers` | `rhr` | 03 | register_table | [read-holding-registers](commands/read-holding-registers.md) |
| `read-input-registers`   | `rir` | 04 | register_table | [read-input-registers](commands/read-input-registers.md)     |

### Write Commands

| Command                         | Alias  | FC | Docs                                                                       |
|---------------------------------|--------|----|----------------------------------------------------------------------------|
| `write-single-coil`             | `wsc`  | 05 | [write-single-coil](commands/write-single-coil.md)                         |
| `write-multiple-coils`          | `wmc`  | 15 | [write-multiple-coils](commands/write-multiple-coils.md)                   |
| `write-single-register`         | `wsr`  | 06 | [write-single-register](commands/write-single-register.md)                 |
| `write-multiple-registers`      | `wmr`  | 16 | [write-multiple-registers](commands/write-multiple-registers.md)           |
| `mask-write-register`           | `mwr`  | 22 | [mask-write-register](commands/mask-write-register.md)                     |
| `read-write-multiple-registers` | `rwmr` | 23 | [read-write-multiple-registers](commands/read-write-multiple-registers.md) |

### Scan

| Command | FC | Docs                     |
|---------|----|--------------------------|
| `scan`  | 03 | [scan](commands/scan.md) |

## Gotchas

1. **Option placement**: client options must go BEFORE the subcommand, not after.
    - Wrong: `modbus --format json client localhost rhr 0 5 --timeout 1000`
    - Right: `modbus --format json client localhost -t 1000 rhr 0 5`

2. **Register value truncation**: values exceeding 16 bits are silently truncated
   (e.g., `999999` becomes `16959`).

3. **Port conflict**: specifying different ports in endpoint and `--port` flag causes an error.
