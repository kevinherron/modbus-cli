# write-single-register / wsr (FC 06)

Write a single 16-bit holding register. Emits protocol events only (no result object).

## Usage

```
modbus --format json client <endpoint> [client-options] write-single-register <address> <value>
modbus --format json client <endpoint> [client-options] wsr <address> <value>
```

| Argument    | Description                                    |
|-------------|------------------------------------------------|
| `<address>` | Register address                               |
| `<value>`   | Register value: decimal or hex (`0x04D2`)      |

## Examples

Write decimal value:

```bash
modbus --format json client localhost wsr 0 1234
```

```json
{"kind":"event","command":"client.write-single-register","invocation":{"id":"621f5aa9-...","sequence":1},"timestamp":"2026-03-11T20:51:08.658328Z","data":{"type":"info","message":"Hostname: localhost:502, Unit ID: 1"}}
{"kind":"event","command":"client.write-single-register","invocation":{"id":"621f5aa9-...","sequence":2},"timestamp":"2026-03-11T20:51:08.662762Z","data":{"type":"protocol","direction":"sent","function_code":6,"pdu":"06000004d2"}}
{"kind":"event","command":"client.write-single-register","invocation":{"id":"621f5aa9-...","sequence":3},"timestamp":"2026-03-11T20:51:08.663182Z","data":{"type":"protocol","direction":"received","function_code":6,"pdu":"06000004d2"}}
```

Write hex value (equivalent to 1234 decimal):

```bash
modbus --format json client localhost wsr 0 0x04D2
```
