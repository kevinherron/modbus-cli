# Modbus CLI

A command-line interface for Modbus TCP and RTU serial operations, built with Java 25 and compilable
to a native executable using GraalVM.

## Quick Examples

**Start a test server (TCP):**

```bash
$ modbus server
Modbus server started on 0.0.0.0:502
```

The server initializes with 65536 holding registers, each pre-populated with its address as a value.
Leave this running in one terminal while trying the client examples below in another.

**Start a test server (RTU serial):**

```bash
$ modbus server rtu:/dev/ttyUSB0 --baud 19200
Modbus RTU server started on /dev/ttyUSB0
```

**Read holding registers:**

```bash
$ modbus client localhost read-holding-registers 0 10
Hostname: localhost:502, Unit ID: 1
→ ReadHoldingRegistersRequest[address=0, quantity=10]
← ReadHoldingRegistersResponse[registers=0000000100020003000400050006000700080009]
Offset (hex)	Bytes (hex)
-------------------------
00000000	00 00 00 01 00 02 00 03 00 04 00 05 00 06 00 07
00000010	00 08 00 09 .. .. .. .. .. .. .. .. .. .. .. ..
```

**Get JSON output for automation:**

```bash
$ modbus --json --emit all client localhost read-holding-registers 0 10
{"kind":"log","command":"client.read-holding-registers","invocation":{"id":"...","sequence":1},"timestamp":"...","data":{"level":"info","message":"Hostname: localhost:502, Unit ID: 1"}}
{"kind":"protocol","command":"client.read-holding-registers","invocation":{"id":"...","sequence":2},"timestamp":"...","data":{"direction":"sent","function_code":3,"pdu":"030000000a"}}
{"kind":"protocol","command":"client.read-holding-registers","invocation":{"id":"...","sequence":3},"timestamp":"...","data":{"direction":"received","function_code":3,"pdu":"03140000000100020003000400050006000700080009"}}
{"kind":"result","command":"client.read-holding-registers","invocation":{"id":"...","sequence":4},"timestamp":"...","data":{"start_address":0,"quantity":10,"bytes":"0000000100020003000400050006000700080009","registers":[0,1,2,3,4,5,6,7,8,9]}}
```

**Write then read back a register:**

```bash
$ modbus --json --emit all client localhost write-single-register 100 42
{"kind":"log","command":"client.write-single-register","invocation":{"id":"...","sequence":1},"timestamp":"...","data":{"level":"info","message":"Hostname: localhost:502, Unit ID: 1"}}
{"kind":"protocol","command":"client.write-single-register","invocation":{"id":"...","sequence":2},"timestamp":"...","data":{"direction":"sent","function_code":6,"pdu":"060064002a"}}
{"kind":"protocol","command":"client.write-single-register","invocation":{"id":"...","sequence":3},"timestamp":"...","data":{"direction":"received","function_code":6,"pdu":"060064002a"}}

$ modbus --json client localhost read-holding-registers 100 1
{"kind":"result","command":"client.read-holding-registers","invocation":{"id":"...","sequence":4},"timestamp":"...","data":{"start_address":100,"quantity":1,"bytes":"002a","registers":[42]}}
```

**Scan a range of registers:**

```bash
$ modbus client localhost scan 0 50 --size=10
Hostname: localhost:502, Unit ID: 1
Address 	Values (hex, 2 bytes each)
-------------------------------------------------------
0000    	0000 0001 0002 0003 0004 0005 0006 0007
0008    	0008 0009 000A 000B 000C 000D 000E 000F
0010    	0010 0011 0012 0013 0014 0015 0016 0017
0018    	0018 0019 001A 001B 001C 001D 001E 001F
0020    	0020 0021 0022 0023 0024 0025 0026 0027
0028    	0028 0029 002A 002B 002C 002D 002E 002F
0030    	0030 0031
```

**Poll and filter JSON output with jq:**

```bash
$ modbus --json client localhost read-holding-registers 0 10 -c 5 \
    | jq -c 'select(.kind == "result") | {timestamp, data: .data.registers}'
{"timestamp":"2026-03-11T22:58:39.194847Z","data":[0,1,2,3,4,5,6,7,8,9]}
{"timestamp":"2026-03-11T22:58:40.196611Z","data":[0,1,2,3,4,5,6,7,8,9]}
{"timestamp":"2026-03-11T22:58:41.201814Z","data":[0,1,2,3,4,5,6,7,8,9]}
{"timestamp":"2026-03-11T22:58:42.203609Z","data":[0,1,2,3,4,5,6,7,8,9]}
{"timestamp":"2026-03-11T22:58:43.208813Z","data":[0,1,2,3,4,5,6,7,8,9]}
```

**Read holding registers over RTU serial:**

```bash
$ modbus client rtu:/dev/ttyUSB0 --baud 19200 read-holding-registers 0 10
Serial Port: /dev/ttyUSB0, Unit ID: 1
...
```

**Use RS-485 mode:**

```bash
$ modbus client rtu:/dev/ttyUSB0 --baud 19200 --rs485 --rs485-rts-high read-holding-registers 0 10
Serial Port: /dev/ttyUSB0, Unit ID: 1, RS-485 mode
...
```

## Features

- **Client Commands**: Read/write coils, discrete inputs, holding registers, and input registers
- **Modbus TCP and RTU**: Full support for Modbus TCP and Modbus RTU over serial (RS-232/RS-485)
- **Flexible Endpoint Format**: Connect via `hostname`, `tcp:hostname[:port]`, or `rtu:/dev/ttyUSB0`
- **Serial Port Configuration**: Baud rate, data bits, parity, stop bits, and RS-485 mode options
- **Multiple Output Formats**: Human-readable tables (default) or JSON for machine parsing
- **Flexible Scanning**: Scan register ranges with configurable window size and step
- **GraalVM Native Image**: Compile to a fast-starting, low-memory native executable
- **Cross-platform**: Works on Linux, macOS, and Windows

## Prerequisites

### For JAR Build

- Java 25 or later
- Maven 3.6+

### For Native Image Build

- GraalVM for JDK 25
- Maven 3.6+

## Installation

### Installing GraalVM

#### Using SDKMAN (Recommended)

```bash
# Install SDKMAN if not already installed
curl -s "https://get.sdkman.io" | bash

# Install GraalVM for JDK 25
sdk install java 25-graal

# Set as default (optional)
sdk default java 25-graal
```

#### Manual Installation

1. Download GraalVM for JDK 25 from [graalvm.org/downloads](https://www.graalvm.org/downloads/)
2. Extract and set `JAVA_HOME` to the GraalVM directory
3. Add `$JAVA_HOME/bin` to your `PATH`

#### Using Homebrew (macOS)

```bash
brew install --cask graalvm-jdk
```

## Building

### Standard JAR Build

```bash
mvn clean package
```

This creates an executable JAR: `target/modbus-cli-0.1-SNAPSHOT.jar`

### Native Image Build

```bash
mvn -Pnative clean package
```

This creates a native executable: `target/modbus`

**Note**: Native image compilation takes 1-3 minutes and requires 4GB+ RAM. The first build may take
longer as GraalVM analyzes all dependencies.

## Usage

### Running with Java

```bash
java -jar target/modbus-cli-0.1-SNAPSHOT.jar [command] [options]
```

### Running Native Executable

```bash
./target/modbus [command] [options]
```

### Command Structure

```
modbus [global-options] client <endpoint> [client-options] <subcommand> [subcommand-options]
modbus [global-options] server [endpoint] [server-options]
```

### Endpoint Formats

The `<endpoint>` parameter accepts several formats:

| Format                  | Example             | Description               |
|-------------------------|---------------------|---------------------------|
| `hostname`              | `localhost`         | TCP with default port 502 |
| `tcp:hostname[:port]`   | `tcp:myhost:1502`   | Explicit TCP              |
| `tcp://hostname[:port]` | `tcp://myhost:1502` | TCP (URI-style)           |
| `rtu:<serial-port>`     | `rtu:/dev/ttyUSB0`  | RTU serial (Linux/macOS)  |
| `rtu:<serial-port>`     | `rtu:COM3`          | RTU serial (Windows)      |

IPv6 addresses are supported with bracket notation: `tcp:[::1]:1502`

Bare hostnames (without a scheme) are treated as TCP for backward compatibility.

### Available Subcommands

#### Read Operations (read-only, safe to retry)

- `read-coils` / `rc` `<address> <quantity>` - Read coils (FC 01)
- `read-discrete-inputs` / `rdi` `<address> <quantity>` - Read discrete inputs (FC 02)
- `read-holding-registers` / `rhr` `<address> <quantity>` - Read holding registers (FC 03)
- `read-input-registers` / `rir` `<address> <quantity>` - Read input registers (FC 04)

#### Write Operations (mutating)

- `write-single-coil` / `wsc` `<address> <value>` - Write single coil (FC 05)
- `write-multiple-coils` / `wmc` `<address> <values...>` - Write multiple coils (FC 15)
- `write-single-register` / `wsr` `<address> <value>` - Write single register (FC 06)
- `write-multiple-registers` / `wmr` `<address> <values...>` - Write multiple registers (FC 16)
- `mask-write-register` / `mwr` `<address> <and-mask> <or-mask>` - Mask write register (FC 22)
- `read-write-multiple-registers` / `rwmr` `<read-addr> <read-qty> <write-addr> <values...>` -
  Read/Write multiple registers (FC 23)

#### Other

- `scan <start> <end>` - Scan a range of registers using a sliding window

### Options

**Global Options:**

- `--json` - Use JSON output format (default: human-readable)
- `--emit <mode>` - Control JSON output volume: `all`, `data` (default with `--json`), `result`
- `-v, --verbose` - Verbose mode - detailed output
- `-q, --quiet` - Quiet mode - minimal output
- `--no-color` - Disable ANSI color output

**Client Options:**

- `-p, --port <port>` - Port number, TCP only (default: 502)
- `--unit-id <id>` - Unit/slave ID (default: 1)
- `-t, --timeout <ms>` - Request timeout in milliseconds (default: 5000)

**Serial Port Options** (apply to both client and server when using `rtu:` endpoints):

- `--baud <rate>` - Baud rate (default: 9600)
- `--data-bits <5|6|7|8>` - Data bits (default: 8)
- `--parity <N|E|O>` - Parity: none, even, or odd (default: N)
- `--stop-bits <1|2>` - Stop bits (default: 1)

**RS-485 Options** (require `--rs485` to enable):

- `--rs485` - Enable RS-485 mode
- `--rs485-rts-high` - RTS active high
- `--rs485-termination` - Enable bus termination
- `--rs485-rx-during-tx` - Enable receiving during transmission
- `--rs485-delay-before <us>` - Delay before send in microseconds (default: 0)
- `--rs485-delay-after <us>` - Delay after send in microseconds (default: 0)

**Scan Options:**

- `--size <n>` - Window size, i.e. number of registers to read in each window (default: 10)
- `--step <n>` - Step size, i.e. how many registers to advance the window (default: same as size)
- `--partial <true|false>` - Read partial windows at the end (default: true)

## Architecture

### Dependencies

- [**digitalpetri/modbus**](https://github.com/digitalpetri/modbus) - Modbus TCP and RTU
  client/server implementation
- [**picocli**](https://github.com/remkop/picocli) - Command-line interface framework
- [**jansi**](https://github.com/fusesource/jansi) - ANSI color support for terminal output

### GraalVM Configuration

The project uses:

1. **Picocli Codegen Annotation Processor** - Automatically generates reflection configuration for
   all `@Command`, `@Option`, and `@Parameters` annotated classes during compilation
2. **Native Maven Plugin** - Handles native image compilation with the `native` profile
3. **Custom Native Image Properties** - Additional configuration for Netty, SLF4J, Jansi, and
   jSerialComm initialization (JNI config, native library resources, runtime initialization)

Configuration files are located at:

- Auto-generated: `target/classes/META-INF/native-image/picocli-generated/`
- Custom: `src/main/resources/META-INF/native-image/com.kevinherron.modbus/modbus-cli/`

## Development

### Project Structure

```
modbus-cli/
├── src/main/java/com/kevinherron/modbus/cli/
│   ├── Modbus.java              # Main entry point
│   ├── ModbusCommand.java       # Root command with global options
│   ├── SerialPortOptions.java   # Serial port config mixin (baud, parity, RS-485)
│   ├── client/
│   │   ├── ClientCommand.java   # Client base command (TCP + RTU)
│   │   ├── Read*.java          # Read operations (read-coils, read-discrete-inputs, read-holding-registers, read-input-registers)
│   │   ├── Write*.java         # Write operations (write-single-coil, write-multiple-coils, write-single-register, write-multiple-registers, mask-write-register)
│   │   ├── ScanCommand.java    # Scan operation with sliding window
│   │   └── ReadWriteMultipleRegistersCommand.java  # read-write-multiple-registers operation
│   ├── server/
│   │   └── ServerCommand.java  # Test server (TCP + RTU)
│   ├── util/
│   │   └── EndpointParser.java # Parses tcp:/rtu: endpoint strings
│   └── output/
│       ├── OutputFormat.java    # Output format enum (HUMAN, JSON)
│       ├── OutputFormatter.java # Formatter interface
│       ├── HumanFormatter.java  # Human-readable table output
│       ├── JsonFormatter.java   # JSON output (NDJSON)
│       ├── OutputContext.java   # Output context interface
│       ├── DefaultOutputContext.java  # Default implementation
│       ├── OutputOptions.java   # Output configuration record
│       └── ...                  # Supporting classes
└── src/main/resources/META-INF/native-image/
    └── com.kevinherron.modbus/modbus-cli/
        ├── native-image.properties
        ├── jni-config.json      # JNI config for jSerialComm
        └── resource-config.json # Native lib resources for jSerialComm
```

### Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd modbus-cli

# Build JAR
mvn clean package

# Build native image (requires GraalVM)
mvn -Pnative clean package

# Run tests (if available)
mvn test
```

## JSON Output Format

The CLI supports JSON output via `--json` for machine parsing and automation.
See [README_JSON_FORMAT.md](README_JSON_FORMAT.md) for complete documentation.

### Record Kinds

- **result** - Register tables, coil tables, scan results
- **protocol** - Raw Modbus PDU messages (hex-encoded)
- **log** - Connection info, success, and warning messages
- **error** - Structured error objects

All JSON output is newline-delimited (NDJSON) for easy streaming and parsing.

## Exit Codes

The CLI uses stable, documented exit codes for automation:

| Code | Meaning                    | Example                                   |
|------|----------------------------|-------------------------------------------|
| `0`  | Success                    | Command completed normally.               |
| `1`  | General failure (fallback) | Unclassified Modbus error.                |
| `2`  | CLI usage error            | Invalid arguments, missing parameters.    |
| `3`  | Connection/Setup failure   | Cannot connect to host, serial port busy. |
| `4`  | Modbus protocol failure    | Modbus exception response, CRC error.     |
| `5`  | Timeout or interrupted     | Request timed out, operation interrupted. |
| `10` | Internal error             | Unexpected exception, bug.                |

## Agent Skill

This project includes an [Agent Skill](https://agentskills.io/home) that lets AI coding agents
interact with Modbus devices on your behalf. When the skill is installed, an agent can read
registers, write coils, scan address ranges, or start a test server using natural language.

The skill is located in [`skills/modbus-cli/`](skills/modbus-cli/) and requires the `modbus`
executable to be on your `$PATH`.

Example interactions:

- *"Start a Modbus test server on port 502 and read the first 10 holding registers"*
- *"Scan Modbus registers 0–100 on 192.168.1.50 and tell me which ones have non-zero values"*
- *"Write 1500 to Modbus holding register 40 on 10.0.0.1, then read it back to confirm"*
- *"Read Modbus coils 0–15 on the RTU device at /dev/ttyUSB0 with baud 19200 and summarize which are
  on"*
- *"Poll Modbus input registers 0–4 on localhost port 502 every second for 10 iterations, then
  chart the trend"*

## License

See [LICENSE.md](LICENSE.md) for details.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
