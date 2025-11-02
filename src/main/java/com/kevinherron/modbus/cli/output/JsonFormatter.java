package com.kevinherron.modbus.cli.output;

import com.digitalpetri.modbus.ModbusPduSerializer.DefaultRequestSerializer;
import com.digitalpetri.modbus.ModbusPduSerializer.DefaultResponseSerializer;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.pdu.ModbusPdu;
import com.digitalpetri.modbus.pdu.ModbusRequestPdu;
import com.digitalpetri.modbus.pdu.ModbusResponsePdu;
import com.kevinherron.modbus.cli.client.ScanCommand.ScanResult;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Formats output as JSON for machine parsing. */
public class JsonFormatter implements OutputFormatter {

  private Integer currentIteration = null;

  @Override
  public void setIteration(Integer iteration) {
    this.currentIteration = iteration;
  }

  @Override
  public void formatProtocol(
      PrintStream out,
      ModbusPdu pdu,
      Direction direction,
      @Nullable Instant timestamp,
      OutputOptions options) {

    if (options.quiet()) {
      return;
    }

    // Encode PDU to hex bytes
    String pduHex = encodePduToHex(pdu);
    int functionCode = pdu.getFunctionCode();

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("timestamp", (timestamp != null ? timestamp : Instant.now()).toString());
    if (currentIteration != null) {
      json.put("iteration", currentIteration);
    }
    json.put("type", "protocol");
    json.put("direction", direction.name().toLowerCase());
    if (functionCode != -1) {
      json.put("function_code", functionCode);
    }
    json.put("pdu", pduHex);

    out.println(toJson(json));
  }

  private String encodePduToHex(ModbusPdu pdu) {
    if (pdu instanceof ModbusRequestPdu request) {
      try {
        var buffer = ByteBuffer.allocate(256);
        DefaultRequestSerializer.INSTANCE.encode(request, buffer);
        return ByteBufUtil.hexDump(Unpooled.wrappedBuffer(buffer.flip()));
      } catch (ModbusException e) {
        return pdu.toString();
      }
    } else if (pdu instanceof ModbusResponsePdu response) {
      try {
        var buffer = ByteBuffer.allocate(256);
        DefaultResponseSerializer.INSTANCE.encode(response, buffer);
        return ByteBufUtil.hexDump(Unpooled.wrappedBuffer(buffer.flip()));
      } catch (ModbusException e) {
        return pdu.toString();
      }
    } else {
      return pdu.toString();
    }
  }

  @Override
  public void formatMessage(
      PrintStream out, OutputType type, String message, OutputOptions options) {

    if (options.quiet() && type == OutputType.INFO) {
      return;
    }

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("timestamp", Instant.now().toString());
    if (currentIteration != null) {
      json.put("iteration", currentIteration);
    }
    json.put("type", type.name().toLowerCase());
    json.put("message", message);
    out.println(toJson(json));
  }

  @Override
  public void formatRegisterTable(
      PrintStream out,
      byte[] registers,
      int startAddress,
      @Nullable Instant timestamp,
      OutputOptions options) {

    // Convert byte array to a list of integers
    List<Integer> bytes = new ArrayList<>();
    for (byte b : registers) {
      bytes.add(b & 0xFF);
    }

    int quantity = registers.length / 2;

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("timestamp", (timestamp != null ? timestamp : Instant.now()).toString());
    if (currentIteration != null) {
      json.put("iteration", currentIteration);
    }
    json.put("type", "register_table");
    json.put("start_address", startAddress);
    json.put("quantity", quantity);
    json.put("data", bytes);
    out.println(toJson(json));
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
    List<Boolean> bits = new ArrayList<>();
    for (int i = 0; i < quantity; i++) {
      int byteIndex = i / 8;
      int bitIndex = i % 8;
      bits.add((coilBytes[byteIndex] & (1 << bitIndex)) != 0);
    }

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("timestamp", (timestamp != null ? timestamp : Instant.now()).toString());
    if (currentIteration != null) {
      json.put("iteration", currentIteration);
    }
    json.put("type", "coil_table");
    json.put("start_address", startAddress);
    json.put("quantity", quantity);
    json.put("data", bits);
    out.println(toJson(json));
  }

  @Override
  public void formatScanResults(PrintStream out, List<ScanResult> results, OutputOptions options) {
    if (results == null || results.isEmpty()) {
      return;
    }

    // map of register address to one or more 2-byte register values
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

    // Build JSON structure
    List<Map<String, Object>> resultsList = new ArrayList<>();
    for (int address : sortedAddresses) {
      List<byte[]> entries = scanResultMap.get(address);

      // Convert byte arrays to lists of integers
      List<List<Integer>> values =
          entries.stream().map(bytes -> List.of(bytes[0] & 0xFF, bytes[1] & 0xFF)).toList();

      boolean identical = areAllEqual(entries);

      Map<String, Object> resultEntry = new LinkedHashMap<>();
      resultEntry.put("address", address);
      resultEntry.put("values", values);
      resultEntry.put("identical", identical);
      resultsList.add(resultEntry);
    }

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("timestamp", Instant.now().toString());
    if (currentIteration != null) {
      json.put("iteration", currentIteration);
    }
    json.put("type", "scan_results");
    json.put("results", resultsList);
    out.println(toJson(json));
  }

  /**
   * Simple JSON serialization for basic Java objects. Handles Map, List, String, Number, Boolean,
   * null.
   */
  private String toJson(Object obj) {
    if (obj == null) {
      return "null";
    } else if (obj instanceof String s) {
      return "\"" + escapeJson(s) + "\"";
    } else if (obj instanceof Number || obj instanceof Boolean) {
      return obj.toString();
    } else if (obj instanceof Map<?, ?> map) {
      return map.entrySet().stream()
          .map(e -> "\"" + e.getKey() + "\":" + toJson(e.getValue()))
          .collect(Collectors.joining(",", "{", "}"));
    } else if (obj instanceof List<?> list) {
      return list.stream().map(this::toJson).collect(Collectors.joining(",", "[", "]"));
    } else {
      return "\"" + escapeJson(obj.toString()) + "\"";
    }
  }

  private String escapeJson(String s) {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
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
