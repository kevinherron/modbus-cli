# read-coils (FC 01)

Read coil status bits. Returns a `coil_table` result with boolean values.

## Usage

```
modbus --json client <endpoint> [client-options] read-coils <address> <quantity>
```

| Argument     | Description             |
|--------------|-------------------------|
| `<address>`  | Starting address        |
| `<quantity>` | Number of coils to read |

## Options

| Option           | Description                                        | Default |
|------------------|----------------------------------------------------|---------|
| `-c, --count`    | Number of times to repeat (0 = indefinite polling) | 1       |
| `-i, --interval` | Interval between reads in milliseconds             | 1000    |

## Examples

Read 10 coils starting at address 0:

```bash
modbus --json client localhost read-coils 0 10
```

```json
{
  "kind": "result",
  "command": "client.read-coils",
  "invocation": {
    "id": "c6dfecdf-...",
    "sequence": 4
  },
  "timestamp": "2026-03-11T20:51:00.096305Z",
  "data": {
    "type": "coil_table",
    "start_address": 0,
    "quantity": 10,
    "bytes": "7401",
    "coils": [
      false,
      false,
      true,
      false,
      true,
      true,
      true,
      false,
      true,
      false
    ]
  }
}
```

Read 8 coils at address 100 with a 2-second timeout:

```bash
modbus --json client localhost -t 2000 read-coils 100 8
```

```json
{
  "kind": "result",
  "command": "client.read-coils",
  "invocation": {
    "id": "fa9ce8db-...",
    "sequence": 4
  },
  "timestamp": "2026-03-11T20:51:01.115399Z",
  "data": {
    "type": "coil_table",
    "start_address": 100,
    "quantity": 8,
    "bytes": "55",
    "coils": [
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
