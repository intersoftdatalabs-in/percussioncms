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
package com.percussion.data.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.data.vfs.PSVirtualApplicationDirectory;
import com.percussion.design.objectstore.PSAclEntry;
import com.percussion.server.PSUserSession;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JDBC catalog {@code getPhysicalPath} must not resolve a user catalog string outside the
 * registered virtual directory (CodeQL {@code java/path-injection} #1985–#1987).
 */
@DisplayName("PSFileSystemDriver.getPhysicalPath path-injection barrier (CWE-22, #1985)")
class PSFileSystemDriverPathInjectionTest {

  @TempDir Path tmp;

  private String registeredName;

  @AfterEach
  void unregisterVdir() {
    if (registeredName != null) {
      PSFileSystemDriver.removeVirtualDirectory(registeredName);
      registeredName = null;
    }
  }

  @Test
  void getPhysicalPath_rejectsCatalogTraversal() throws Exception {
    File base = tmp.toFile();
    DirMap vdir = new DirMap(base);
    registeredName = vdir.getVirtualDirectory();
    PSFileSystemDriver.addVirtualDirectory(vdir);

    String catalog = registeredName + "/../../../escape.txt";
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PSFileSystemDriver.getPhysicalPath(
                catalog, null, PSAclEntry.AACE_DESIGN_READ));
  }

  @Test
  void getPhysicalPath_resolvesCatalogRootToPhysicalLocation() throws Exception {
    File base = tmp.toFile();
    DirMap vdir = new DirMap(base);
    registeredName = vdir.getVirtualDirectory();
    PSFileSystemDriver.addVirtualDirectory(vdir);

    File resolved =
        PSFileSystemDriver.getPhysicalPath(
            registeredName, null, PSAclEntry.AACE_DESIGN_READ);
    assertEquals(base.getCanonicalFile(), resolved.getCanonicalFile());
    assertTrue(resolved.getCanonicalFile().toPath().startsWith(base.getCanonicalFile().toPath()));
  }

  private static final class DirMap extends PSVirtualApplicationDirectory {
    DirMap(File f) {
      super(f.getName(), f, null);
    }

    @Override
    public boolean hasPermissions(PSUserSession session, int permissions) {
      return true;
    }
  }
}
