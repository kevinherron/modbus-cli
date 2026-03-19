# write-single-coil (FC 05)

Write a single coil. Emits protocol events only (no result object).

## Usage

```
modbus --json client <endpoint> [client-options] write-single-coil <address> <value>
```

| Argument    | Description                                                        |
|-------------|--------------------------------------------------------------------|
| `<address>` | Coil address                                                       |
| `<value>`   | Coil value: `true`/`false`, `1`/`0`, `on`/`off` (case-insensitive) |

## Examples

Write coil at address 0 to true:

```bash
modbus --json client localhost write-single-coil 0 true
```

```json lines
{
  "kind": "protocol",
  "command": "client.write-single-coil",
  "invocation": {
    "id": "69685c1d-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:02.783475Z",
  "data": {
    "direction": "sent",
    "function_code": 5,
    "pdu": "050000ff00"
  }
}
{
  "kind": "protocol",
  "command": "client.write-single-coil",
  "invocation": {
    "id": "69685c1d-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:02.783964Z",
  "data": {
    "direction": "received",
    "function_code": 5,
    "pdu": "050000ff00"
  }
}
```

Numeric and keyword values are also accepted:

```bash
modbus --json client localhost write-single-coil 0 1
modbus --json client localhost write-single-coil 0 on
```
