---
name: modbus-cli
description: >-
  Interact with Modbus TCP/RTU devices using the CLI. Read coils, discrete
  inputs, holding registers, and input registers. Write single or multiple coils
  and registers. Scan address ranges. Start a test server. Use when the user
  wants to communicate with a Modbus device, build modbus commands, or run a
  local Modbus server for testing.
compatibility: Requires the modbus CLI binary on $PATH (https://github.com/kevinherron/modbus-cli).
---

# Modbus CLI Skill

Use this skill when the user wants to interact with Modbus devices or run a test server using the
Modbus CLI.

## Agent Usage

Assume the `modbus` executable is available on `$PATH`.

**Always use `--format json --emit result`** when running commands programmatically or as an agent.
JSON output is machine-readable and structured, making it reliable to parse. The `--emit result`
flag filters out protocol events so you receive only the final result object. For write commands
(which produce no result object), use `--format json` without `--emit result` to see protocol
events confirming success.

## Global Options

| Option                       | Description                       | Default |
|------------------------------|-----------------------------------|---------|
| `--format human\|json`       | Output format                     | `human` |
| `--emit all\|result\|events` | Output filtering                  | `all`   |
| `-v, --verbose`              | Verbose output, full stack traces | false   |
| `--no-color`                 | Disable ANSI colors               | false   |

## Endpoint Formats

TCP (all equivalent):

- `localhost` or `localhost:502`
- `tcp:localhost` or `tcp://localhost:502`
- `[::1]` (IPv6)

Serial RTU:

- `rtu:/dev/ttyUSB0` or `rtu:COM3`

## Value Formats

- **Coils**: `true`/`false`, `1`/`0`, `on`/`off` (case-insensitive)
- **Registers**: decimal (`1234`) or hex (`0x04D2`)
- **Masks**: hex with optional `0x` prefix (`0xFFFF` or `FFFF`)
- **Multiple values**: comma-separated, no spaces (`100,200,300`)

---

## Client

```
modbus [global-options] client <endpoint> [client-options] <subcommand> [args]
```

**Important**: Client options (e.g. `--timeout`, `--unit-id`) must appear BEFORE the subcommand.

### Client Options

| Option          | Description          | Default |
|-----------------|----------------------|---------|
| `-p, --port`    | TCP port             | 502     |
| `--unit-id`     | Modbus Unit/Slave ID | 1       |
| `-t, --timeout` | Request timeout (ms) | 5000    |

Serial-only options: `--baud` (9600), `--data-bits` (8), `--parity` (N), `--stop-bits` (1),
`--rs485`, `--rs485-rts-high`, `--rs485-rx-during-tx`, `--rs485-termination`,
`--rs485-delay-before` (0), `--rs485-delay-after` (0).

### Read Commands

All read commands support polling: `-c N` (repeat count, 0=indefinite) and `-i N` (interval ms,
default 1000).

| Command                  | Alias | FC | Returns        | Docs                                                                |
|--------------------------|-------|----|----------------|---------------------------------------------------------------------|
| `read-coils`             | `rc`  | 01 | coil_table     | [read-coils](client/commands/read-coils.md)                         |
| `read-discrete-inputs`   | `rdi` | 02 | coil_table     | [read-discrete-inputs](client/commands/read-discrete-inputs.md)     |
| `read-holding-registers` | `rhr` | 03 | register_table | [read-holding-registers](client/commands/read-holding-registers.md) |
| `read-input-registers`   | `rir` | 04 | register_table | [read-input-registers](client/commands/read-input-registers.md)     |

### Write Commands

| Command                         | Alias  | FC | Docs                                                                              |
|---------------------------------|--------|----|-----------------------------------------------------------------------------------|
| `write-single-coil`             | `wsc`  | 05 | [write-single-coil](client/commands/write-single-coil.md)                         |
| `write-multiple-coils`          | `wmc`  | 15 | [write-multiple-coils](client/commands/write-multiple-coils.md)                   |
| `write-single-register`         | `wsr`  | 06 | [write-single-register](client/commands/write-single-register.md)                 |
| `write-multiple-registers`      | `wmr`  | 16 | [write-multiple-registers](client/commands/write-multiple-registers.md)           |
| `mask-write-register`           | `mwr`  | 22 | [mask-write-register](client/commands/mask-write-register.md)                     |
| `read-write-multiple-registers` | `rwmr` | 23 | [read-write-multiple-registers](client/commands/read-write-multiple-registers.md) |

### Scan

| Command | FC | Docs                            |
|---------|----|---------------------------------|
| `scan`  | 03 | [scan](client/commands/scan.md) |

### JSON Output Format

With `--format json`, output is NDJSON (one JSON object per line). Every line has a common
envelope with the following fields:

| Field                  | Description                                                                          |
|------------------------|--------------------------------------------------------------------------------------|
| `kind`                 | `"event"` (info or protocol), `"result"` (final command output), or `"error"`        |
| `command`              | Command that produced this line, e.g. `"client.read-holding-registers"`              |
| `invocation.id`        | UUID shared by all lines from the same invocation                                    |
| `invocation.sequence`  | Monotonically increasing counter within the invocation                               |
| `invocation.iteration` | Present only when polling with `-c`; the 1-based poll iteration                      |
| `timestamp`            | ISO-8601 UTC timestamp                                                               |
| `data`                 | Payload for `"event"` and `"result"` kinds — shape depends on command (see below)    |
| `error`                | Payload for `"error"` kind — contains `code`, `category`, `message`, and opt. `details` |

Result example — reading five holding registers with `--format json --emit result`:

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
    "quantity": 5,
    "bytes": "00000001000200030004",
    "registers": [
      0,
      1,
      2,
      3,
      4
    ]
  }
}
```

Error example — connection refused:

```json
{
  "kind": "error",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "9520bd66-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T22:00:33.115703Z",
  "error": {
    "code": "CONNECTION_REFUSED",
    "category": "connection",
    "message": "io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: localhost/127.0.0.1:50200"
  }
}
```

#### Emit Modes

| Mode     | Behavior                                               |
|----------|--------------------------------------------------------|
| `all`    | Events + results (default)                             |
| `result` | Results only (no protocol PDUs). No output for writes. |
| `events` | Protocol events only (no result tables)                |

#### Result Data Types

| `data.type`      | Returned by               | Key fields                                                              |
|------------------|---------------------------|-------------------------------------------------------------------------|
| `register_table` | Register read commands    | `start_address`, `quantity`, `bytes`, `registers`                       |
| `coil_table`     | Coil/discrete input reads | `start_address`, `quantity`, `bytes`, `coils`                           |
| `scan_results`   | `scan`                    | `windows[]` each with `start_address`, `quantity`, `bytes`, `registers` |

**Write commands** emit only protocol events (no result object). Using `--emit result` with a
write command produces no output. See individual [command docs](client/commands/) for full
event and result examples.

---

## Server

```
modbus [global-options] server [endpoint] [server-options]
```

Start a Modbus test server with pre-initialized process images.

### Server Options

| Option             | Description                                                 | Default |
|--------------------|-------------------------------------------------------------|---------|
| `-p, --port`       | TCP port number                                             | 502     |
| `--separate-units` | Treat each unit ID as a separate device (own process image) | false   |

Serial-only options: `--baud` (9600), `--data-bits` (8), `--parity` (N), `--stop-bits` (1),
`--rs485`, `--rs485-rts-high`, `--rs485-rx-during-tx`, `--rs485-termination`,
`--rs485-delay-before` (0), `--rs485-delay-after` (0).

### Pre-Initialized Data

The server starts with all 65,536 addresses pre-populated:

- **Holding registers & input registers**: each address contains its own address as the value
  (e.g., address 0 = 0, address 42 = 42, address 300 = 300).
- **Coils & discrete inputs**: even addresses are `true`, odd addresses are `false`.

### Server Examples

```bash
modbus server -p 5020                           # TCP on non-privileged port
modbus server tcp:localhost -p 5020             # bound to localhost only
modbus server -p 5020 --separate-units          # separate process images per unit ID
modbus server rtu:/dev/ttyUSB0 --baud 19200     # RTU on serial port
```

---

## Gotchas

1. **Option placement**: client options must go BEFORE the subcommand, not after.
    - Wrong: `modbus --format json client localhost rhr 0 5 --timeout 1000`
    - Right: `modbus --format json client localhost -t 1000 rhr 0 5`

2. **Register value truncation**: values exceeding 16 bits are silently truncated
   (e.g., `999999` becomes `16959`).

3. **Port conflict**: specifying different ports in endpoint and `--port` flag causes an error.

4. **Port 502 requires root/admin**: the default Modbus port (502) is privileged. Use `-p 5020`
   or another high port for unprivileged usage.

5. **Shared process image by default**: all unit IDs share one process image. Writes from any
   unit ID are visible to all others. Use `--separate-units` to isolate them.

6. **Server runs indefinitely**: the server blocks until killed (Ctrl+C / SIGTERM).
