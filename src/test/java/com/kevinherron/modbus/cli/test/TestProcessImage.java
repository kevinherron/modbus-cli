package com.kevinherron.modbus.cli.test;

import com.digitalpetri.modbus.server.ProcessImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A test-friendly extension of {@link ProcessImage} that provides convenient methods for setting
 * values during tests.
 *
 * <p>This class allows tests to easily configure specific register and coil values without dealing
 * with the transaction API directly.
 */
public class TestProcessImage extends ProcessImage {

  /**
   * Set the value of a holding register at the specified address.
   *
   * @param address the register address.
   * @param value the 16-bit unsigned value to set (0-65535).
   */
  public void setHoldingRegister(int address, int value) {
    with(tx -> tx.writeHoldingRegisters(registers -> registers.put(address, toByteArray(value))));
  }

  /**
   * Set multiple holding registers starting at the specified address.
   *
   * @param address the starting register address.
   * @param values the 16-bit unsigned values to set.
   */
  public void setHoldingRegisters(int address, int... values) {
    with(
        tx ->
            tx.writeHoldingRegisters(
                registers -> {
                  for (int i = 0; i < values.length; i++) {
                    registers.put(address + i, toByteArray(values[i]));
                  }
                }));
  }

  /**
   * Set the value of an input register at the specified address.
   *
   * @param address the register address.
   * @param value the 16-bit unsigned value to set (0-65535).
   */
  public void setInputRegister(int address, int value) {
    with(tx -> tx.writeInputRegisters(registers -> registers.put(address, toByteArray(value))));
  }

  /**
   * Set multiple input registers starting at the specified address.
   *
   * @param address the starting register address.
   * @param values the 16-bit unsigned values to set.
   */
  public void setInputRegisters(int address, int... values) {
    with(
        tx ->
            tx.writeInputRegisters(
                registers -> {
                  for (int i = 0; i < values.length; i++) {
                    registers.put(address + i, toByteArray(values[i]));
                  }
                }));
  }

  /**
   * Set the value of a coil at the specified address.
   *
   * @param address the coil address.
   * @param value the boolean value to set.
   */
  public void setCoil(int address, boolean value) {
    with(tx -> tx.writeCoils(coils -> coils.put(address, value)));
  }

  /**
   * Set multiple coils starting at the specified address.
   *
   * @param address the starting coil address.
   * @param values the boolean values to set.
   */
  public void setCoils(int address, boolean... values) {
    with(
        tx ->
            tx.writeCoils(
                coils -> {
                  for (int i = 0; i < values.length; i++) {
                    coils.put(address + i, values[i]);
                  }
                }));
  }

  /**
   * Set the value of a discrete input at the specified address.
   *
   * @param address the discrete input address.
   * @param value the boolean value to set.
   */
  public void setDiscreteInput(int address, boolean value) {
    with(tx -> tx.writeDiscreteInputs(inputs -> inputs.put(address, value)));
  }

  /**
   * Set multiple discrete inputs starting at the specified address.
   *
   * @param address the starting discrete input address.
   * @param values the boolean values to set.
   */
  public void setDiscreteInputs(int address, boolean... values) {
    with(
        tx ->
            tx.writeDiscreteInputs(
                inputs -> {
                  for (int i = 0; i < values.length; i++) {
                    inputs.put(address + i, values[i]);
                  }
                }));
  }

  /**
   * Get the value of a holding register at the specified address.
   *
   * @param address the register address.
   * @return the 16-bit unsigned value, or 0 if not set.
   */
  public int getHoldingRegister(int address) {
    return get(
        tx ->
            tx.readHoldingRegisters(
                registers -> {
                  byte[] bytes = registers.get(address);
                  return bytes != null ? fromByteArray(bytes) : 0;
                }));
  }

  /**
   * Get the value of an input register at the specified address.
   *
   * @param address the register address.
   * @return the 16-bit unsigned value, or 0 if not set.
   */
  public int getInputRegister(int address) {
    return get(
        tx ->
            tx.readInputRegisters(
                registers -> {
                  byte[] bytes = registers.get(address);
                  return bytes != null ? fromByteArray(bytes) : 0;
                }));
  }

  /**
   * Get the value of a coil at the specified address.
   *
   * @param address the coil address.
   * @return the boolean value, or false if not set.
   */
  public boolean getCoil(int address) {
    return get(tx -> tx.readCoils(coils -> coils.getOrDefault(address, false)));
  }

  /**
   * Get the value of a discrete input at the specified address.
   *
   * @param address the discrete input address.
   * @return the boolean value, or false if not set.
   */
  public boolean getDiscreteInput(int address) {
    return get(tx -> tx.readDiscreteInputs(inputs -> inputs.getOrDefault(address, false)));
  }

  private static byte[] toByteArray(int value) {
    return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short) value).array();
  }

  private static int fromByteArray(byte[] bytes) {
    return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort() & 0xFFFF;
  }
}
