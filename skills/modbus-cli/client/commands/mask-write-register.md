# mask-write-register (FC 22)

Bitwise modify a single holding register. Emits protocol events only (no result object).

Formula: `Result = (CurrentValue AND andMask) OR (orMask AND NOT andMask)`

- `andMask` preserves bits where mask=1, clears where mask=0
- `orMask` sets bits in positions cleared by the AND mask

## Usage

```
modbus --json client <endpoint> [client-options] mask-write-register <address> <andMask> <orMask>
```

| Argument    | Description                             |
|-------------|-----------------------------------------|
| `<address>` | Register address                        |
| `<andMask>` | AND mask: hex with optional `0x` prefix |
| `<orMask>`  | OR mask: hex with optional `0x` prefix  |

## Examples

Set the low byte while preserving the high byte:

```bash
modbus --json client localhost mask-write-register 0 0xFF00 0x00FF
```

```json lines
{
  "kind": "log",
  "command": "client.mask-write-register",
  "invocation": {
    "id": "7ec31479-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:11.630868Z",
  "data": {
    "level": "info",
    "message": "Hostname: localhost:502, Unit ID: 1"
  }
}
{
  "kind": "protocol",
  "command": "client.mask-write-register",
  "invocation": {
    "id": "7ec31479-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:11.636942Z",
  "data": {

    "direction": "sent",
    "function_code": 22,
    "pdu": "160000ff0000ff"
  }
}
{
  "kind": "protocol",
  "command": "client.mask-write-register",
  "invocation": {
    "id": "7ec31479-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:11.637629Z",
  "data": {

    "direction": "received",
    "function_code": 22,
    "pdu": "160000ff0000ff"
  }
}
```
