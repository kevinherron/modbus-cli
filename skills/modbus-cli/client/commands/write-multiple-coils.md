# write-multiple-coils (FC 15)

Write multiple coils. Emits protocol events only (no result object).

## Usage

```
modbus --json client <endpoint> [client-options] write-multiple-coils <address> <quantity> <values>
```

| Argument     | Description                             |
|--------------|-----------------------------------------|
| `<address>`  | Starting address                        |
| `<quantity>` | Number of coils to write                |
| `<values>`   | Comma-separated coil values (no spaces) |

Values accept: `true`/`false`, `1`/`0`, `on`/`off` (case-insensitive).

> **Important:** All three positional arguments are required. A common mistake is omitting
> `<quantity>` and passing values as the second argument:
> - Wrong: `write-multiple-coils 0 true,false,true`
> - Right: `write-multiple-coils 0 3 true,false,true`

## Examples

Write 4 coils starting at address 0:

```bash
modbus --json client localhost write-multiple-coils 0 4 true,false,true,false
```

```json lines
{
  "kind": "protocol",
  "command": "client.write-multiple-coils",
  "invocation": {
    "id": "5a3255d2-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:03.227992Z",
  "data": {
    "direction": "sent",
    "function_code": 15,
    "pdu": "0f000000040105"
  }
}
{
  "kind": "protocol",
  "command": "client.write-multiple-coils",
  "invocation": {
    "id": "5a3255d2-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:03.230701Z",
  "data": {
    "direction": "received",
    "function_code": 15,
    "pdu": "0f00000004"
  }
}
```

Using numeric values:

```bash
modbus --json client localhost write-multiple-coils 0 3 1,0,1
```
