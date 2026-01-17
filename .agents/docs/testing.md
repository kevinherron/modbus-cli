# Testing Guide

This project uses JUnit 5 for testing. Tests are organized into:

- **Unit tests** (`*Test.java`): Fast, isolated tests for individual components
- **Integration tests** (`*IT.java`): Tests that run against a test Modbus server

## Running Tests

### Run All Tests

```bash
mvn -q clean verify
```

### Run Only Unit Tests

```bash
mvn -q test -Dtest='*Test'
```

### Run Only Integration Tests

```bash
mvn -q test -Dtest='*IT'
```

### Run a Specific Test Class

```bash
mvn -q test -Dtest=ValueParserTest
```

### Run a Specific Test Method

```bash
mvn -q test -Dtest=ValueParserTest#parseCoilValue_true
```

### Run Tests Matching a Pattern

```bash
mvn -q test -Dtest='*Parser*'
```

## Test Locations

- Unit tests: `src/test/java/com/kevinherron/modbus/cli/util/`
- Integration tests: `src/test/java/com/kevinherron/modbus/cli/client/`
- Test utilities: `src/test/java/com/kevinherron/modbus/cli/test/`
