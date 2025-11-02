# Modbus CLI

A command-line interface for Modbus TCP operations, built with Java 25 and compilable to a native
executable using GraalVM.

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

### Examples

#### Read 10 holding registers starting at address 0

```bash
# Using JAR
java -jar target/modbus-cli-0.1-SNAPSHOT.jar client 192.168.1.100 rhr 0 10

# Using native executable
./target/modbus client 192.168.1.100 rhr 0 10
```

#### Write a single coil at address 100

```bash
./target/modbus client 192.168.1.100 wsc 100 true
```

#### Read from a specific unit ID on non-standard port

```bash
./target/modbus client 192.168.1.100 -p 5020 --unit-id 5 rc 0 16
```

#### Write multiple registers

```bash
./target/modbus client 192.168.1.100 wmr 0 100 200 300 400
```

#### Scan a range of registers (0-99)

```bash
./target/modbus client 192.168.1.100 scan 0 100
```

#### Scan with custom window size and step

```bash
# Read 5 registers at a time, advancing by 3 each step
./target/modbus client 192.168.1.100 scan 0 50 --size=5 --step=3
```

#### Get JSON output for automation

```bash
# Human-readable (default)
./target/modbus client 192.168.1.100 rhr 0 10

# JSON format
./target/modbus --format=json client 192.168.1.100 rhr 0 10

# JSON with minimal output (data only)
./target/modbus --format=json --quiet client 192.168.1.100 rhr 0 10 | jq
```

## Performance Comparison

The native executable offers significantly faster startup times and lower memory usage compared to
running the JAR with a JVM, while maintaining similar execution performance for network I/O bound
operations.

## Architecture

### Dependencies

- **modbus-tcp** (2.1.0) - Modbus TCP client implementation with Netty 4.2.6
- **picocli** (4.7.6) - Command-line interface framework
- **picocli-codegen** (4.7.6) - Annotation processor for GraalVM compatibility
- **slf4j-simple** (2.0.16) - Logging implementation
- **jansi** (2.4.0) - ANSI color support for terminal output

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

### Troubleshooting Native Image Build

If you encounter issues:

1. **Missing reflection configuration**: The picocli-codegen annotation processor should handle this
   automatically. Verify it ran during compilation:
   ```bash
   mvn clean compile
   ls -la target/classes/META-INF/native-image/picocli-generated/
   ```

2. **Netty-related errors**: Netty includes native-image configuration. If issues occur, you may
   need to add runtime initialization:
   ```
   --initialize-at-run-time=io.netty.channel.epoll.Epoll
   ```

3. **Memory errors during build**: Increase Maven memory:
   ```bash
   export MAVEN_OPTS="-Xmx6g"
   mvn -Pnative clean package
   ```

4. **Platform-specific issues**: Some Netty features (epoll on Linux, kqueue on macOS) may need
   additional configuration.

## JSON Output Format

The CLI supports JSON output via `--format=json` for machine parsing and automation.
See [JSON_OUTPUT.md](README_JSON_FORMAT.md) for complete documentation.

### Quick Examples

```bash
# Get register data as JSON
modbus --format=json --quiet client localhost rhr 0 10 | jq

# Parse with jq - extract just the data array
modbus --format=json --quiet client localhost rhr 0 10 | jq '.data'

# Convert bytes to 16-bit integers
modbus --format=json --quiet client localhost rhr 0 5 | \
  jq '[.data | _nwise(2) | .[0] * 256 + .[1]]'
```

### Output Types

- **register_table** - Register read results (rhr, rir, rwmr)
- **coil_table** - Coil/discrete input results (rc, rdi)
- **scan_results** - Scan command results with overlap detection
- **protocol** - Raw Modbus PDU messages (hex-encoded)
- **info** - Connection and status messages
- **error** / **warning** - Diagnostic messages

All JSON output is newline-delimited (NDJSON) for easy streaming and parsing.

## License

See LICENSE file for details.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
