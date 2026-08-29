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
package com.percussion.data.vfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for {@link PSVirtualApplicationDirectory#getPhysicalPath} / {@link
 * PSVirtualApplicationDirectory#resolveUnderAppRoot} (CodeQL {@code java/path-injection} alerts
 * #1985–#1987). Catalog paths from the XML JDBC driver used to be joined onto the virtual
 * application root with {@code new File(appRoot, relPath).getCanonicalFile()} and no containment
 * check, so {@code ../../../etc/passwd} escaped the application directory.
 */
@DisplayName("PSVirtualApplicationDirectory path-injection barrier (CWE-22, #1985)")
class PSVirtualApplicationDirectoryPathInjectionTest {

  @TempDir Path tmp;

  @Test
  void getPhysicalPath_rejectsParentTraversalWhenRootExists() throws Exception {
    File base = tmp.toFile();
    PSVirtualApplicationDirectory vdir = new PSVirtualApplicationDirectory("virt", base, null);
    File traversal = new File(".." + File.separator + "escape.txt");
    assertThrows(IllegalArgumentException.class, () -> vdir.getPhysicalPath(traversal));
  }

  @Test
  void getPhysicalPath_rejectsParentTraversalWhenRootDoesNotExistYet() {
    File missing = tmp.resolve("not-created-yet").toFile();
    PSVirtualApplicationDirectory vdir = new PSVirtualApplicationDirectory("virt", missing, null);
    File traversal = new File(".." + File.separator + "escape.txt");
    assertThrows(IllegalArgumentException.class, () -> vdir.getPhysicalPath(traversal));
  }

  @Test
  void getPhysicalPath_resolvesDotToRoot() throws Exception {
    File base = tmp.toFile();
    PSVirtualApplicationDirectory vdir = new PSVirtualApplicationDirectory("virt", base, null);
    File resolved = vdir.getPhysicalPath(new File("."));
    assertEquals(base.getCanonicalFile(), resolved.getCanonicalFile());
  }

  @Test
  void getPhysicalPath_resolvesChildUnderRoot() throws Exception {
    File base = tmp.toFile();
    File child = new File(base, "sub");
    Files.createDirectory(child.toPath());
    PSVirtualApplicationDirectory vdir = new PSVirtualApplicationDirectory("virt", base, null);
    File resolved = vdir.getPhysicalPath(new File("sub"));
    assertEquals(child.getCanonicalFile(), resolved.getCanonicalFile());
    assertTrue(resolved.getCanonicalFile().toPath().startsWith(base.getCanonicalFile().toPath()));
  }

  @Test
  void getPhysicalPath_rejectsAbsoluteRelPath() {
    File base = tmp.toFile();
    PSVirtualApplicationDirectory vdir = new PSVirtualApplicationDirectory("virt", base, null);
    File abs = tmp.resolve("outside").toAbsolutePath().toFile();
    assertThrows(IllegalArgumentException.class, () -> vdir.getPhysicalPath(abs));
  }

  @Test
  void getPhysicalPath_nullAppRootReturnsNull() {
    PSVirtualApplicationDirectory vdir = new PSVirtualApplicationDirectory("virt", null, null);
    assertNull(vdir.getPhysicalPath(new File(".")));
  }
}
