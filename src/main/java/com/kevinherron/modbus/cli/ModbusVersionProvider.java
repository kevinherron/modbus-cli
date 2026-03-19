package com.kevinherron.modbus.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine.IVersionProvider;

public class ModbusVersionProvider implements IVersionProvider {

  private static final String GROUP_ID = "com.kevinherron.modbus";
  private static final String ARTIFACT_ID = "modbus-cli";
  private static final String FALLBACK_VERSION = "0.3-SNAPSHOT";

  @Override
  public String[] getVersion() {
    return new String[] {"modbus-cli " + resolveVersion()};
  }

  private String resolveVersion() {
    String implementationVersion =
        ModbusVersionProvider.class.getPackage().getImplementationVersion();

    if (implementationVersion != null && !implementationVersion.isBlank()) {
      return implementationVersion;
    }

    String pomPropertiesVersion = loadPomPropertiesVersion();

    if (pomPropertiesVersion != null && !pomPropertiesVersion.isBlank()) {
      return pomPropertiesVersion;
    }

    return FALLBACK_VERSION;
  }

  private String loadPomPropertiesVersion() {
    String resourcePath = "/META-INF/maven/%s/%s/pom.properties".formatted(GROUP_ID, ARTIFACT_ID);

    try (InputStream stream = ModbusVersionProvider.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        return null;
      }

      Properties properties = new Properties();
      properties.load(stream);

      return properties.getProperty("version");
    } catch (IOException e) {
      return null;
    }
  }
}
