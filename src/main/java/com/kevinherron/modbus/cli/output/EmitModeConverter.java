package com.kevinherron.modbus.cli.output;

import java.util.Arrays;
import java.util.stream.Collectors;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/** Case-insensitive converter for EmitMode enum. */
public class EmitModeConverter implements ITypeConverter<EmitMode> {
  @Override
  public EmitMode convert(String value) {
    try {
      return EmitMode.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      String validValues =
          Arrays.stream(EmitMode.values())
              .map(m -> m.name().toLowerCase())
              .collect(Collectors.joining(", "));
      throw new TypeConversionException(
          "expected one of [%s] (case-insensitive) but was '%s'".formatted(validValues, value));
    }
  }
}
