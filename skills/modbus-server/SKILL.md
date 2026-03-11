---
name: modbus-server
description: >-
  Start a Modbus TCP or RTU test server with pre-initialized process images.
  Use when the user wants to run a local Modbus server for testing or development.
compatibility: Requires the modbus CLI binary on $PATH (https://github.com/kevinherron/modbus-cli).
---

# Modbus Server Skill

Use this skill when the user wants to start a Modbus test server for development or testing
purposes.

## Agent Usage

Assume the `modbus` executable is available on `$PATH`.

## Command Structure

```
modbus [global-options] server [endpoint] [server-options]
```

## Endpoint Formats

TCP (default: `tcp:0.0.0.0`):

- `tcp:0.0.0.0` or `tcp://0.0.0.0:502`
- `tcp:localhost` or `tcp://localhost:5020`

Serial RTU:

- `rtu:/dev/ttyUSB0` or `rtu:COM3`

## Server Options

| Option             | Description                                                 | Default |
|--------------------|-------------------------------------------------------------|---------|
| `-p, --port`       | TCP port number                                             | 502     |
| `--separate-units` | Treat each unit ID as a separate device (own process image) | false   |

Serial-only options: `--baud` (9600), `--data-bits` (8), `--parity` (N), `--stop-bits` (1),
`--rs485`, `--rs485-rts-high`, `--rs485-rx-during-tx`, `--rs485-termination`,
`--rs485-delay-before` (0), `--rs485-delay-after` (0).

## Global Options

| Option                 | Description                       | Default |
|------------------------|-----------------------------------|---------|
| `--format human\|json` | Output format                     | `human` |
| `-q, --quiet`          | Suppress INFO messages            | false   |
| `-v, --verbose`        | Verbose output, full stack traces | false   |
| `--no-color`           | Disable ANSI colors               | false   |

## Pre-Initialized Data

The server starts with all 65,536 addresses pre-populated:

- **Holding registers & input registers**: each address contains its own address as the value
  (e.g., address 0 = 0, address 42 = 42, address 300 = 300).
- **Coils & discrete inputs**: even addresses are `true`, odd addresses are `false`.

## Examples

Start a TCP server on the default port (502):

```bash
modbus server
```

Start a TCP server on a non-privileged port:

```bash
modbus server -p 5020
```

Start a TCP server bound to localhost only:

```bash
modbus server tcp:localhost -p 5020
```

Start with separate process images per unit ID:

```bash
modbus server -p 5020 --separate-units
```

Start an RTU server on a serial port:

```bash
modbus server rtu:/dev/ttyUSB0 --baud 19200
```

## Gotchas

1. **Port 502 requires root/admin**: the default Modbus port (502) is privileged. Use `-p 5020`
   or another high port for unprivileged usage.

2. **Shared process image by default**: all unit IDs share one process image. Writes from any
   unit ID are visible to all others. Use `--separate-units` to isolate them.

3. **Server runs indefinitely**: the server blocks until killed (Ctrl+C / SIGTERM).
