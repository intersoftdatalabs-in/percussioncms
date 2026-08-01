/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathsUnderTest {

  @TempDir Path tempDir;

  @Test
  void resolveUnderReturnsChild() {
    Path root = tempDir.toAbsolutePath().normalize();
    Path child = PathsUnder.resolveUnder(root, "child");
    assertEquals(root.resolve("child").normalize(), child);
    assertTrue(child.startsWith(root));
  }

  @Test
  void resolveUnderRejectsDotDotSegmentWhenValidatedSeparately() {
    // ConfigNames rejects ".." before resolve; PathsUnder still guards resolve
    Path root = tempDir.toAbsolutePath().normalize();
    assertThrows(IllegalArgumentException.class, () -> PathsUnder.resolveUnder(root, ".."));
  }
}
