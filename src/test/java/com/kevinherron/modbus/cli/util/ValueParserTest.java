package com.kevinherron.modbus.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValueParserTest {

  @Test
  void parseCoilValue_true() {
    assertTrue(ValueParser.parseCoilValue("true"));
    assertTrue(ValueParser.parseCoilValue("TRUE"));
    assertTrue(ValueParser.parseCoilValue("True"));
    assertTrue(ValueParser.parseCoilValue("1"));
    assertTrue(ValueParser.parseCoilValue("on"));
    assertTrue(ValueParser.parseCoilValue("ON"));
    assertTrue(ValueParser.parseCoilValue("On"));
  }

  @Test
  void parseCoilValue_false() {
    assertFalse(ValueParser.parseCoilValue("false"));
    assertFalse(ValueParser.parseCoilValue("FALSE"));
    assertFalse(ValueParser.parseCoilValue("False"));
    assertFalse(ValueParser.parseCoilValue("0"));
    assertFalse(ValueParser.parseCoilValue("off"));
    assertFalse(ValueParser.parseCoilValue("OFF"));
    assertFalse(ValueParser.parseCoilValue("Off"));
  }

  @Test
  void parseCoilValue_withWhitespace() {
    assertTrue(ValueParser.parseCoilValue("  true  "));
    assertFalse(ValueParser.parseCoilValue("  false  "));
    assertTrue(ValueParser.parseCoilValue("\t1\t"));
    assertFalse(ValueParser.parseCoilValue(" 0 "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"yes", "no", "2", "invalid", "", "truee", "falsse"})
  void parseCoilValue_invalid(String value) {
    var exception =
        assertThrows(IllegalArgumentException.class, () -> ValueParser.parseCoilValue(value));
    assertTrue(exception.getMessage().contains("Invalid coil value"));
  }

  @Test
  void parseRegisterValue_decimal() {
    assertEquals(0, ValueParser.parseRegisterValue("0"));
    assertEquals(1234, ValueParser.parseRegisterValue("1234"));
    assertEquals(65535, ValueParser.parseRegisterValue("65535"));
    assertEquals(-1, ValueParser.parseRegisterValue("-1"));
  }

  @Test
  void parseRegisterValue_hexWithPrefix() {
    assertEquals(0, ValueParser.parseRegisterValue("0x0"));
    assertEquals(0x1234, ValueParser.parseRegisterValue("0x1234"));
    assertEquals(0x04D2, ValueParser.parseRegisterValue("0x04D2"));
    assertEquals(0xFFFF, ValueParser.parseRegisterValue("0xFFFF"));
    assertEquals(0xABCD, ValueParser.parseRegisterValue("0xabcd"));
    assertEquals(0xABCD, ValueParser.parseRegisterValue("0xABCD"));
    assertEquals(0xABCD, ValueParser.parseRegisterValue("0XAbCd"));
  }

  @Test
  void parseRegisterValue_withWhitespace() {
    assertEquals(1234, ValueParser.parseRegisterValue("  1234  "));
    assertEquals(0x1234, ValueParser.parseRegisterValue("  0x1234  "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "0xGGGG", "123abc", "  ", "0x", "x123"})
  void parseRegisterValue_invalid(String value) {
    var exception =
        assertThrows(IllegalArgumentException.class, () -> ValueParser.parseRegisterValue(value));
    assertTrue(exception.getMessage().contains("Invalid value"));
  }

  @Test
  void parseHexValue_withPrefix() {
    assertEquals(0, ValueParser.parseHexValue("0x0"));
    assertEquals(0x1234, ValueParser.parseHexValue("0x1234"));
    assertEquals(0xFFFF, ValueParser.parseHexValue("0xFFFF"));
    assertEquals(0xABCD, ValueParser.parseHexValue("0xabcd"));
    assertEquals(0xABCD, ValueParser.parseHexValue("0xABCD"));
    assertEquals(0xABCD, ValueParser.parseHexValue("0XAbCd"));
  }

  @Test
  void parseHexValue_withoutPrefix() {
    assertEquals(0, ValueParser.parseHexValue("0"));
    assertEquals(0x1234, ValueParser.parseHexValue("1234"));
    assertEquals(0xFFFF, ValueParser.parseHexValue("FFFF"));
    assertEquals(0xABCD, ValueParser.parseHexValue("abcd"));
    assertEquals(0xABCD, ValueParser.parseHexValue("ABCD"));
  }

  @Test
  void parseHexValue_withWhitespace() {
    assertEquals(0x1234, ValueParser.parseHexValue("  1234  "));
    assertEquals(0x1234, ValueParser.parseHexValue("  0x1234  "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "GGGG", "0xGGGG", "  ", "0x", "xyz"})
  void parseHexValue_invalid(String value) {
    var exception =
        assertThrows(IllegalArgumentException.class, () -> ValueParser.parseHexValue(value));
    assertTrue(exception.getMessage().contains("Invalid hex value"));
  }

  @Test
  void parseRegisterValue_boundaryValues() {
    // Test 16-bit unsigned range
    assertEquals(0, ValueParser.parseRegisterValue("0"));
    assertEquals(65535, ValueParser.parseRegisterValue("65535"));
    assertEquals(0xFFFF, ValueParser.parseRegisterValue("0xFFFF"));

    // Test negative values (will wrap in 16-bit representation)
    assertEquals(-1, ValueParser.parseRegisterValue("-1"));
    assertEquals(-32768, ValueParser.parseRegisterValue("-32768"));
  }

  @Test
  void parseHexValue_fourDigitValues() {
    // Common 4-digit hex patterns in Modbus
    assertEquals(0x0000, ValueParser.parseHexValue("0000"));
    assertEquals(0x8000, ValueParser.parseHexValue("8000"));
    assertEquals(0xFFFF, ValueParser.parseHexValue("FFFF"));
    assertEquals(0x1234, ValueParser.parseHexValue("1234"));
    assertEquals(0xABCD, ValueParser.parseHexValue("ABCD"));
  }
}
