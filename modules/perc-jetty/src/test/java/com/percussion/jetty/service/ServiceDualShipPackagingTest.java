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

package com.percussion.jetty.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GH-1978 / GH-962 slice 3: packaging dual-ship proof for CMS Jetty Linux service assets.
 *
 * <p>Asserts that the dual-ship surface (native systemd unit template + init.d installer + ops
 * README) remains present under {@code src/main/jetty/service/}, is staged into the assembly
 * distribution tree, and is present in the packaged zip/tar when those artifacts exist. Does not
 * perform a live {@code systemctl} soak (deferred to ops residual on #1978).
 *
 * <p><b>Dual-ship policy:</b> keep init.d fallback until a real Linux install soak signs off; do
 * not remove product init.d without human ops review (Child D / #1976).
 */
class ServiceDualShipPackagingTest {

  private static final Path SERVICE_SRC = Path.of("src", "main", "jetty", "service");

  /** Required dual-ship files under service/ (Linux path + docs). */
  private static final String[] REQUIRED_LINUX_SERVICE_FILES = {
    "install-jetty-service.sh", "percussion-cms.service.in", "README-systemd.md",
  };

  /** Windows Procrun installer is out of scope for dual-ship but must keep shipping. */
  private static final String INSTALL_BAT = "install-jetty-service.bat";

  private static Path serviceDir;

  @BeforeAll
  static void resolveServiceDir() {
    serviceDir = SERVICE_SRC;
    if (!Files.isDirectory(serviceDir)) {
      // Reactor CWD may be repo root
      Path alt = Path.of("modules", "perc-jetty").resolve(SERVICE_SRC);
      if (Files.isDirectory(alt)) {
        serviceDir = alt;
      }
    }
    assertTrue(
        Files.isDirectory(serviceDir),
        () -> "missing jetty service dir at " + SERVICE_SRC.toAbsolutePath());
  }

  @Test
  void sourceTree_shipsDualShipLinuxServiceAssets() {
    for (String name : REQUIRED_LINUX_SERVICE_FILES) {
      Path f = serviceDir.resolve(name);
      assertTrue(Files.isRegularFile(f), () -> "missing dual-ship source " + f.toAbsolutePath());
    }
    assertTrue(
        Files.isRegularFile(serviceDir.resolve(INSTALL_BAT)),
        "Windows service installer must still ship alongside Linux dual-ship");
  }

  @Test
  void installScript_retainsInitdFallback_dualShipPolicy() throws IOException {
    Path script = serviceDir.resolve("install-jetty-service.sh");
    String text = Files.readString(script, StandardCharsets.UTF_8);
    assertTrue(text.contains("--initd"), "force init.d flag required until soak (#1978)");
    assertTrue(text.contains("enableSysV") || text.contains("/etc/init.d/"), "init.d path present");
    assertTrue(text.contains("is_systemd_available"), "systemd path present");
    assertTrue(text.contains("installSystemdUnit"), "native unit install present");
    // Policy note: no single-path-only packaging — both mechanisms stay in one script
    assertFalse(
        text.contains("init.d is removed") || text.contains("initd removed"),
        "must not claim init.d removed while dual-ship policy is active");
  }

  @Test
  void readme_documentsDualShipAndKeepInitdUntilSoak() throws IOException {
    Path readme = serviceDir.resolve("README-systemd.md");
    String text = Files.readString(readme, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("init.d") || text.contains("SysV"),
        "README must document init.d / SysV fallback");
    assertTrue(text.contains("--initd"), "README must document --initd force path");
    assertTrue(
        text.toLowerCase().contains("dual-ship")
            || text.contains("keep init.d")
            || text.contains("until soak")
            || text.contains("fallback"),
        "README must state dual-ship / keep-init.d-until-soak policy");
  }

  @Test
  void assemblyStaging_includesServiceDirectoryWhenBuilt() throws IOException {
    Path staged = resolveAssemblyServiceDir();
    assumeTrue(
        staged != null && Files.isDirectory(staged),
        "assembly not staged yet (process-resources); source dual-ship assertions still apply");

    for (String name : REQUIRED_LINUX_SERVICE_FILES) {
      Path f = staged.resolve(name);
      assertTrue(
          Files.isRegularFile(f),
          () ->
              "staged assembly missing "
                  + f
                  + " — antrun must copy src/main/jetty (including service/) into"
                  + " target/distribution");
    }
  }

  @Test
  void packagedZip_containsServiceDualShipAssetsWhenPresent() throws IOException {
    List<Path> archives = findPackagedArchives();
    assumeTrue(
        !archives.isEmpty(),
        "no perc-jetty zip/tar yet (package phase); source dual-ship assertions still apply");

    for (Path archive : archives) {
      if (!archive.getFileName().toString().endsWith(".zip")) {
        // tar.gz: open as zip may fail; only assert zip here (same fileSet content)
        continue;
      }
      Set<String> names = new HashSet<>();
      try (ZipFile zip = new ZipFile(archive.toFile())) {
        zip.stream().map(e -> e.getName().replace('\\', '/')).forEach(names::add);
      } catch (IOException io) {
        fail("Could not read " + archive + ": " + io.getMessage());
      }
      for (String required : REQUIRED_LINUX_SERVICE_FILES) {
        String needle = "service/" + required;
        assertTrue(
            names.stream().anyMatch(n -> n.equals(needle) || n.endsWith("/" + needle)),
            () -> archive + " must contain " + needle + " (GH-1978 dual-ship packaging)");
      }
    }
  }

  private static Path resolveAssemblyServiceDir() {
    Path moduleDist = Path.of("target", "distribution", "service");
    if (Files.isDirectory(moduleDist)) {
      return moduleDist;
    }
    Path reactorDist = Path.of("modules", "perc-jetty", "target", "distribution", "service");
    if (Files.isDirectory(reactorDist)) {
      return reactorDist;
    }
    return null;
  }

  private static List<Path> findPackagedArchives() throws IOException {
    List<Path> found = new ArrayList<>();
    for (Path target : List.of(Path.of("target"), Path.of("modules", "perc-jetty", "target"))) {
      if (!Files.isDirectory(target)) {
        continue;
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(target, "perc-jetty-*")) {
        for (Path p : stream) {
          String n = p.getFileName().toString();
          if (n.endsWith(".zip") || n.endsWith(".tar.gz")) {
            found.add(p);
          }
        }
      }
    }
    return found;
  }
}
