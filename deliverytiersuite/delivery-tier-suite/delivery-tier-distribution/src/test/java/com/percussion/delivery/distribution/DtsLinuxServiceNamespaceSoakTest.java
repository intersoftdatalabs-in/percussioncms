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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * GH-1989: run {@code DTSProductionService.sh} inside a user namespace. Staging uses the same
 * installer shape; this soak covers one DTS role. Not a customer-host journalctl sign-off.
 */
@EnabledOnOs(OS.LINUX)
class DtsLinuxServiceNamespaceSoakTest {

  @Test
  void productionDts_systemdInitdUninstallAndMigration() throws Exception {
    assumeTrue(new ProcessBuilder("unshare", "--help").start().waitFor() == 0, "unshare missing");
    Path repo = repoRoot();
    Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target")), "dts-soak-");
    try {
      Path root = work.resolve("install");
      Path server = root.resolve("Deployment").resolve("Server");
      Files.createDirectories(server.resolve("bin"));
      copy(
          repo.resolve("modules/perc-jetty/src/main/jetty/resolve-java-home.sh"),
          root.resolve("resolve-java-home.sh"));
      copy(
          repo.resolve(
              "deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/DTSProductionService.sh"),
          server.resolve("DTSProductionService.sh"));
      copy(
          repo.resolve(
              "deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/dts-tomcat.service.in"),
          server.resolve("dts-tomcat.service.in"));
      Files.writeString(
          server.resolve("bin/catalina.sh"),
          "#!/bin/bash\n# catalina helper stub for service-install soak\n"
              + "case \"$1\" in status) echo not running; exit 3;; *) exit 0;; esac\n",
          StandardCharsets.UTF_8);
      String javaHome = System.getProperty("java.home");
      Files.writeString(
          root.resolve("java.properties"),
          "JAVA_HOME=" + javaHome + "\nJAVA=" + javaHome + "/bin/java\n",
          StandardCharsets.UTF_8);
      Path evidence = work.resolve("evidence");
      Files.createDirectories(evidence);
      ProcessBuilder pb =
          new ProcessBuilder(
              "bash",
              repo.resolve("scripts/linux-service-namespace-soak.sh").toString(),
              "dts",
              root.toAbsolutePath().toString(),
              evidence.toAbsolutePath().toString());
      pb.redirectErrorStream(true);
      pb.redirectOutput(evidence.resolve("stdout.txt").toFile());
      int code = pb.start().waitFor();
      String stdout = read(evidence.resolve("stdout.txt"));
      assertEquals(0, code, stdout);
      assertEquals("dts-ok\n", read(evidence.resolve("result.txt")).replace("\r\n", "\n"));
      String unit = read(evidence.resolve("dts-systemd.unit"));
      assertTrue(unit.contains("TimeoutStartSec=1800"), unit);
      assertTrue(unit.contains("StandardOutput=journal"), unit);
      assertTrue(read(evidence.resolve("dts-initd-chkconfig.log")).contains("PercussionProductionDTS on"));
    } finally {
      deleteTree(work);
    }
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static void copy(Path from, Path to) throws IOException {
    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
  }

  private static Path repoRoot() {
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null) {
      if (Files.isRegularFile(dir.resolve("scripts/linux-service-namespace-soak.sh"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    throw new IllegalStateException("linux-service-namespace-soak.sh not found");
  }

  private static void deleteTree(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (var walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                  // best-effort cleanup of the fixture
                }
              });
    }
  }
}
