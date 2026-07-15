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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression for v8.1.7 PR #921: short/null buildNumber must not crash startup. */
class PSFormatVersionPortTest {

  @Test
  void getVersionStringGuardsBuildNumberLength() throws Exception {
    Path root = resolveRoot();
    Path src =
        root.resolve("system/src/main/java/com/percussion/system/utils/PSFormatVersion.java");
    if (!Files.isRegularFile(src)) fail(src.toString());
    String java = Files.readString(src, StandardCharsets.UTF_8);
    assertTrue(java.contains("buildNum.length() >= 6"));
    assertTrue(java.contains("typeCode != null"));
  }

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (Files.isDirectory(cwd.resolve("system"))) return cwd;
    Path up = cwd.resolve("..").normalize();
    if (Files.isDirectory(up.resolve("system"))) return up;
    Path up2 = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up2.resolve("system"))) return up2;
    fail("could not resolve monorepo root from " + cwd);
    return cwd;
  }
}
