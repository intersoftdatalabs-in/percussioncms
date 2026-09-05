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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Path-injection barrier for {@link IOTools#getFileContent} (CodeQL {@code java/path-injection}
 * #2045).
 */
@DisplayName("IOTools.getFileContent path-injection barrier (CWE-22, #2045)")
class IOToolsPathInjectionTest {

  @TempDir Path tmp;

  @Test
  void getFileContent_readsUtf8File() throws Exception {
    Path child = tmp.resolve("ok.txt");
    Files.writeString(child, "hello-io");
    assertEquals("hello-io", IOTools.getFileContent(child.toFile()));
  }

  @Test
  void getFileContent_rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> IOTools.getFileContent(null));
  }

  @Test
  void getFileContentUnderBase_rejectsEscapeAndReadsChild() throws Exception {
    File base = tmp.toFile();
    File outside = tmp.getParent().resolve("escape-io.txt").toFile();
    Files.writeString(outside.toPath(), "no");
    assertThrows(
        IllegalArgumentException.class, () -> IOTools.getFileContentUnderBase(base, outside));

    Path child = tmp.resolve("in.txt");
    Files.writeString(child, "yes");
    assertEquals("yes", IOTools.getFileContentUnderBase(base, child.toFile()));
  }
}
