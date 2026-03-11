# scan (FC 03)

Sliding window scan over a range of holding register addresses. Reads `--size` registers at each
step, advancing by `--step` addresses. Returns a `scan_results` result with all windows.

## Usage

```
modbus --format json client <endpoint> [client-options] scan <start> <end> [options]
```

| Argument  | Description                  |
|-----------|------------------------------|
| `<start>` | Starting address (inclusive) |
| `<end>`   | Ending address (exclusive)   |

## Options

| Option      | Description                                | Default |
|-------------|--------------------------------------------|---------|
| `--size`    | Registers per read (window size)           | 1       |
| `--step`    | Address advance per iteration              | 1       |
| `--partial` | Toggle partial windows (default: included) | true    |

**Note**: Specifying `--partial` on the command line toggles the default to false (skips partial
windows at range end). If `--step` < `--size`, windows overlap.

## Examples

Scan addresses 0-99 one register at a time (default size=1, step=1):

```bash
modbus --format json client localhost scan 0 100
```

Scan addresses 0-99 in windows of 10:

```bash
modbus --format json client localhost scan 0 100 --size 10 --step 10
```

Scan with overlapping windows (size 20, step 10):

```bash
modbus --format json client localhost scan 0 100 --size 20 --step 10
```

Scan a large range with bigger windows:

```bash
modbus --format json client localhost scan 0 1000 --size 50 --step 50
```
