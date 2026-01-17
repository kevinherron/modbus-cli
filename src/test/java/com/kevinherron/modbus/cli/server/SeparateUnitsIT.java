package com.kevinherron.modbus.cli.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.digitalpetri.modbus.client.ModbusTcpClient;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.pdu.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.pdu.WriteSingleRegisterRequest;
import com.digitalpetri.modbus.tcp.client.NettyTcpClientTransport;
import com.kevinherron.modbus.cli.test.TestServerBuilder;
import org.junit.jupiter.api.Test;

public class SeparateUnitsIT {

  @Test
  void testSeparateUnitsIsolation() throws Exception {
    try (var server = new TestServerBuilder().withSeparateUnits(true).build()) {
      server.start();

      var client = createClient(server);
      client.connect();
      try {
        // Write value 0xBEEF to holding register 100 on unit ID 1
        client.writeSingleRegister(1, new WriteSingleRegisterRequest(100, 0xBEEF));

        // Read holding register 100 from unit ID 2 - should return default value (100)
        ReadHoldingRegistersResponse unit2Response =
            client.readHoldingRegisters(2, new ReadHoldingRegistersRequest(100, 1));
        byte[] unit2Registers = unit2Response.registers();
        int unit2Value = ((unit2Registers[0] & 0xFF) << 8) | (unit2Registers[1] & 0xFF);
        assertEquals(100, unit2Value, "Unit 2 should have default value (address 100)");

        // Read holding register 100 from unit ID 1 - should return written value
        ReadHoldingRegistersResponse unit1Response =
            client.readHoldingRegisters(1, new ReadHoldingRegistersRequest(100, 1));
        byte[] unit1Registers = unit1Response.registers();
        int unit1Value = ((unit1Registers[0] & 0xFF) << 8) | (unit1Registers[1] & 0xFF);
        assertEquals(0xBEEF, unit1Value, "Unit 1 should have written value 0xBEEF");
      } finally {
        client.disconnect();
      }
    }
  }

  @Test
  void testSharedUnitsDefault() throws Exception {
    try (var server = new TestServerBuilder().build()) {
      server.start();

      var client = createClient(server);
      client.connect();
      try {
        // Write value 0xCAFE to holding register 100 on unit ID 1
        client.writeSingleRegister(1, new WriteSingleRegisterRequest(100, 0xCAFE));

        // Read holding register 100 from unit ID 2 - should return the same written value
        ReadHoldingRegistersResponse unit2Response =
            client.readHoldingRegisters(2, new ReadHoldingRegistersRequest(100, 1));
        byte[] unit2Registers = unit2Response.registers();
        int unit2Value = ((unit2Registers[0] & 0xFF) << 8) | (unit2Registers[1] & 0xFF);
        assertEquals(0xCAFE, unit2Value, "Unit 2 should see shared value 0xCAFE");
      } finally {
        client.disconnect();
      }
    }
  }

  @Test
  void testSeparateUnitsDefaultInitialization() throws Exception {
    try (var server = new TestServerBuilder().withSeparateUnits(true).build()) {
      server.start();

      var client = createClient(server);
      client.connect();
      try {
        // Read holding registers 0-9 from unit ID 5 (first access)
        ReadHoldingRegistersResponse unit5Response =
            client.readHoldingRegisters(5, new ReadHoldingRegistersRequest(0, 10));
        byte[] unit5Registers = unit5Response.registers();
        int[] unit5Values = extractRegisterValues(unit5Registers, 10);
        assertArrayEquals(
            new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            unit5Values,
            "Unit 5 should have default values (addresses 0-9)");

        // Read holding registers 0-9 from unit ID 10 (first access)
        ReadHoldingRegistersResponse unit10Response =
            client.readHoldingRegisters(10, new ReadHoldingRegistersRequest(0, 10));
        byte[] unit10Registers = unit10Response.registers();
        int[] unit10Values = extractRegisterValues(unit10Registers, 10);
        assertArrayEquals(
            new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            unit10Values,
            "Unit 10 should have default values (addresses 0-9)");
      } finally {
        client.disconnect();
      }
    }
  }

  @Test
  void testSeparateUnitsWritePersists() throws Exception {
    try (var server = new TestServerBuilder().withSeparateUnits(true).build()) {
      server.start();

      var client = createClient(server);
      client.connect();
      try {
        // Write value 0xABCD to holding register 200 on unit ID 3
        client.writeSingleRegister(3, new WriteSingleRegisterRequest(200, 0xABCD));

        // Read holding register 200 from unit ID 3
        ReadHoldingRegistersResponse response1 =
            client.readHoldingRegisters(3, new ReadHoldingRegistersRequest(200, 1));
        byte[] registers1 = response1.registers();
        int value1 = ((registers1[0] & 0xFF) << 8) | (registers1[1] & 0xFF);
        assertEquals(0xABCD, value1, "First read should return 0xABCD");

        // Write another value 0x1234 to the same register
        client.writeSingleRegister(3, new WriteSingleRegisterRequest(200, 0x1234));

        // Read again and verify it returns 0x1234
        ReadHoldingRegistersResponse response2 =
            client.readHoldingRegisters(3, new ReadHoldingRegistersRequest(200, 1));
        byte[] registers2 = response2.registers();
        int value2 = ((registers2[0] & 0xFF) << 8) | (registers2[1] & 0xFF);
        assertEquals(0x1234, value2, "Second read should return 0x1234");
      } finally {
        client.disconnect();
      }
    }
  }

  private static ModbusTcpClient createClient(TestServerBuilder server) {
    NettyTcpClientTransport transport =
        NettyTcpClientTransport.create(
            cfg -> {
              cfg.hostname = server.getBindAddress();
              cfg.port = server.getPort();
            });
    return ModbusTcpClient.create(transport);
  }

  private static int[] extractRegisterValues(byte[] registers, int count) {
    int[] values = new int[count];
    for (int i = 0; i < count; i++) {
      values[i] = ((registers[i * 2] & 0xFF) << 8) | (registers[i * 2 + 1] & 0xFF);
    }
    return values;
  }
}
