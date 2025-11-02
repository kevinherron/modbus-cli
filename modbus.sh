#!/bin/bash

# Execute the modbus-cli JAR with options to suppress warnings
exec java \
  --enable-native-access=ALL-UNNAMED \
  -jar target/modbus-cli-0.1-SNAPSHOT.jar \
  "$@"
