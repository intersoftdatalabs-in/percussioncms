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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GH-3342: H2 qa-up died because {@code sitesAdaptor} could not load {@code
 * PSVirtualSitePublishCopyResult}. That type lives in perc-system and must be present on the same
 * runtime classpath as sitemanage (WebUI WAR {@code WEB-INF/lib}).
 */
class PercSystemVirtualSiteClassPackagingTest {

  private static final String COPY_RESULT_FQN =
      "com.percussion.services.virtualsite.PSVirtualSitePublishCopyResult";

  private static final String COPY_RESULT_ENTRY =
      "com/percussion/services/virtualsite/PSVirtualSitePublishCopyResult.class";

  @Test
  @DisplayName("provided perc-system on dist-tree classpath contains the copy-result record")
  void percSystemOnClasspathContainsCopyResult() throws Exception {
    Class<?> type = Class.forName(COPY_RESULT_FQN);
    assertTrue(type.isRecord(), COPY_RESULT_FQN + " must be a record in perc-system");
    URL location = type.getProtectionDomain().getCodeSource().getLocation();
    assertNotNull(location, "perc-system code source");
    Path artifact = Paths.get(location.toURI());
    if (Files.isRegularFile(artifact) && artifact.getFileName().toString().endsWith(".jar")) {
      assertTrue(
          jarContains(artifact, COPY_RESULT_ENTRY),
          "perc-system jar must contain " + COPY_RESULT_ENTRY + ": " + artifact);
    } else {
      assertTrue(
          Files.isRegularFile(artifact.resolve(COPY_RESULT_ENTRY)),
          "perc-system classes dir must contain " + COPY_RESULT_ENTRY + ": " + artifact);
    }
  }

  @Test
  @DisplayName("WebUI WAR perc-system (when packaged) includes the copy-result class")
  void webUiWarPercSystemContainsCopyResultWhenPresent() throws Exception {
    Path war = findWebUiWar();
    if (war == null) {
      return;
    }
    boolean foundPercSystem = false;
    try (JarFile warFile = new JarFile(war.toFile())) {
      Enumeration<JarEntry> entries = warFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName().replace('\\', '/');
        if (!name.startsWith("WEB-INF/lib/") || !name.contains("perc-system") || !name.endsWith(".jar")) {
          continue;
        }
        foundPercSystem = true;
        Path tmp = Files.createTempFile("perc-system-from-war-", ".jar");
        try (var in = warFile.getInputStream(entry)) {
          Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
          assertTrue(
              jarContains(tmp, COPY_RESULT_ENTRY),
              "WEB-INF/lib perc-system inside " + war + " must contain " + COPY_RESULT_ENTRY);
        } finally {
          Files.deleteIfExists(tmp);
        }
      }
    }
    assertTrue(foundPercSystem, "WebUI WAR must package perc-system under WEB-INF/lib: " + war);
  }

  private static boolean jarContains(Path jar, String entryName) throws IOException {
    try (JarFile jf = new JarFile(jar.toFile())) {
      return jf.getEntry(entryName) != null;
    }
  }

  private static Path findWebUiWar() throws IOException {
    Path cwd = Paths.get("").toAbsolutePath().normalize();
    Path[] dirs =
        new Path[] {
          cwd.resolve("WebUI").resolve("target"),
          cwd.resolve("..").resolve("..").resolve("WebUI").resolve("target"),
        };
    for (Path dir : dirs) {
      if (dir == null || !Files.isDirectory(dir)) {
        continue;
      }
      try (var stream = Files.list(dir)) {
        Path war =
            stream
                .filter(p -> p.getFileName().toString().endsWith(".war"))
                .filter(p -> p.getFileName().toString().contains("perc-web-ui"))
                .findFirst()
                .orElse(null);
        if (war != null) {
          return war;
        }
      }
    }
    return null;
  }
}
