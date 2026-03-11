# read-input-registers / rir (FC 04)

Read 16-bit input registers. Returns a `register_table` result.

## Usage

```
modbus --format json client <endpoint> [client-options] read-input-registers <address> <quantity>
modbus --format json client <endpoint> [client-options] rir <address> <quantity>
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

Read 10 input registers starting at address 0:

```bash
modbus --format json client localhost rir 0 10
```

```json
{
  "kind": "result",
  "command": "client.read-input-registers",
  "invocation": {
    "id": "98987aa3-...",
    "sequence": 4
  },
  "timestamp": "2026-03-11T20:51:02.489919Z",
  "data": {
    "type": "register_table",
    "start_address": 0,
    "quantity": 10,
    "bytes": "0000000100020003000400050006000700080009",
    "registers": [
      0,
      1,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9
    ]
  }
}
```
