/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
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
        () ->
            "free connector port must not look like a running DTS (ports "
                + ports[0]
                + ", "
                + ports[1]
                + ")");
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
        () ->
            "free connector + shutdown ports must not look like a running DTS (ports "
                + ports[0]
                + ", "
                + ports[1]
                + ")");
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

  /**
   * UDP-only holders must not make a TCP port look unavailable (install/DTS probes are TCP-only).
   * Repeat several iterations so intermittent Windows Winsock SO_REUSEADDR+UDP races surface in
   * CI rather than only occasionally (GH-2779).
   */
  @Test
  void portAvailable_ignoresUdpBinding() throws Exception {
    final int iterations = 25;
    for (int i = 0; i < iterations; i++) {
      final int iteration = i;
      // Bind explicitly (reuse off) so the hold is a pure UDP exclusive binding.
      try (DatagramSocket hold = new DatagramSocket(null)) {
        hold.setReuseAddress(false);
        hold.bind(new InetSocketAddress(0));
        final int port = hold.getLocalPort();
        assertTrue(
            InstallUtil.portAvailable(port),
            () ->
                "UDP-only binding on port "
                    + port
                    + " must leave TCP available (iteration "
                    + iteration
                    + " of "
                    + iterations
                    + ")");
      }
    }
  }

  @Test
  void portAvailable_roundTrip() throws Exception {
    int free = findFreePort();
    assertTrue(InstallUtil.portAvailable(free));
    try (ServerSocket hold = new ServerSocket()) {
      // Bind first without reuse so the hold is an exclusive TCP listener (matches product
      // sockets and avoids Windows dual-SO_REUSEADDR ambiguity).
      hold.setReuseAddress(false);
      hold.bind(new InetSocketAddress(free));
      assertFalse(
          InstallUtil.portAvailable(free),
          "exclusive TCP listener must make portAvailable return false");
    }
    // After close, TIME_WAIT may briefly hold the port; phase-2 SO_REUSEADDR should still
    // report available so offline detection is not stuck busy.
    assertTrue(
        InstallUtil.portAvailable(free),
        "port must look available again after TCP holder closed (TIME_WAIT tolerant)");
  }

  @Test
  void isLocalTcpPortAccepting_tracksListener() throws Exception {
    int port = findFreePort();
    assertFalse(
        InstallUtil.isLocalTcpPortAccepting(port),
        "nothing should accept on a freshly allocated free port");
    try (ServerSocket hold = new ServerSocket()) {
      hold.setReuseAddress(false);
      hold.bind(new InetSocketAddress(port));
      assertTrue(
          InstallUtil.isLocalTcpPortAccepting(port),
          "exclusive TCP listener must be reachable on a local address");
    }
    assertFalse(
        InstallUtil.isLocalTcpPortAccepting(port),
        "listener close must leave the port not accepting");
  }

  /**
   * Allocated "free" ports must still be bindable after the probe sockets close. Windows Hyper-V
   * excluded ranges can make {@code ServerSocket(0)} return a port that binds once and then looks
   * occupied, which made offline DTS checks treat a stopped instance as running.
   */
  @Test
  void findDistinctFreePorts_remainAvailableAfterRelease() throws Exception {
    final int iterations = 20;
    for (int i = 0; i < iterations; i++) {
      final int iteration = i;
      int[] ports = findDistinctFreePorts(2);
      assertNotEquals(ports[0], ports[1], "allocated ports must be distinct");
      assertTrue(
          InstallUtil.portAvailable(ports[0]),
          () -> "connector probe port still available (iteration " + iteration + ")");
      assertTrue(
          InstallUtil.portAvailable(ports[1]),
          () -> "shutdown probe port still available (iteration " + iteration + ")");
      assertTrue(
          canRebindTcpPort(ports[0]),
          () -> "connector probe port still bindable (iteration " + iteration + ")");
      assertTrue(
          canRebindTcpPort(ports[1]),
          () -> "shutdown probe port still bindable (iteration " + iteration + ")");
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
    return findDistinctFreePorts(1)[0];
  }

  /**
   * Allocate {@code n} distinct free ports that remain bindable after the probe sockets close.
   *
   * <p>Holds all sockets open until the last bind so the OS cannot re-issue the same ephemeral port
   * twice in a row. Then re-checks a real TCP rebind: on Windows, Hyper-V excluded port ranges can
   * make {@code ServerSocket(0)} return a port that binds once and refuses every later bind.
   */
  private static int[] findDistinctFreePorts(int n) throws IOException {
    if (n < 1) {
      throw new IllegalArgumentException("n must be >= 1");
    }
    final int maxAttempts = 32;
    IOException lastBindFailure = null;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      ServerSocket[] holders = new ServerSocket[n];
      int[] ports = new int[n];
      try {
        for (int i = 0; i < n; i++) {
          ServerSocket ss = new ServerSocket();
          ss.setReuseAddress(false);
          ss.bind(new InetSocketAddress(0));
          holders[i] = ss;
          ports[i] = ss.getLocalPort();
        }
      } catch (IOException e) {
        lastBindFailure = e;
        closeQuietly(holders);
        continue;
      }
      closeQuietly(holders);

      boolean stillBindable = true;
      for (int port : ports) {
        if (!canRebindTcpPort(port)) {
          stillBindable = false;
          break;
        }
      }
      if (stillBindable) {
        return ports;
      }
    }
    throw new IOException(
        "Could not allocate "
            + n
            + " distinct free TCP ports that remain bindable after "
            + maxAttempts
            + " attempts",
        lastBindFailure);
  }

  /**
   * Two-phase TCP bind (no connect fallback). Used to reject Windows excluded-range ports that
   * {@link InstallUtil#portAvailable(int)} correctly reports as "not a listener" but that later
   * {@code new ServerSocket(port)} would fail to hold.
   */
  private static boolean canRebindTcpPort(int port) {
    InetSocketAddress address = new InetSocketAddress(port);
    try (ServerSocket ss = new ServerSocket()) {
      ss.setReuseAddress(false);
      ss.bind(address);
      return true;
    } catch (IOException ignored) {
      // TIME_WAIT or excluded — try reuse bind
    }
    try (ServerSocket ss = new ServerSocket()) {
      ss.setReuseAddress(true);
      ss.bind(address);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private static void closeQuietly(ServerSocket[] holders) {
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
}
