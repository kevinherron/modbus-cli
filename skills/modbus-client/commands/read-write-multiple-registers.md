# read-write-multiple-registers / rwmr (FC 23)

Atomic read and write of holding registers in a single transaction. The write happens first,
then the read, but both occur in the same request/response cycle. Returns a `register_table`
result for the read portion.

## Usage

```
modbus --format json client <endpoint> [client-options] read-write-multiple-registers <readAddr> <readQty> <writeAddr> <writeQty> <writeValues>
modbus --format json client <endpoint> [client-options] rwmr <readAddr> <readQty> <writeAddr> <writeQty> <writeValues>
```

| Argument        | Description                                     |
|-----------------|-------------------------------------------------|
| `<readAddr>`    | Starting address for the read                   |
| `<readQty>`     | Number of registers to read                     |
| `<writeAddr>`   | Starting address for the write                  |
| `<writeQty>`    | Number of registers to write                    |
| `<writeValues>` | Comma-separated register values, decimal or hex |

## Examples

Read 5 registers from address 0, write 3 registers starting at address 10:

```bash
modbus --format json client localhost rwmr 0 5 10 3 100,200,300
```

```json lines
{
  "kind": "event",
  "command": "client.read-write-multiple-registers",
  "invocation": {
    "id": "b6f9e8cb-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:12.233480Z",
  "data": {
    "type": "info",
    "message": "Hostname: localhost:502, Unit ID: 1"
  }
}
{
  "kind": "event",
  "command": "client.read-write-multiple-registers",
  "invocation": {
    "id": "b6f9e8cb-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:12.238896Z",
  "data": {
    "type": "protocol",
    "direction": "sent",
    "function_code": 23,
    "pdu": "1700000005000a000306006400c8012c"
  }
}
{
  "kind": "event",
  "command": "client.read-write-multiple-registers",
  "invocation": {
    "id": "b6f9e8cb-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:12.239342Z",
  "data": {
    "type": "protocol",
    "direction": "received",
    "function_code": 23,
    "pdu": "170a00ff0000012c00030004"
  }
}
{
  "kind": "result",
  "command": "client.read-write-multiple-registers",
  "invocation": {
    "id": "b6f9e8cb-...",
    "sequence": 4
  },
  "timestamp": "2026-03-11T20:51:12.239342Z",
  "data": {
    "type": "register_table",
    "start_address": 0,
    "quantity": 5,
    "bytes": "00ff0000012c00030004",
    "registers": [
      255,
      0,
      300,
      3,
      4
    ]
  }
}
```
