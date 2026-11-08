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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.javafx.PSBrowserUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for #5773: DCE applet-init paths must route diagnostics to the product logger
 * (log4j2) instead of swallowing exceptions via {@code printStackTrace} to stdout.
 */
public class PSAppletLoggingXlintTest {

  private static final List<String> LOGGED_SOURCES =
      List.of(
          "com/percussion/cx/PSContentExplorerApplet.java",
          "com/percussion/cx/PSContentExplorerHelper.java",
          "com/percussion/cx/PSActionManager.java",
          "com/percussion/cx/PSFolderActionManager.java",
          "com/percussion/cx/PSContentExplorerLoginPanel.java",
          "com/percussion/cx/PSContentExplorerUtils.java",
          "com/percussion/cx/PSSearchViewActionManager.java",
          "com/percussion/cx/javafx/PSBrowserUtils.java");

  @Test
  public void appletInitSourcesContainNoPrintStackTrace() throws Exception {
    Path srcRoot = resolveMainSourceRoot();
    for (String rel : LOGGED_SOURCES) {
      Path file = srcRoot.resolve(rel.replace("/", srcRoot.getFileSystem().getSeparator()));
      assertTrue(Files.isRegularFile(file), () -> "expected source file: " + file);
      String content = Files.readString(file, StandardCharsets.UTF_8);
      assertTrue(
          !content.contains("printStackTrace"),
          () -> rel + " must not swallow exceptions via printStackTrace (see #5773)");
    }
  }

  @Test
  public void appletInitTypesExposeLog4jLogger() throws Exception {
    assertLoggerField(PSContentExplorerApplet.class, "log");
    assertLoggerField(PSContentExplorerHelper.class, "log");
    assertLoggerField(PSActionManager.class, "log");
    assertLoggerField(PSFolderActionManager.class, "log");
    assertLoggerField(PSContentExplorerLoginPanel.class, "log");
    assertLoggerField(PSContentExplorerUtils.class, "log");
    assertLoggerField(PSSearchViewActionManager.class, "log");
    assertLoggerField(PSBrowserUtils.class, "log");
  }

  private static void assertLoggerField(Class<?> type, String name) throws Exception {
    Field log = type.getDeclaredField(name);
    assertEquals(
        Logger.class,
        log.getType(),
        () -> type.getName() + "#" + name + " must be a log4j2 Logger");
    assertTrue(
        Modifier.isStatic(log.getModifiers()),
        () -> type.getName() + "#" + name + " must be static");
  }

  private static Path resolveMainSourceRoot() {
    Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    for (Path candidate : List.of(dir, dir.getParent())) {
      if (candidate != null && Files.isDirectory(candidate.resolve("src").resolve("main"))) {
        return candidate.resolve("src").resolve("main").resolve("java");
      }
    }
    return dir.resolve("src").resolve("main").resolve("java");
  }
}
