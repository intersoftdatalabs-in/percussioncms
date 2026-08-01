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
    int[] ports = findDistinctFreePorts(2);
    Path root = writeDtsLayout(ports[0], ports[1], false);
    assertFalse(
        InstallUtil.checkTomcatServerRunning(root.toString()),
        "free connector port must not look like a running DTS");
  }

  @Test
  void checkTomcatServerRunning_trueWhenLiteralPortBound() throws Exception {
    int[] ports = findDistinctFreePorts(2);
    Path root = writeDtsLayout(ports[0], ports[1], false);
    try (ServerSocket hold = new ServerSocket(ports[0])) {
      hold.setReuseAddress(true);
      assertTrue(
          InstallUtil.checkTomcatServerRunning(root.toString()),
          "bound connector port must look like a running DTS");
    }
  }

  @Test
  void checkTomcatServerRunning_resolvesCatalinaPlaceholderWhenBound() throws Exception {
    int[] ports = findDistinctFreePorts(2);
    Path root = writeDtsLayout(ports[0], ports[1], true);
    try (ServerSocket hold = new ServerSocket(ports[0])) {
      hold.setReuseAddress(true);
      assertTrue(
          InstallUtil.checkTomcatServerRunning(root.toString()),
          "placeholder ${http.port} must resolve via perc-catalina.properties");
    }
  }

  @Test
  void checkTomcatServerRunning_placeholderFreePortIsOffline() throws Exception {
    int[] ports = findDistinctFreePorts(2);
    Path root = writeDtsLayout(ports[0], ports[1], true);
    assertFalse(
        InstallUtil.checkTomcatServerRunning(root.toString()),
        "free connector + shutdown ports must not look like a running DTS");
  }

  @Test
  void checkTomcatServerRunning_trueWhenOnlyShutdownPortBound() throws Exception {
    int[] ports = findDistinctFreePorts(2);
    Path root = writeDtsLayout(ports[0], ports[1], false);
    try (ServerSocket hold = new ServerSocket(ports[1])) {
      hold.setReuseAddress(true);
      assertTrue(
          InstallUtil.checkTomcatServerRunning(root.toString()),
          "bound Server shutdown port is a secondary running signal");
    }
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
   * @param connectorPort HTTP connector port (literal or value of {@code ${http.port}})
   * @param shutdownPort {@code Server/@port} — must be free for offline assertions; never hardcode
   *     product default {@code 8005} (a running local DTS would flake offline tests)
   * @param usePlaceholder when true, server.xml uses {@code ${http.port}} and properties file sets
   *     the real port
   */
  private Path writeDtsLayout(int connectorPort, int shutdownPort, boolean usePlaceholder)
      throws IOException {
    Path root =
        temp.resolve(
            "dtsRoot-" + connectorPort + "-" + shutdownPort + (usePlaceholder ? "-ph" : "-lit"));
    Path conf = root.resolve("Deployment").resolve("Server").resolve("conf");
    Path perc = conf.resolve("perc");
    Files.createDirectories(perc);

    String portAttr = usePlaceholder ? "${http.port}" : Integer.toString(connectorPort);
    String xml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <Server port="%s" shutdown="SHUTDOWN">
          <Service name="Catalina">
            <Connector port="%s" protocol="HTTP/1.1"/>
          </Service>
        </Server>
        """
            .formatted(Integer.toString(shutdownPort), portAttr);
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

  /**
   * Allocate {@code n} distinct free ports. Holds all sockets open until the last bind so the OS
   * cannot re-issue the same ephemeral port twice in a row.
   */
  private static int[] findDistinctFreePorts(int n) throws IOException {
    if (n < 1) {
      throw new IllegalArgumentException("n must be >= 1");
    }
    ServerSocket[] holders = new ServerSocket[n];
    int[] ports = new int[n];
    try {
      for (int i = 0; i < n; i++) {
        holders[i] = new ServerSocket(0);
        holders[i].setReuseAddress(true);
        ports[i] = holders[i].getLocalPort();
      }
    } finally {
      for (ServerSocket ss : holders) {
        if (ss != null) {
          try {
            ss.close();
          } catch (IOException ignored) {
            // best-effort close of probe sockets
          }
        }
      }
    }
    return ports;
  }
}
