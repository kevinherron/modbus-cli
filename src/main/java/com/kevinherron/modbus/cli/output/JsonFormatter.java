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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Formats output as JSON for machine parsing. */
public class JsonFormatter implements OutputFormatter {

  private final String invocationId = UUID.randomUUID().toString();
  private final AtomicInteger sequence = new AtomicInteger(0);

  private @Nullable String command = null;
  private @Nullable Integer currentIteration = null;

  @Override
  public void setIteration(Integer iteration) {
    this.currentIteration = iteration;
  }

  @Override
  public void setCommand(@Nullable String command) {
    this.command = command;
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

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("type", "protocol");
    data.put("direction", direction.name().toLowerCase());
    if (functionCode != -1) {
      data.put("function_code", functionCode);
    }
    data.put("pdu", pduHex);

    Instant ts = timestamp != null ? timestamp : Instant.now();
    out.println(toJson(buildEnvelope("event", ts, data, null)));
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

    Instant ts = Instant.now();

    if (type == OutputType.ERROR) {
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("code", "UNKNOWN_ERROR");
      error.put("category", "internal");
      error.put("message", message);
      out.println(toJson(buildEnvelope("error", ts, null, error)));
    } else {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("type", type.name().toLowerCase());
      data.put("message", message);
      out.println(toJson(buildEnvelope("event", ts, data, null)));
    }
  }

  @Override
  public void formatError(
      PrintStream out, Exception exception, String message, OutputOptions options) {

    Instant ts = Instant.now();

    ErrorClassifier.ErrorInfo info = ErrorClassifier.classify(exception, message);

    Map<String, Object> error = new LinkedHashMap<>();
    error.put("code", info.code());
    error.put("category", info.category());
    error.put("message", info.message());
    if (info.details() != null) {
      error.put("details", info.details());
    }

    out.println(toJson(buildEnvelope("error", ts, null, error)));
  }

  @Override
  public void formatRegisterTable(
      PrintStream out,
      byte[] registers,
      int startAddress,
      @Nullable Instant timestamp,
      OutputOptions options) {

    int quantity = registers.length / 2;

    // Convert byte pairs to unsigned 16-bit register values
    List<Integer> registerValues = new ArrayList<>();
    for (int i = 0; i < registers.length; i += 2) {
      registerValues.add(((registers[i] & 0xFF) << 8) | (registers[i + 1] & 0xFF));
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("type", "register_table");
    data.put("start_address", startAddress);
    data.put("quantity", quantity);
    data.put("bytes", bytesToHex(registers));
    data.put("registers", registerValues);

    Instant ts = timestamp != null ? timestamp : Instant.now();
    out.println(toJson(buildEnvelope("result", ts, data, null)));
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
    List<Boolean> coils = new ArrayList<>();
    for (int i = 0; i < quantity; i++) {
      int byteIndex = i / 8;
      int bitIndex = i % 8;
      coils.add((coilBytes[byteIndex] & (1 << bitIndex)) != 0);
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("type", "coil_table");
    data.put("start_address", startAddress);
    data.put("quantity", quantity);
    data.put("bytes", bytesToHex(coilBytes));
    data.put("coils", coils);

    Instant ts = timestamp != null ? timestamp : Instant.now();
    out.println(toJson(buildEnvelope("result", ts, data, null)));
  }

  @Override
  public void formatScanResults(PrintStream out, List<ScanResult> results, OutputOptions options) {
    if (results == null || results.isEmpty()) {
      return;
    }

    List<Map<String, Object>> windows = new ArrayList<>();

    for (ScanResult result : results) {
      byte[] registers = result.registers();
      int quantity = registers.length / 2;

      List<Integer> registerValues = new ArrayList<>();
      for (int i = 0; i < registers.length; i += 2) {
        registerValues.add(((registers[i] & 0xFF) << 8) | (registers[i + 1] & 0xFF));
      }

      Map<String, Object> window = new LinkedHashMap<>();
      window.put("start_address", result.address());
      window.put("quantity", quantity);
      window.put("bytes", bytesToHex(registers));
      window.put("registers", registerValues);
      windows.add(window);
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("type", "scan_results");
    data.put("windows", windows);

    Instant ts = Instant.now();
    out.println(toJson(buildEnvelope("result", ts, data, null)));
  }

  /**
   * Builds the common JSON envelope structure.
   *
   * @param kind the record kind: "result", "event", "error", or "summary".
   * @param timestamp the timestamp for this record.
   * @param data the data payload, or null if not applicable.
   * @param error the error payload, or null if not applicable.
   * @return the envelope as an ordered map.
   */
  private Map<String, Object> buildEnvelope(
      String kind,
      Instant timestamp,
      @Nullable Map<String, Object> data,
      @Nullable Map<String, Object> error) {

    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("schema_version", "2.0");
    envelope.put("kind", kind);
    if (command != null) {
      envelope.put("command", command);
    }

    Map<String, Object> invocation = new LinkedHashMap<>();
    invocation.put("id", invocationId);
    invocation.put("sequence", sequence.incrementAndGet());
    if (currentIteration != null) {
      invocation.put("iteration", currentIteration);
    }
    envelope.put("invocation", invocation);

    envelope.put("timestamp", timestamp.toString());

    if (data != null) {
      envelope.put("data", data);
    }
    if (error != null) {
      envelope.put("error", error);
    }

    return envelope;
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
          .map(e -> "\"" + escapeJson(String.valueOf(e.getKey())) + "\":" + toJson(e.getValue()))
          .collect(Collectors.joining(",", "{", "}"));
    } else if (obj instanceof List<?> list) {
      return list.stream().map(this::toJson).collect(Collectors.joining(",", "[", "]"));
    } else {
      return "\"" + escapeJson(obj.toString()) + "\"";
    }
  }

  private String escapeJson(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' -> sb.append("\\\\");
        case '"' -> sb.append("\\\"");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append("\\u%04x".formatted((int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }

  private static String bytesToHex(byte[] bytes) {
    return ByteBufUtil.hexDump(Unpooled.wrappedBuffer(bytes));
  }
}
