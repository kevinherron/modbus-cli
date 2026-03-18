# write-multiple-registers (FC 16)

Write multiple 16-bit holding registers. Emits protocol events only (no result object).

## Usage

```
modbus --json client <endpoint> [client-options] write-multiple-registers <address> <quantity> <values>
```

| Argument     | Description                                     |
|--------------|-------------------------------------------------|
| `<address>`  | Starting address                                |
| `<quantity>` | Number of registers to write                    |
| `<values>`   | Comma-separated register values, decimal or hex |

> **Important:** All three positional arguments are required. A common mistake is omitting
> `<quantity>` and passing values as the second argument:
> - Wrong: `write-multiple-registers 0 100,200,300`
> - Right: `write-multiple-registers 0 3 100,200,300`

## Examples

Write 3 registers starting at address 0:

```bash
modbus --json client localhost write-multiple-registers 0 3 100,200,300
```

```json lines
{
  "kind": "event",
  "command": "client.write-multiple-registers",
  "invocation": {
    "id": "e452744c-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:09.906125Z",
  "data": {
    "type": "info",
    "message": "Hostname: localhost:502, Unit ID: 1"
  }
}
{
  "kind": "event",
  "command": "client.write-multiple-registers",
  "invocation": {
    "id": "e452744c-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:09.911380Z",
  "data": {
    "type": "protocol",
    "direction": "sent",
    "function_code": 16,
    "pdu": "100000000306006400c8012c"
  }
}
{
  "kind": "event",
  "command": "client.write-multiple-registers",
  "invocation": {
    "id": "e452744c-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:09.953174Z",
  "data": {
    "type": "protocol",
    "direction": "received",
    "function_code": 16,
    "pdu": "1000000003"
  }
}
```

Using hex values:

```bash
modbus --json client localhost write-multiple-registers 0 2 0xFF,0x00
```
