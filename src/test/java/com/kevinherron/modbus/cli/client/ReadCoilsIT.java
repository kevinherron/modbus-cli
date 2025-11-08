package com.kevinherron.modbus.cli.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.kevinherron.modbus.cli.test.CliTestRunner;
import com.kevinherron.modbus.cli.test.CliTestRunner.Result;
import com.kevinherron.modbus.cli.test.TestServerBuilder;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ReadCoilsIT {

  @Test
  void testReadCoils() throws Exception {
    try (var server = new TestServerBuilder().build()) {
      server.start();

      // Read coils 100-109 (10 coils)
      Result result =
          CliTestRunner.execute(
              "--format",
              "json",
              "client",
              "localhost",
              "-p",
              String.valueOf(server.getPort()),
              "rc",
              "100",
              "10");

      assertEquals(0, result.exitCode(), "Command should succeed");

      // Parse and validate JSON output - output contains multiple JSON lines
      String output = result.getOutput();
      System.out.println(output);

      var jsonNodes = new ArrayList<JsonNode>();
      var objectMapper = new ObjectMapper();

      try (var reader = new BufferedReader(new StringReader(result.getOutput()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.trim().isEmpty()) {
            jsonNodes.add(objectMapper.readTree(line));
          }
        }
      }

      // Verify we have 4 lines of output
      assertEquals(4, jsonNodes.size(), "Should have 4 JSON lines");

      // Verify the "info" type object
      JsonNode infoNode = jsonNodes.getFirst();
      assertEquals("info", infoNode.get("type").asText());
      assertEquals(
          "Hostname: localhost:" + server.getPort() + ", Unit ID: 1",
          infoNode.get("message").asText());
      assertTrue(infoNode.has("timestamp"), "info node should have timestamp");

      // Verify "protocol" type object in "sent" direction
      JsonNode sentNode = jsonNodes.get(1);
      assertEquals("protocol", sentNode.get("type").asText());
      assertEquals("sent", sentNode.get("direction").asText());
      assertEquals(1, sentNode.get("function_code").asInt());
      assertTrue(sentNode.has("pdu"), "sent node should have pdu");
      assertTrue(sentNode.has("timestamp"), "sent node should have timestamp");

      // Verify "protocol" type object in "received" direction
      JsonNode receivedNode = jsonNodes.get(2);
      assertEquals("protocol", receivedNode.get("type").asText());
      assertEquals("received", receivedNode.get("direction").asText());
      assertEquals(1, receivedNode.get("function_code").asInt());
      assertTrue(receivedNode.has("pdu"), "received node should have pdu");
      assertTrue(receivedNode.has("timestamp"), "received node should have timestamp");

      // Verify the "coil_table" type object
      JsonNode tableNode = jsonNodes.get(3);
      assertEquals("coil_table", tableNode.get("type").asText());
      assertEquals(100, tableNode.get("start_address").asInt());
      assertEquals(10, tableNode.get("quantity").asInt());
      assertTrue(tableNode.has("timestamp"), "table node should have timestamp");

      // Verify the table's data array contains boolean values
      ArrayNode dataNode = (ArrayNode) tableNode.get("data");
      assertEquals(10, dataNode.size(), "Should have 10 boolean values");
      List<Boolean> booleans = dataNode.valueStream().map(JsonNode::asBoolean).toList();
      assertEquals(10, booleans.size());
    }
  }

  @Test
  void testReadCoilsWithCounterAndInterval() throws Exception {
    try (var server = new TestServerBuilder().build()) {
      server.start();

      // Read coils 100-109 (10 coils) 3 times with interval 0
      Result result =
          CliTestRunner.execute(
              "--format",
              "json",
              "client",
              "localhost",
              "-p",
              String.valueOf(server.getPort()),
              "rc",
              "100",
              "10",
              "-c",
              "3",
              "-i",
              "0");

      assertEquals(0, result.exitCode(), "Command should succeed");

      // Parse and validate JSON output - output contains multiple JSON lines
      String output = result.getOutput();
      System.out.println(output);

      var jsonNodes = new ArrayList<JsonNode>();
      var objectMapper = new ObjectMapper();

      try (var reader = new BufferedReader(new StringReader(output))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.trim().isEmpty()) {
            jsonNodes.add(objectMapper.readTree(line));
          }
        }
      }

      // Verify we have 10 lines of output:
      // 1 info + (3 iterations * (1 sent + 1 received + 1 table))
      assertEquals(10, jsonNodes.size(), "Should have 10 JSON lines");

      // Verify the info message
      JsonNode infoNode = jsonNodes.getFirst();
      assertEquals("info", infoNode.get("type").asText());

      // Verify each iteration has sent, received, and table nodes
      for (int iteration = 0; iteration < 3; iteration++) {
        int baseIndex = 1 + (iteration * 3);

        // Verify sent node
        JsonNode sentNode = jsonNodes.get(baseIndex);
        assertEquals("protocol", sentNode.get("type").asText());
        assertEquals("sent", sentNode.get("direction").asText());
        assertEquals(1, sentNode.get("function_code").asInt());
        assertEquals(iteration + 1, sentNode.get("iteration").asInt());
        assertTrue(sentNode.has("timestamp"), "sent node should have timestamp");

        // Verify received node
        JsonNode receivedNode = jsonNodes.get(baseIndex + 1);
        assertEquals("protocol", receivedNode.get("type").asText());
        assertEquals("received", receivedNode.get("direction").asText());
        assertEquals(1, receivedNode.get("function_code").asInt());
        assertEquals(iteration + 1, receivedNode.get("iteration").asInt());
        assertTrue(receivedNode.has("timestamp"), "received node should have timestamp");

        // Verify table node
        JsonNode tableNode = jsonNodes.get(baseIndex + 2);
        assertEquals("coil_table", tableNode.get("type").asText());
        assertEquals(100, tableNode.get("start_address").asInt());
        assertEquals(10, tableNode.get("quantity").asInt());
        assertEquals(iteration + 1, tableNode.get("iteration").asInt());
        assertTrue(tableNode.has("timestamp"), "table node should have timestamp");

        ArrayNode dataNode = (ArrayNode) tableNode.get("data");
        assertEquals(10, dataNode.size(), "Should have 10 boolean values");
      }
    }
  }
}
