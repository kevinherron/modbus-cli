# Modbus CLI

A command-line interface for Modbus TCP operations, built with Java 25 and compilable to a native
executable using GraalVM.

## Quick Examples

**Start a test server:**

```bash
$ modbus server
Modbus server started on 0.0.0.0:502
```

The server initializes with 65536 holding registers, each pre-populated with its address as a value.
Leave this running in one terminal while trying the client examples below in another.

**Read holding registers:**

```bash
$ modbus client localhost rhr 0 10
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
$ modbus --format=json client localhost rhr 0 10
{"timestamp":"2025-11-02T23:07:57.618695Z","type":"info","message":"Hostname: localhost:502, Unit ID: 1"}
{"timestamp":"2025-11-02T23:07:57.627904Z","type":"register_table","start_address":0,"quantity":10,"data":[0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,0,9]}
```

**Write then read back a register:**

```bash
$ modbus --format=json client localhost wsr 100 42
{"timestamp":"2025-11-02T23:08:09.316542Z","type":"protocol","direction":"received","function_code":6,"pdu":"060064002a"}

$ modbus --format=json client localhost rhr 100 1
{"timestamp":"2025-11-02T23:08:09.332309Z","type":"register_table","start_address":100,"quantity":1,"data":[0,42]}
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
$ modbus --format=json client localhost rhr 0 10 -c 5 | jq -c 'select(.type == "register_table") | {timestamp, data}'
{"timestamp":"2025-11-02T23:12:37.072923Z","data":[0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,0,9]}
{"timestamp":"2025-11-02T23:12:38.075708Z","data":[0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,0,9]}
{"timestamp":"2025-11-02T23:12:39.081328Z","data":[0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,0,9]}
{"timestamp":"2025-11-02T23:12:40.086801Z","data":[0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,0,9]}
{"timestamp":"2025-11-02T23:12:41.088714Z","data":[0,0,0,1,0,2,0,3,0,4,0,5,0,6,0,7,0,8,0,9]}
```

## Features

- **Client Commands**: Read/write coils, discrete inputs, holding registers, and input registers
- **Modbus TCP Protocol**: Full support for standard Modbus function codes
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
modbus [global-options] client <hostname> [client-options] <subcommand> [subcommand-options]
```

### Available Subcommands

#### Read Operations

- `rc <address> <quantity>` - Read coils (FC 01)
- `rdi <address> <quantity>` - Read discrete inputs (FC 02)
- `rhr <address> <quantity>` - Read holding registers (FC 03)
- `rir <address> <quantity>` - Read input registers (FC 04)

#### Write Operations

- `wsc <address> <value>` - Write single coil (FC 05)
- `wmc <address> <values...>` - Write multiple coils (FC 15)
- `wsr <address> <value>` - Write single register (FC 06)
- `wmr <address> <values...>` - Write multiple registers (FC 16)
- `mwr <address> <and-mask> <or-mask>` - Mask write register (FC 22)
- `rwmr <read-addr> <read-qty> <write-addr> <values...>` - Read/Write multiple registers (FC 23)

#### Other

- `scan <start> <end>` - Scan a range of registers using a sliding window

### Options

**Global Options:**

- `--format <format>` - Output format: `human` (default), `json`
- `-v, --verbose` - Verbose mode - detailed output
- `-q, --quiet` - Quiet mode - minimal output
- `--no-color` - Disable ANSI color output

**Client Options:**

- `-p, --port <port>` - Port number (default: 502)
- `--unit-id <id>` - Unit/slave ID (default: 1)
- `-t, --timeout <ms>` - Request timeout in milliseconds (default: 5000)

**Scan Options:**

- `--size <n>` - Window size, i.e. number of registers to read in each window (default: 10)
- `--step <n>` - Step size, i.e. how many registers to advance the window (default: same as size)
- `--partial <true|false>` - Read partial windows at the end (default: true)

## Architecture

### Dependencies

- [**digitalpetri/modbus**](https://github.com/digitalpetri/modbus) - Modbus TCP client implementation
- [**picocli**](https://github.com/remkop/picocli) - Command-line interface framework
- [**jansi**](https://github.com/fusesource/jansi) - ANSI color support for terminal output

### GraalVM Configuration

The project uses:

1. **Picocli Codegen Annotation Processor** - Automatically generates reflection configuration for
   all `@Command`, `@Option`, and `@Parameters` annotated classes during compilation
2. **Native Maven Plugin** - Handles native image compilation with the `native` profile
3. **Custom Native Image Properties** - Additional configuration for Netty, SLF4J, and Jansi
   initialization

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
│   ├── client/
│   │   ├── ClientCommand.java   # Client base command
│   │   ├── Read*.java          # Read operations (rc, rdi, rhr, rir)
│   │   ├── Write*.java         # Write operations (wsc, wmc, wsr, wmr, mwr)
│   │   ├── ScanCommand.java    # Scan operation with sliding window
│   │   └── ReadWriteMultipleRegistersCommand.java  # rwmr operation
│   ├── server/
│   │   └── ServerCommand.java  # Server command (future)
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
        └── native-image.properties
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

The CLI supports JSON output via `--format=json` for machine parsing and automation.
See [README_JSON_FORMAT.md](README_JSON_FORMAT.md) for complete documentation.

### Output Types

- **register_table** - Register read results (rhr, rir, rwmr)
- **coil_table** - Coil/discrete input results (rc, rdi)
- **scan_results** - Scan command results with overlap detection
- **protocol** - Raw Modbus PDU messages (hex-encoded)
- **info** - Connection and status messages
- **error** / **warning** - Diagnostic messages

All JSON output is newline-delimited (NDJSON) for easy streaming and parsing.

## License

See [LICENSE.md](LICENSE.md) for details.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
