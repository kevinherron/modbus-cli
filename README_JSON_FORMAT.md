# JSON Output Format

The modbus-cli tool supports JSON output format via the `--format=json` flag. This format is
designed for machine parsing and automation.

## Usage

```bash
modbus --format=json client <hostname> <command> [args...]
```

## Output Format

All JSON output is emitted as newline-delimited JSON objects (NDJSON). Each line is a complete,
valid JSON object.

## Message Types

### Info Messages

Connection and status information.

```json
{"type":"info","message":"Hostname: localhost:502, Unit ID: 1"}
```

**Schema:**

- `type`: Always `"info"`
- `message`: String containing informational text

### Protocol Messages

Modbus request and response messages with raw PDU bytes.

```json
{"type":"protocol","direction":"sent","function_code":3,"pdu":"030000000A"}
```

```json
{"type":"protocol","direction":"received","function_code":3,"pdu":"03140000010002000300040005000600070008000900"}
```

**Schema:**

- `type`: Always `"protocol"`
- `direction`: Either `"sent"` or `"received"`
- `function_code`: Integer Modbus function code (1-127)
- `pdu`: Hex-encoded PDU bytes (uppercase, no spaces or separators)

**Common Function Codes:**

- `1`: Read Coils
- `2`: Read Discrete Inputs
- `3`: Read Holding Registers
- `4`: Read Input Registers
- `5`: Write Single Coil
- `6`: Write Single Register
- `15`: Write Multiple Coils
- `16`: Write Multiple Registers
- `22`: Mask Write Register
- `23`: Read/Write Multiple Registers

### Error Messages

Error information sent to stderr.

```json
{"type":"error","message":"Connection failed: timeout"}
```

**Schema:**

- `type`: Always `"error"`
- `message`: String describing the error

### Warning Messages

Warning information sent to stderr.

```json
{"type":"warning","message":"Deprecated option used"}
```

**Schema:**

- `type`: Always `"warning"`
- `message`: String describing the warning

## Data Outputs

### Register Table

Output from register read operations (holding registers, input registers).

**Commands:** `rhr`, `rir`, `rwmr`

```bash
$ modbus --format=json --quiet client localhost rhr 0 10 | jq
```

```json
{
  "type": "register_table",
  "start_address": 0,
  "quantity": 10,
  "data": [
    0,
    0,
    1,
    0,
    2,
    0,
    3,
    0,
    4,
    0,
    5,
    0,
    6,
    0,
    7,
    0,
    8,
    0,
    9,
    0
  ]
}
```

**Schema:**

- `type`: Always `"register_table"`
- `start_address`: Starting register address (each register is 2 bytes)
- `quantity`: Number of registers read
- `data`: Array of byte values (0-255), in order received

**Example:** Reading 10 registers starting at address 0 yields 20 bytes.

### Coil Table

Output from coil/discrete input read operations.

**Commands:** `rc`, `rdi`

```bash
$ modbus --format=json --quiet client localhost rc 0 16 | jq
```

```json
{
  "type": "coil_table",
  "start_address": 0,
  "quantity": 16,
  "data": [
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false
  ]
}
```

**Schema:**

- `type`: Always `"coil_table"`
- `start_address`: Starting coil/discrete input address
- `quantity`: Number of coils/discrete inputs
- `data`: Array of boolean values, in LSB-first order per Modbus protocol

### Scan Results

Output from scan operations that read multiple register windows.

**Command:** `scan`

```bash
$ modbus --format=json --quiet client localhost scan 0 3 | jq
```

```json
{
  "type": "scan_results",
  "results": [
    {
      "address": 0,
      "values": [
        [
          0,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 1,
      "values": [
        [
          1,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 2,
      "values": [
        [
          2,
          0
        ],
        [
          2,
          1
        ]
      ],
      "identical": false
    }
  ]
}
```

**Schema:**

- `type`: Always `"scan_results"`
- `results`: Array of scan result objects
    - `address`: Register address
    - `values`: Array of 2-byte register values (each value is `[high_byte, low_byte]`)
    - `identical`: Boolean indicating if all scanned values for this address are identical
        - `true`: All scan windows that included this address read the same value
        - `false`: Different scan windows read different values for this address

**Note:** Multiple values for an address occur when scan windows overlap (e.g., scanning 0-9 with
step size 5 reads address 5 twice).

## Command Output Reference

### Read Commands

| Command | Description                   | Data Output      |
|---------|-------------------------------|------------------|
| `rc`    | Read Coils                    | `coil_table`     |
| `rdi`   | Read Discrete Inputs          | `coil_table`     |
| `rhr`   | Read Holding Registers        | `register_table` |
| `rir`   | Read Input Registers          | `register_table` |
| `rwmr`  | Read/Write Multiple Registers | `register_table` |
| `scan`  | Scan register range           | `scan_results`   |

### Write Commands

| Command | Description              | Data Output                   |
|---------|--------------------------|-------------------------------|
| `wsc`   | Write Single Coil        | None (protocol messages only) |
| `wmc`   | Write Multiple Coils     | None (protocol messages only) |
| `wsr`   | Write Single Register    | None (protocol messages only) |
| `wmr`   | Write Multiple Registers | None (protocol messages only) |
| `mwr`   | Mask Write Register      | None (protocol messages only) |

## Examples

### Read Holding Registers

```bash
$ modbus --format=json client localhost rhr 100 5 | jq
```

```json lines
{
  "type": "info",
  "message": "Hostname: localhost:502, Unit ID: 1"
}
{
  "type": "protocol",
  "direction": "sent",
  "function_code": 3,
  "pdu": "0300641005"
}
{
  "type": "protocol",
  "direction": "received",
  "function_code": 3,
  "pdu": "030A0000010002000300040000"
}
{
  "type": "register_table",
  "start_address": 100,
  "quantity": 5,
  "data": [
    0,
    0,
    1,
    0,
    2,
    0,
    3,
    0,
    4,
    0
  ]
}
```

**PDU Breakdown:**

- Request: `03 0064 0005` (FC=3, Address=100, Quantity=5)
- Response: `03 0A 0000010002000300040000` (FC=3, ByteCount=10, 5 registers of data)

### Read Coils

```bash
$ modbus --format=json client localhost rc 0 8 | jq
```

```json lines
{
  "type": "info",
  "message": "Hostname: localhost:502, Unit ID: 1"
}
{
  "type": "protocol",
  "direction": "sent",
  "function_code": 1,
  "pdu": "0100000008"
}
{
  "type": "protocol",
  "direction": "received",
  "function_code": 1,
  "pdu": "01010000"
}
{
  "type": "coil_table",
  "start_address": 0,
  "quantity": 8,
  "data": [
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false
  ]
}
```

**PDU Breakdown:**

- Request: `01 0000 0008` (FC=1, Address=0, Quantity=8)
- Response: `01 01 00` (FC=1, ByteCount=1, CoilData=0x00)

### Write Single Register

```bash
$ modbus --format=json client localhost wsr 100 0x1234 | jq
```

```json lines
{
  "type": "info",
  "message": "Hostname: localhost:502, Unit ID: 1"
}
{
  "type": "protocol",
  "direction": "sent",
  "function_code": 6,
  "pdu": "0600641234"
}
{
  "type": "protocol",
  "direction": "received",
  "function_code": 6,
  "pdu": "0600641234"
}
```

**PDU Breakdown:**

- Request: `06 0064 1234` (FC=6, Address=100, Value=0x1234)
- Response: `06 0064 1234` (FC=6, Address=100, Value=0x1234, echo of request)

### Scan with Overlapping Windows

```bash
$ modbus --format=json client localhost scan 0 10 --size=5 --step=3 | jq
```

```json lines
{
  "type": "info",
  "message": "Hostname: localhost:502, Unit ID: 1"
}
{
  "type": "scan_results",
  "results": [
    {
      "address": 0,
      "values": [
        [
          0,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 1,
      "values": [
        [
          1,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 2,
      "values": [
        [
          2,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 3,
      "values": [
        [
          3,
          0
        ],
        [
          3,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 4,
      "values": [
        [
          4,
          0
        ],
        [
          4,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 5,
      "values": [
        [
          5,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 6,
      "values": [
        [
          6,
          0
        ],
        [
          6,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 7,
      "values": [
        [
          7,
          0
        ],
        [
          7,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 8,
      "values": [
        [
          8,
          0
        ]
      ],
      "identical": true
    },
    {
      "address": 9,
      "values": [
        [
          9,
          0
        ]
      ],
      "identical": true
    }
  ]
}
```

**Note:** Protocol messages are omitted in scan output by default to reduce verbosity when reading
many windows.

## Quiet Mode

When combined with `--quiet`, info and protocol messages are suppressed, leaving only data output:

```bash
$ modbus --format=json --quiet client localhost rhr 0 5 | jq
```

```json
{
  "type": "register_table",
  "start_address": 0,
  "quantity": 5,
  "data": [
    0,
    0,
    1,
    0,
    2,
    0,
    3,
    0,
    4,
    0
  ]
}
```

## Parsing JSON Output

### jq Example

Extract just the register data:

```bash
$ modbus --format=json --quiet client localhost rhr 0 5 | jq -r '.data'
[
  0,
  0,
  1,
  0,
  2,
  0,
  3,
  0,
  4,
  0
]
```

Count total registers read:

```bash
$ modbus --format=json --quiet client localhost rhr 0 10 | jq '.data | length / 2'
10
```

Convert byte pairs to 16-bit integers:

```bash
$ modbus --format=json --quiet client localhost rhr 0 5 | \
  jq '[.data | _nwise(2) | .[0] * 256 + .[1]]'
[
  0,
  1,
  2,
  3,
  4
]
```

## Parsing PDU Hex Strings

The `pdu` field contains raw Modbus PDU bytes as uppercase hex strings without separators.

## Notes

- All JSON output uses double quotes for strings
- Newlines in messages are represented as `\n`
- Special characters in strings are properly escaped
- Byte values are always integers in the range 0-255
- Boolean values are lowercase `true` and `false`
- Numbers are never quoted
- PDU hex strings are uppercase with no spaces or separators
- All multibyte values in PDUs use big-endian byte order
