/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for offline install gates: DTS Tomcat connector binding detection ({@link
 * InstallUtil#checkTomcatServerRunning(String)}) including {@code ${http.port}} resolution from
 * perc-catalina.properties.
 */
@Tag("UnitTest")
public class InstallUtilRunningServerTest {

  @TempDir Path temp;

  @Test
  void checkTomcatServerRunning_falseWhenNoServerXml() {
    Path emptyRoot = temp.resolve("emptyDts");
    assertFalse(InstallUtil.checkTomcatServerRunning(emptyRoot.toString()));
  }

  @Test
  void checkTomcatServerRunning_falseWhenLiteralPortFree() throws Exception {
    int freePort = findFreePort();
    Path root = writeDtsLayout(freePort, false);
    assertFalse(
        InstallUtil.checkTomcatServerRunning(root.toString()),
        "free connector port must not look like a running DTS");
  }

  @Test
  void checkTomcatServerRunning_trueWhenLiteralPortBound() throws Exception {
    int port = findFreePort();
    Path root = writeDtsLayout(port, false);
    try (ServerSocket hold = new ServerSocket(port)) {
      hold.setReuseAddress(true);
      assertTrue(
          InstallUtil.checkTomcatServerRunning(root.toString()),
          "bound connector port must look like a running DTS");
    }
  }

  @Test
  void checkTomcatServerRunning_resolvesCatalinaPlaceholderWhenBound() throws Exception {
    int port = findFreePort();
    Path root = writeDtsLayout(port, true);
    try (ServerSocket hold = new ServerSocket(port)) {
      hold.setReuseAddress(true);
      assertTrue(
          InstallUtil.checkTomcatServerRunning(root.toString()),
          "placeholder ${http.port} must resolve via perc-catalina.properties");
    }
  }

  @Test
  void checkTomcatServerRunning_placeholderFreePortIsOffline() throws Exception {
    int freePort = findFreePort();
    Path root = writeDtsLayout(freePort, true);
    assertFalse(InstallUtil.checkTomcatServerRunning(root.toString()));
  }

  @Test
  void resolveTomcatPortToken_literalAndPlaceholder() {
    Properties p = new Properties();
    p.setProperty("http.port", "29980");
    assertEquals(8080, InstallUtil.resolveTomcatPortToken("8080", p));
    assertEquals(29980, InstallUtil.resolveTomcatPortToken("${http.port}", p));
    assertNull(InstallUtil.resolveTomcatPortToken("${missing.port}", p));
    assertNull(InstallUtil.resolveTomcatPortToken("not-a-port", p));
    assertNull(InstallUtil.resolveTomcatPortToken(null, p));
  }

  @Test
  void portAvailable_ignoresUdpBinding() throws Exception {
    try (DatagramSocket hold = new DatagramSocket(0)) {
      assertTrue(InstallUtil.portAvailable(hold.getLocalPort()));
    }
  }

  @Test
  void portAvailable_roundTrip() throws Exception {
    int free = findFreePort();
    assertTrue(InstallUtil.portAvailable(free));
    try (ServerSocket hold = new ServerSocket(free)) {
      hold.setReuseAddress(true);
      assertFalse(InstallUtil.portAvailable(free));
    }
  }

  /**
   * @param usePlaceholder when true, server.xml uses {@code ${http.port}} and properties file sets
   *     the real port
   */
  private Path writeDtsLayout(int connectorPort, boolean usePlaceholder) throws IOException {
    Path root = temp.resolve("dtsRoot-" + connectorPort + (usePlaceholder ? "-ph" : "-lit"));
    Path conf = root.resolve("Deployment").resolve("Server").resolve("conf");
    Path perc = conf.resolve("perc");
    Files.createDirectories(perc);

    String portAttr = usePlaceholder ? "${http.port}" : Integer.toString(connectorPort);
    String xml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <Server port="8005" shutdown="SHUTDOWN">
          <Service name="Catalina">
            <Connector port="%s" protocol="HTTP/1.1"/>
          </Service>
        </Server>
        """
            .formatted(portAttr);
    Files.writeString(conf.resolve("server.xml"), xml, StandardCharsets.UTF_8);

    if (usePlaceholder) {
      Files.writeString(
          perc.resolve("perc-catalina.properties"),
          "http.port=" + connectorPort + "\n",
          StandardCharsets.UTF_8);
    }
    return root;
  }

  private static int findFreePort() throws IOException {
    try (ServerSocket ss = new ServerSocket(0)) {
      ss.setReuseAddress(true);
      return ss.getLocalPort();
    }
  }
}
