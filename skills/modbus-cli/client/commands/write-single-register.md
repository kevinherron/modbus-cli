# write-single-register (FC 06)

Write a single 16-bit holding register. Emits protocol events only (no result object).

## Usage

```
modbus --json client <endpoint> [client-options] write-single-register <address> <value>
```

| Argument    | Description                               |
|-------------|-------------------------------------------|
| `<address>` | Register address                          |
| `<value>`   | Register value: decimal or hex (`0x04D2`) |

## Examples

Write decimal value:

```bash
modbus --json client localhost write-single-register 0 1234
```

```json lines
{
  "kind": "log",
  "command": "client.write-single-register",
  "invocation": {
    "id": "621f5aa9-...",
    "sequence": 1
  },
  "timestamp": "2026-03-11T20:51:08.658328Z",
  "data": {
    "level": "info",
    "message": "Hostname: localhost:502, Unit ID: 1"
  }
}
{
  "kind": "protocol",
  "command": "client.write-single-register",
  "invocation": {
    "id": "621f5aa9-...",
    "sequence": 2
  },
  "timestamp": "2026-03-11T20:51:08.662762Z",
  "data": {

    "direction": "sent",
    "function_code": 6,
    "pdu": "06000004d2"
  }
}
{
  "kind": "protocol",
  "command": "client.write-single-register",
  "invocation": {
    "id": "621f5aa9-...",
    "sequence": 3
  },
  "timestamp": "2026-03-11T20:51:08.663182Z",
  "data": {

    "direction": "received",
    "function_code": 6,
    "pdu": "06000004d2"
  }
}
```

Write hex value (equivalent to 1234 decimal):

```bash
modbus --json client localhost write-single-register 0 0x04D2
```
