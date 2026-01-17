package com.kevinherron.modbus.cli.client;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterRequest;
import com.digitalpetri.modbus.pdu.MaskWriteRegisterResponse;
import com.kevinherron.modbus.cli.output.Direction;
import com.kevinherron.modbus.cli.output.OutputContext;
import com.kevinherron.modbus.cli.util.ValueParser;
import java.time.Instant;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Implements the Modbus function code 22 (Mask Write Register) operation.
 *
 * <p>This command modifies specific bits in a holding register without affecting other bits. The
 * operation applies an AND mask followed by an OR mask using the formula:
 *
 * <pre>Result = (CurrentValue AND andMask) OR (orMask AND (NOT andMask))</pre>
 *
 * <p>The AND mask preserves bits where the mask is 1 and clears bits where the mask is 0. The OR
 * mask sets bits where the mask is 1 (only in positions cleared by the AND mask).
 *
 * <p>Common use cases include:
 *
 * <ul>
 *   <li>Setting specific bits: use andMask=0xFFFF and orMask with target bits set
 *   <li>Clearing specific bits: use andMask with target bits cleared and orMask=0x0000
 *   <li>Toggling specific bits: combine `AND` and `OR` masks appropriately
 * </ul>
 *
 * <p>This command is invoked using {@code mwr} (e.g., {@code modbus client mwr 0 0xFF00 0x00FF}).
 *
 * @see WriteSingleRegisterCommand for writing an entire register value (function code 06)
 * @see WriteMultipleRegistersCommand for writing multiple registers (function code 16)
 */
@Command(name = "mwr", description = "Mask Write Register")
class MaskWriteRegisterCommand implements Runnable {

  /** The address of the register to modify. Addressing is typically 0-based. */
  @Parameters(index = "0", description = "register address")
  int address;

  /**
   * The AND mask for the operation. Bits set to 1 preserve the corresponding bits in the current
   * register value; bits set to 0 clear those bits. Accepts a hexadecimal format with or without
   * the {@code 0x} prefix (e.g., {@code 0xFFFF} or {@code FFFF}), parsed by {@link
   * ValueParser#parseHexValue(String)}.
   */
  @Parameters(index = "1", description = "AND mask (hex, e.g., 0xFFFF or FFFF)")
  String andMask;

  /**
   * The OR mask for the operation. Bits set to 1 will set the corresponding bits in positions that
   * were cleared by the AND mask. Accepts a hexadecimal format with or without the {@code 0x}
   * prefix (e.g., {@code 0x0000} or {@code 0000}), parsed by {@link
   * ValueParser#parseHexValue(String)}.
   */
  @Parameters(index = "2", description = "OR mask (hex, e.g., 0x0000 or 0000)")
  String orMask;

  @ParentCommand ClientCommand clientCommand;

  @Override
  public void run() {
    clientCommand.runWithClient(
        (ModbusTcpClient client, int unitId, OutputContext output) -> {
          int andMaskValue = ValueParser.parseHexValue(andMask);
          int orMaskValue = ValueParser.parseHexValue(orMask);

          var request = new MaskWriteRegisterRequest(address, andMaskValue, orMaskValue);

          output.protocol(request, Direction.SENT, null);

          MaskWriteRegisterResponse response = client.maskWriteRegister(unitId, request);
          Instant responseTime = Instant.now();

          output.protocol(response, Direction.RECEIVED, responseTime);
        });
  }
}
