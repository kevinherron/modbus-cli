# Project Context

**Tech Stack:** Java 25, Maven, GraalVM Native Image

Modbus CLI is a command-line interface for Modbus TCP operations. It provides client commands
for reading/writing coils and registers, plus a test server. The CLI is built with picocli and
can be compiled to a native executable using GraalVM.

**Architecture:**

- **client/**: Modbus client commands (read/write coils, registers, scan)
- **server/**: Modbus test server
- **output/**: Output formatting (human-readable tables, JSON/NDJSON)
- **util/**: Utility classes

## Key Entry Points

- Main: `src/main/java/com/kevinherron/modbus/cli/Modbus.java`
- Root Command: `src/main/java/com/kevinherron/modbus/cli/ModbusCommand.java`
- Client Commands: `src/main/java/com/kevinherron/modbus/cli/client/`

## Building and Testing

| Command                      | Purpose                                    |
|------------------------------|--------------------------------------------|
| `mvn -q clean compile`       | Compile without tests                      |
| `mvn -q clean verify`        | Full build with tests and formatting check |
| `mvn -q spotless:apply`      | Fix code formatting (Google Java Format)   |
| `mvn -Pnative clean package` | Build native executable with GraalVM       |

Run specific tests:

```bash
mvn -q test -Dtest=ValueParserTest            # Run a single test class
mvn -q test -Dtest=*IT                        # Run integration tests
```

## Additional Resources

- Java coding conventions: `.agents/docs/java-coding-conventions.md`
- Testing guidelines: `.agents/docs/testing.md`
- Dependencies: `.agents/docs/dependencies.md`

## Verification

Use these steps to verify any completed work. Implementation plans should include these as success
criteria.

1. **Format and compile**:
    - `mvn -q spotless:apply` - Format code
    - `mvn -q clean compile` - Compile (skip tests)

2. **Run tests** (when applicable):
    - `mvn -q test` - Run unit tests
    - `mvn -q verify` - Run all tests including integration tests
