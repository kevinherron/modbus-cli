# read-holding-registers (FC 03)

Read 16-bit holding registers. Returns a `register_table` result.

## Usage

```
modbus --json client <endpoint> [client-options] read-holding-registers <address> <quantity>
```

| Argument     | Description                 |
|--------------|-----------------------------|
| `<address>`  | Starting address            |
| `<quantity>` | Number of registers to read |

## Options

| Option           | Description                                        | Default |
|------------------|----------------------------------------------------|---------|
| `-c, --count`    | Number of times to repeat (0 = indefinite polling) | 1       |
| `-i, --interval` | Interval between reads in milliseconds             | 1000    |

## Examples

Read 10 holding registers starting at address 0:

```bash
modbus --json client localhost read-holding-registers 0 10
```

```json lines
{
  "kind": "protocol",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:02.067134Z",
  "data": {
    "direction": "sent",
    "function_code": 3,
    "pdu": "030000000a"
  }
}
{
  "kind": "protocol",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:02.069600Z",
  "data": {
    "direction": "received",
    "function_code": 3,
    "pdu": "0314002a0000012c0003000400640006000700080009"
  }
}
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "13eb6dda-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:02.069600Z",
  "data": {
    "start_address": 0,
    "quantity": 10,
    "bytes": "002a0000012c0003000400640006000700080009",
    "registers": [
      42,
      0,
      300,
      3,
      4,
      100,
      6,
      7,
      8,
      9
    ]
  }
}
```

Read registers from a different unit ID:

```bash
modbus --json client localhost --unit-id 2 read-holding-registers 40000 5
```

```json lines
{
  "kind": "protocol",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "981d582c-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:25.663841Z",
  "data": {
    "direction": "sent",
    "function_code": 3,
    "pdu": "039c400005"
  }
}
{
  "kind": "protocol",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "981d582c-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:25.666287Z",
  "data": {
    "direction": "received",
    "function_code": 3,
    "pdu": "030a9c409c419c429c439c44"
  }
}
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "981d582c-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:25.666287Z",
  "data": {
    "start_address": 40000,
    "quantity": 5,
    "bytes": "9c409c419c429c439c44",
    "registers": [
      40000,
      40001,
      40002,
      40003,
      40004
    ]
  }
}
```

Poll 5 times at 500ms intervals:

```bash
modbus --json client localhost read-holding-registers 0 10 -c 5 -i 500
```

Each iteration emits its own result with an `iteration` field in the invocation:

```json lines
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "e45c4677-...",
    "sequence": 3,
    "iteration": 1
  },
  "timestamp": "...",
  "data": {
    "start_address": 0,
    "quantity": 10,
    "bytes": "...",
    "registers": [
      ...
    ]
  }
}
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "e45c4677-...",
    "sequence": 6,
    "iteration": 2
  },
  "timestamp": "...",
  "data": {
    "start_address": 0,
    "quantity": 10,
    "bytes": "...",
    "registers": [
      ...
    ]
  }
}
```

Suppress info and protocol events with `--emit result`:

```bash
modbus --json --emit result client localhost read-holding-registers 0 10
```

```json
{
  "kind": "result",
  "command": "client.read-holding-registers",
  "invocation": {
    "id": "ea7d35d5-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:21.755366Z",
  "data": {
    "start_address": 0,
    "quantity": 10,
    "bytes": "002a0000012c0003000400640006000700080009",
    "registers": [
      42,
      0,
      300,
      3,
      4,
      100,
      6,
      7,
      8,
      9
    ]
  }
}
```

Note: with `--emit result`, the sequence starts at 1 because info and protocol events are
suppressed.
