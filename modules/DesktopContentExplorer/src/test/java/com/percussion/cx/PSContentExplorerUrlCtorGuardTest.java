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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the #2045 {@code java.net.URL} constructor migration (issue #5711): no
 * main or test source in this module may reintroduce a deprecated-for-removal {@code new URL(..)}
 * constructor call. The module build compiles with {@code -Xlint:-deprecation}, so a reintroduced
 * call site would not fail the build on its own — this test keeps the {@code -Xlint:all} probe
 * count for URL constructors at zero. All URL construction must go through the
 * {@link PSContentExplorerUtils#toURL} helpers, which preserve the checked
 * {@link java.net.MalformedURLException} contract. Headless-safe: pure file scan, no UI.
 */
public class PSContentExplorerUrlCtorGuardTest {

  private static final Pattern URL_CTOR = Pattern.compile("\\bnew\\s+URL\\s*\\(");

  @Test
  public void noDeprecatedUrlConstructorsInMainOrTestSources() throws Exception {
    Path moduleDir = locateModuleDir();
    assumeTrue(moduleDir != null, "DCE module sources not locatable from user.dir, skipping guard");

    List<String> violations = new ArrayList<>();
    scanSourceRoot(moduleDir.resolve(Path.of("src", "main", "java")), violations);
    scanSourceRoot(moduleDir.resolve(Path.of("src", "test", "java")), violations);

    assertTrue(
        violations.isEmpty(),
        () ->
            "Deprecated-for-removal URL constructor call sites must use the toURL helpers instead:\n"
                + String.join("\n", violations.stream().limit(20).toList()));
  }

  private static void scanSourceRoot(Path root, List<String> violations) throws IOException {
    if (!Files.isDirectory(root)) {
      return;
    }
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
        scanFile(root, file, violations);
      }
    }
  }

  private static void scanFile(Path root, Path file, List<String> violations) throws IOException {
    try {
      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
      for (int i = 0; i < lines.size(); i++) {
        String code = stripLineComment(lines.get(i)).trim();
        if (code.startsWith("*") || code.startsWith("/*") || code.isEmpty()) {
          continue;
        }
        if (URL_CTOR.matcher(code).find()) {
          violations.add(root.relativize(file) + ":" + (i + 1) + ": " + lines.get(i).trim());
        }
      }
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static String stripLineComment(String line) {
    boolean inString = false;
    for (int i = 0; i + 1 < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
        inString = !inString;
      }
      if (!inString && c == '/' && line.charAt(i + 1) == '/') {
        return line.substring(0, i);
      }
    }
    return line;
  }

  /**
   * Resolves the module directory from the Surefire working directory (the module basedir),
   * walking up a few levels so IDE runs with a different {@code user.dir} still locate it.
   * Returns {@code null} when the sources cannot be located (caller aborts the test).
   */
  private static Path locateModuleDir() {
    Path dir = Path.of(System.getProperty("user.dir", "")).toAbsolutePath().normalize();
    for (int level = 0; level < 5 && dir != null; level++) {
      if (Files.isDirectory(dir.resolve(Path.of("src", "main", "java")))) {
        return dir;
      }
      dir = dir.getParent();
    }
    return null;
  }
}
