package com.kevinherron.modbus.cli.output;

import com.digitalpetri.modbus.exceptions.ModbusConnectException;
import com.digitalpetri.modbus.exceptions.ModbusCrcException;
import com.digitalpetri.modbus.exceptions.ModbusException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ParameterException;

/**
 * Classifies exceptions into structured error information for machine-readable output.
 *
 * <p>Maps exception types to stable symbolic error codes, categories, and retryability flags.
 */
public final class ErrorClassifier {

  private ErrorClassifier() {}

  /**
   * Classifies an exception into a structured error info record.
   *
   * @param exception the exception to classify.
   * @param message the human-readable error message.
   * @return the classified error information.
   */
  public static ErrorInfo classify(Exception exception, String message) {
    // Walk the cause chain to find the most specific classification.
    // Generic ModbusException is handled as a fallback after the full
    // chain has been examined, so it doesn't shadow more specific
    // inner causes like ConnectException.
    for (Throwable error = exception; error != null; error = error.getCause()) {
      ErrorInfo info = classifyDirect(error, message);
      if (info != null) {
        return info;
      }
    }

    for (Throwable error = exception; error != null; error = error.getCause()) {
      if (error instanceof ModbusException) {
        return new ErrorInfo("MODBUS_ERROR", "protocol", message, null);
      }
    }

    return new ErrorInfo("INTERNAL_ERROR", "internal", message, null);
  }

  private static @Nullable ErrorInfo classifyDirect(Throwable error, String message) {
    return switch (error) {
      case ParameterException _ -> new ErrorInfo("USAGE_ERROR", "usage", message, null);
      case IllegalArgumentException _ -> new ErrorInfo("INVALID_ARGUMENT", "usage", message, null);
      case ModbusResponseException mre -> {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("function_code", mre.getFunctionCode());
        details.put("exception_code", mre.getExceptionCode());
        yield new ErrorInfo("MODBUS_EXCEPTION_RESPONSE", "protocol", message, details);
      }
      case ModbusCrcException _ -> new ErrorInfo("MODBUS_CRC_ERROR", "protocol", message, null);
      case ModbusConnectException _ ->
          new ErrorInfo("CONNECTION_FAILED", "connection", message, null);
      case ConnectException _ -> new ErrorInfo("CONNECTION_REFUSED", "connection", message, null);
      case NoRouteToHostException _ ->
          new ErrorInfo("NO_ROUTE_TO_HOST", "connection", message, null);
      case UnknownHostException _ -> new ErrorInfo("UNKNOWN_HOST", "connection", message, null);
      case ModbusTimeoutException _ -> new ErrorInfo("TIMEOUT", "timeout", message, null);
      case SocketTimeoutException _ -> new ErrorInfo("TIMEOUT", "timeout", message, null);
      case TimeoutException _ -> new ErrorInfo("TIMEOUT", "timeout", message, null);
      case InterruptedException _ -> new ErrorInfo("INTERRUPTED", "timeout", message, null);
      default -> null;
    };
  }

  /**
   * Structured error information for machine-readable output.
   *
   * @param code stable symbolic error identifier.
   * @param category error category: usage, connection, protocol, timeout, or internal.
   * @param message human-readable error summary.
   * @param details optional command-specific context, or null.
   */
  public record ErrorInfo(
      String code, String category, String message, @Nullable Map<String, Object> details) {}
}
