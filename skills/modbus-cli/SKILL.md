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

**Always use `--json --emit result`** when running commands programmatically or as an agent.
JSON output is machine-readable and structured, making it reliable to parse. The `--emit result`
flag filters out protocol events so you receive only the final result object. For write commands
(which produce no result object), use `--json` without `--emit result` to see protocol
events confirming success.

## Global Options

| Option                       | Description                       | Default |
|------------------------------|-----------------------------------|---------|
| `--json`                     | Output JSON instead of human text | false   |
| `--emit all\|data\|result`   | Output filtering                  | `data` (with `--json`) |
| `-v, --verbose`              | Verbose output, full stack traces | false   |
| `--no-color`                 | Disable ANSI colors               | false   |

## Exit Codes

Use CLI exit codes for automation:

| Code | Meaning |
|------|---------|
| `0`  | Success |
| `1`  | General Modbus failure fallback |
| `2`  | CLI usage or invalid argument error |
| `3`  | Connection or serial-port setup failure |
| `4`  | Modbus protocol failure (exception response, CRC error) |
| `5`  | Timeout or interrupted operation |
| `10` | Unexpected internal error |

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

> Before running any subcommand, read its linked command doc to learn the exact positional
> arguments required. Do not guess argument order from the command name alone.

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

| Command                  | FC | Returns        | Docs                                                                |
|--------------------------|----|----------------|---------------------------------------------------------------------|
| `read-coils`             | 01 | coil_table     | [read-coils](client/commands/read-coils.md)                         |
| `read-discrete-inputs`   | 02 | coil_table     | [read-discrete-inputs](client/commands/read-discrete-inputs.md)     |
| `read-holding-registers` | 03 | register_table | [read-holding-registers](client/commands/read-holding-registers.md) |
| `read-input-registers`   | 04 | register_table | [read-input-registers](client/commands/read-input-registers.md)     |

### Write Commands

| Command                         | FC | Docs                                                                              |
|---------------------------------|----|-----------------------------------------------------------------------------------|
| `write-single-coil`             | 05 | [write-single-coil](client/commands/write-single-coil.md)                         |
| `write-multiple-coils`          | 15 | [write-multiple-coils](client/commands/write-multiple-coils.md)                   |
| `write-single-register`         | 06 | [write-single-register](client/commands/write-single-register.md)                 |
| `write-multiple-registers`      | 16 | [write-multiple-registers](client/commands/write-multiple-registers.md)           |
| `mask-write-register`           | 22 | [mask-write-register](client/commands/mask-write-register.md)                     |
| `read-write-multiple-registers` | 23 | [read-write-multiple-registers](client/commands/read-write-multiple-registers.md) |

### Scan

| Command | FC | Docs                            |
|---------|----|---------------------------------|
| `scan`  | 03 | [scan](client/commands/scan.md) |

### JSON Output Format

With `--json`, output is NDJSON (one JSON object per line). Every line has a common
envelope with the following fields:

| Field                  | Description                                                                             |
|------------------------|-----------------------------------------------------------------------------------------|
| `kind`                 | `"result"` (final output), `"protocol"` (PDU sent/received), `"log"` (info/warnings), or `"error"` |
| `command`              | Command that produced this line, e.g. `"client.read-holding-registers"`                 |
| `invocation.id`        | UUID shared by all lines from the same invocation                                       |
| `invocation.sequence`  | Monotonically increasing counter within the invocation                                  |
| `invocation.iteration` | Present only when polling with `-c`; the 1-based poll iteration                         |
| `timestamp`            | ISO-8601 UTC timestamp                                                                  |
| `data`                 | Payload for `"result"`, `"protocol"`, and `"log"` kinds — shape depends on command (see below) |
| `error`                | Payload for `"error"` kind — contains `code`, `category`, `message`, and opt. `details` |

Result example — reading 5 holding registers with `--json --emit result`:

```json
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:02.069600Z",
  "data": {
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

| Mode     | Behavior                                                          |
|----------|-------------------------------------------------------------------|
| `all`    | Results + protocol + log + errors                                 |
| `data`   | Results + protocol + errors (no log). Default with `--json`.      |
| `result` | Results + errors only (no protocol PDUs). No output for writes.   |

#### Result Data

Results are distinguishable by their fields — no `data.type` sub-field:

| Returned by               | Key fields                                                              |
|---------------------------|-------------------------------------------------------------------------|
| Register read commands    | `start_address`, `quantity`, `bytes`, `registers`                       |
| Coil/discrete input reads | `start_address`, `quantity`, `bytes`, `coils`                           |
| `scan`                    | `windows[]` each with `start_address`, `quantity`, `bytes`, `registers` |

**Write commands** emit only protocol records (no result object). Using `--emit result` with a
write command produces no output. See individual [command docs](client/commands/) for full
protocol and result examples.

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
    - Wrong: `modbus --json client localhost read-holding-registers 0 5 --timeout 1000`
    - Right: `modbus --json client localhost -t 1000 read-holding-registers 0 5`

2. **Register value truncation**: values exceeding 16 bits are silently truncated
   (e.g., `999999` becomes `16959`).

3. **Port conflict**: specifying different ports in endpoint and `--port` flag causes an error.

4. **Port 502 requires root/admin**: the default Modbus port (502) is privileged. Use `-p 5020`
   or another high port for unprivileged usage.

5. **Shared process image by default**: all unit IDs share one process image. Writes from any
   unit ID are visible to all others. Use `--separate-units` to isolate them.

6. **Server runs indefinitely**: the server blocks until killed (Ctrl+C / SIGTERM).
