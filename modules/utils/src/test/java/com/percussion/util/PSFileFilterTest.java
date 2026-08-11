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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.tools.PSPatternMatcher;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
public class PSFileFilterTest {

  @TempDir Path tempDir;

  @Test
  public void attributeCtorAcceptsFilesOnly() throws Exception {
    Path file = Files.createFile(tempDir.resolve("a.txt"));
    Path dir = Files.createDirectory(tempDir.resolve("subdir"));

    PSFileFilter filter = new PSFileFilter(PSFileFilter.IS_FILE);
    assertTrue(filter.accept(file.toFile()));
    assertFalse(filter.accept(dir.toFile()));
  }

  @Test
  public void namePatternCtorMatchesPattern() throws Exception {
    Path match = Files.createFile(tempDir.resolve("note.txt"));
    Path other = Files.createFile(tempDir.resolve("note.md"));

    PSPatternMatcher matcher = new PSPatternMatcher('?', '*', "*.txt", false);
    PSFileFilter filter = new PSFileFilter(matcher);
    assertTrue(filter.accept(match.toFile()));
    assertFalse(filter.accept(other.toFile()));
  }

  @Test
  public void filenameFilterDelegateUsesSameRules() throws Exception {
    Files.createFile(tempDir.resolve("keep.log"));
    PSFileFilter filter = new PSFileFilter(PSFileFilter.IS_FILE);
    File dir = tempDir.toFile();
    assertTrue(filter.accept(dir, "keep.log"));
  }
}
