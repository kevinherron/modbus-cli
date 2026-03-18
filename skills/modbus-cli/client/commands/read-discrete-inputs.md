# read-discrete-inputs (FC 02)

Read discrete input bits. Returns a `coil_table` result with boolean values.

## Usage

```
modbus --json client <endpoint> [client-options] read-discrete-inputs <address> <quantity>
```

| Argument     | Description                       |
|--------------|-----------------------------------|
| `<address>`  | Starting address                  |
| `<quantity>` | Number of discrete inputs to read |

## Options

| Option           | Description                                        | Default |
|------------------|----------------------------------------------------|---------|
| `-c, --count`    | Number of times to repeat (0 = indefinite polling) | 1       |
| `-i, --interval` | Interval between reads in milliseconds             | 1000    |

## Examples

Read 16 discrete inputs starting at address 0:

```bash
modbus --json client localhost read-discrete-inputs 0 16
```

```json lines
{
  "kind": "protocol",
  "command": "client.read-discrete-inputs",
  "invocation": {
    "id": "75112be7-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:01.624839Z",
  "data": {
    "direction": "sent",
    "function_code": 2,
    "pdu": "0200000010"
  }
}
{
  "kind": "protocol",
  "command": "client.read-discrete-inputs",
  "invocation": {
    "id": "75112be7-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:01.627163Z",
  "data": {
    "direction": "received",
    "function_code": 2,
    "pdu": "02025555"
  }
}
{
  "kind": "result",
  "command": "client.read-discrete-inputs",
  "invocation": {
    "id": "75112be7-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:01.627163Z",
  "data": {
    "start_address": 0,
    "quantity": 16,
    "bytes": "5555",
    "coils": [
      true,
      false,
      true,
      false,
      true,
      false,
      true,
      false,
      true,
      false,
      true,
      false,
      true,
      false,
      true,
      false
    ]
  }
}
```
