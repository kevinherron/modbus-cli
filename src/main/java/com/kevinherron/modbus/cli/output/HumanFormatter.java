package com.kevinherron.modbus.cli.output;

import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.kevinherron.modbus.cli.client.ScanCommand.ScanResult;
import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.jspecify.annotations.Nullable;

/** Formats output for human-readable terminal display with ANSI colors. */
public class HumanFormatter implements OutputFormatter {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  private Integer currentIteration = null;

  @Override
  public void setIteration(Integer iteration) {
    this.currentIteration = iteration;
  }

  private String getTimestampPrefix(@Nullable Instant timestamp) {
    Instant effectiveTime = timestamp != null ? timestamp : Instant.now();
    String formattedTime = TIMESTAMP_FORMATTER.format(effectiveTime);
    if (currentIteration != null) {
      return String.format("[%s] [%d] ", formattedTime, currentIteration);
    }
    return "";
  }

  @Override
  public void formatProtocol(
      PrintStream out,
      ModbusPdu pdu,
      Direction direction,
      @Nullable Instant timestamp,
      OutputOptions options) {
    if (options.quiet()) {
      return; // Skip protocol messages in quiet mode
    }

    String arrow = direction.arrow();
    String prefix = getTimestampPrefix(timestamp);
    String formatted = prefix + arrow + " " + pdu;

    if (options.colorsEnabled()) {
      Color color = direction == Direction.SENT ? Color.CYAN : Color.GREEN;
      out.println(Ansi.ansi().fg(color).a(formatted).reset());
    } else {
      out.println(formatted);
    }
  }

  @Override
  public void formatMessage(
      PrintStream out, OutputType type, String message, OutputOptions options) {
    // Respect quiet mode for INFO messages only
    if (options.quiet() && type == OutputType.INFO) {
      return;
    }

    String prefix = getTimestampPrefix(null);
    String formatted = prefix + message;

    if (options.colorsEnabled()) {
      Color color = getColorForType(type);
      out.println(Ansi.ansi().fg(color).a(formatted).reset());
    } else {
      out.println(formatted);
    }
  }

  @Override
  public void formatRegisterTable(
      PrintStream out,
      byte[] registers,
      int startAddress,
      @Nullable Instant timestamp,
      OutputOptions options) {
    // Convert register address to byte offset
    int startByteOffset = startAddress * 2;
    int endByteOffset = startByteOffset + registers.length - 1;

    // Align to 16-byte boundaries
    int firstRowOffset = (startByteOffset / 16) * 16;
    int lastRowOffset = (endByteOffset / 16) * 16;

    // Print header with color
    String headerText = String.format("%-8s\t%s%n", "Offset (hex)", "Bytes (hex)");
    if (options.colorsEnabled()) {
      out.print(Ansi.ansi().fg(Color.BLUE).a(headerText).reset());
      out.print(Ansi.ansi().fg(Color.BLUE).a("-".repeat(headerText.length())).reset().a("\n"));
    } else {
      out.print(headerText);
      out.print("-".repeat(headerText.length()) + "\n");
    }

    for (int rowOffset = firstRowOffset; rowOffset <= lastRowOffset; rowOffset += 16) {
      // Print row offset with color
      if (options.colorsEnabled()) {
        out.print(Ansi.ansi().fg(Color.CYAN).a(String.format("%08X", rowOffset)).reset());
        out.print("\t");
      } else {
        out.printf("%08X\t", rowOffset);
      }

      for (int position = 0; position < 16; position++) {
        int absoluteByteOffset = rowOffset + position;
        if (absoluteByteOffset < startByteOffset || absoluteByteOffset > endByteOffset) {
          if (options.colorsEnabled()) {
            out.print(Ansi.ansi().fgBright(Color.BLACK).a(".. ").reset());
          } else {
            out.print(".. ");
          }
        } else {
          int arrayIndex = absoluteByteOffset - startByteOffset;
          if (options.colorsEnabled()) {
            out.print(
                Ansi.ansi()
                    .fg(Color.GREEN)
                    .a(String.format("%02X ", registers[arrayIndex] & 0xFF))
                    .reset());
          } else {
            out.printf("%02X ", registers[arrayIndex] & 0xFF);
          }
        }
      }
      out.println();
    }
  }

  @Override
  public void formatCoilTable(
      PrintStream out,
      byte[] coilBytes,
      int startAddress,
      int quantity,
      @Nullable Instant timestamp,
      OutputOptions options) {
    // Convert bytes to bits (LSB first per Modbus protocol)
    boolean[] bits = new boolean[quantity];
    for (int i = 0; i < quantity; i++) {
      int byteIndex = i / 8;
      int bitIndex = i % 8;
      bits[i] = (coilBytes[byteIndex] & (1 << bitIndex)) != 0;
    }

    // Calculate address range
    int endAddress = startAddress + quantity - 1;
    int firstRowAddress = (startAddress / 8) * 8;
    int lastRowAddress = (endAddress / 8) * 8;

    // Print header with color
    String headerText = String.format("%-10s %s%n", "Address", "Bits");
    if (options.colorsEnabled()) {
      out.print(Ansi.ansi().fg(Color.BLUE).a(headerText).reset());
      out.print(Ansi.ansi().fg(Color.BLUE).a("-".repeat(10) + " " + "-".repeat(15) + "\n").reset());
    } else {
      out.print(headerText);
      out.print("-".repeat(10) + " " + "-".repeat(15) + "\n");
    }

    // Print bits, 8 per row, aligned to multiples of 8
    for (int rowAddress = firstRowAddress; rowAddress <= lastRowAddress; rowAddress += 8) {
      // Print row address with color
      if (options.colorsEnabled()) {
        out.print(Ansi.ansi().fg(Color.CYAN).a(String.format("0x%04X     ", rowAddress)).reset());
      } else {
        out.printf("0x%04X     ", rowAddress);
      }

      for (int position = 0; position < 8; position++) {
        int absoluteAddress = rowAddress + position;
        if (absoluteAddress < startAddress || absoluteAddress > endAddress) {
          if (options.colorsEnabled()) {
            out.print(Ansi.ansi().fgBright(Color.BLACK).a(". ").reset());
          } else {
            out.print(". ");
          }
        } else {
          int bitIndex = absoluteAddress - startAddress;
          if (options.colorsEnabled()) {
            String bitValue = bits[bitIndex] ? "1 " : "0 ";
            Color bitColor = bits[bitIndex] ? Color.GREEN : Color.YELLOW;
            out.print(Ansi.ansi().fg(bitColor).a(bitValue).reset());
          } else {
            out.print(bits[bitIndex] ? "1 " : "0 ");
          }
        }
      }
      out.println();
    }
  }

  @Override
  public void formatScanResults(PrintStream out, List<ScanResult> results, OutputOptions options) {
    // Handle empty input
    if (results == null || results.isEmpty()) {
      return;
    }

    // map of register address to one or more 2-byte register values from a ScanResult
    Map<Integer, List<byte[]>> scanResultMap = new HashMap<>();

    for (ScanResult result : results) {
      int address = result.address();
      byte[] registers = result.registers();

      for (int i = 0; i < registers.length; i += 2) {
        int currentAddress = address + (i / 2);
        byte[] registerValue = new byte[] {registers[i], registers[i + 1]};

        scanResultMap.computeIfAbsent(currentAddress, _ -> new ArrayList<>()).add(registerValue);
      }
    }

    if (scanResultMap.isEmpty()) {
      return;
    }

    // Get all unique register addresses and sort them
    List<Integer> sortedAddresses = new ArrayList<>(scanResultMap.keySet());
    Collections.sort(sortedAddresses);

    // Print table header with color
    String headerText = String.format("%-8s\t%s%n", "Address", "Values (hex, 2 bytes each)");
    if (options.colorsEnabled()) {
      out.print(Ansi.ansi().fg(Color.BLUE).a(headerText).reset());
      out.println(Ansi.ansi().fg(Color.BLUE).a("-".repeat(55)).reset());
    } else {
      out.print(headerText);
      out.println("-".repeat(55));
    }

    // Define the number of 2-byte pairs to print per line
    int pairsPerLine = 8;
    int currentAddressIndex = 0;

    while (currentAddressIndex < sortedAddresses.size()) {
      int displayLineStartAddress =
          (sortedAddresses.get(currentAddressIndex) / pairsPerLine) * pairsPerLine;

      // Print the line start address with color
      if (options.colorsEnabled()) {
        out.print(
            Ansi.ansi()
                .fg(Color.CYAN)
                .a(String.format("%04X    \t", displayLineStartAddress))
                .reset());
      } else {
        out.printf("%04X    \t", displayLineStartAddress);
      }

      // Print values or placeholders for this line
      for (int j = 0; j < pairsPerLine; j++) {
        int targetAddress = displayLineStartAddress + j;

        if (targetAddress == sortedAddresses.get(currentAddressIndex)) {
          // Get the values for this address
          List<byte[]> entries = scanResultMap.get(targetAddress);
          byte[] valueToPrint = entries.getFirst();

          // Format the value as a 4-digit hex string
          String text = String.format("%02X%02X", valueToPrint[0] & 0xFF, valueToPrint[1] & 0xFF);

          // Determine color based on whether all values are equal
          if (entries.size() > 1) {
            boolean allEqual = areAllEqual(entries);
            Color color = allEqual ? Color.YELLOW : Color.RED;
            if (options.colorsEnabled()) {
              out.print(Ansi.ansi().fg(color).a(text).a(" ").reset());
            } else {
              out.print(text + " ");
            }
          } else {
            // Single scan value - use green
            if (options.colorsEnabled()) {
              out.print(Ansi.ansi().fg(Color.GREEN).a(text).a(" ").reset());
            } else {
              out.print(text + " ");
            }
          }

          // Move to the next address
          currentAddressIndex++;

          // Break if we've processed all addresses
          if (currentAddressIndex >= sortedAddresses.size()) {
            break;
          }
        } else {
          // Print placeholder for empty slot
          if (options.colorsEnabled()) {
            out.print(Ansi.ansi().fgBright(Color.BLACK).a(".... ").reset());
          } else {
            out.print(".... ");
          }
        }
      }

      // End the line
      out.println();
    }
  }

  private Color getColorForType(OutputType type) {
    return switch (type) {
      case ERROR -> Color.RED;
      case WARNING -> Color.YELLOW;
      case SUCCESS -> Color.GREEN;
      case INFO -> Color.BLUE;
      case DATA -> Color.DEFAULT;
      case PROTOCOL -> Color.CYAN;
    };
  }

  private static boolean areAllEqual(List<byte[]> list) {
    if (list == null || list.size() <= 1) {
      return true;
    }

    byte[] first = list.getFirst();
    for (int i = 1; i < list.size(); i++) {
      if (!Arrays.equals(first, list.get(i))) {
        return false;
      }
    }

    return true;
  }
}
