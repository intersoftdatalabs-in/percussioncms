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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserConfigurationTest {

  @TempDir Path tempHome;

  @Test
  void openCreatesIntsofRootUnderHome() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    Path root = config.getRootPath();
    assertTrue(Files.isDirectory(root));
    assertEquals(UserConfiguration.ROOT_DIR_NAME, root.getFileName().toString());
    assertEquals(tempHome.toAbsolutePath().normalize().resolve(".intsof"), root);
  }

  @Test
  void openIsIdempotentWhenRootAlreadyExists() throws IOException {
    UserConfiguration first = UserConfiguration.open(tempHome);
    UserConfiguration second = UserConfiguration.open(tempHome);
    assertEquals(first.getRootPath(), second.getRootPath());
    assertTrue(Files.isDirectory(first.getRootPath()));
  }

  @Test
  void openRejectsNullHome() {
    assertThrows(NullPointerException.class, () -> UserConfiguration.open(null));
  }

  @Test
  void createApplicationCreatesSubfolder() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    AppConfigurationFolder app = config.createApplication("percussion");
    assertEquals("percussion", app.getApplicationName());
    assertTrue(Files.isDirectory(app.getPath()));
    assertEquals(config.getRootPath().resolve("percussion"), app.getPath());
    assertTrue(config.applicationExists("percussion"));
  }

  @Test
  void createApplicationIsIdempotent() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    AppConfigurationFolder first = config.createApplication("my-app");
    Path marker = first.addFile("keep.txt");
    AppConfigurationFolder second = config.createApplication("my-app");
    assertEquals(first.getPath(), second.getPath());
    assertTrue(Files.isRegularFile(marker));
  }

  @Test
  void findApplicationReturnsEmptyWhenMissing() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    assertFalse(config.applicationExists("missing-app"));
    assertEquals(Optional.empty(), config.findApplication("missing-app"));
  }

  @Test
  void findApplicationReturnsHandleWhenPresent() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    config.createApplication("present");
    Optional<AppConfigurationFolder> found = config.findApplication("present");
    assertTrue(found.isPresent());
    assertEquals("present", found.get().getApplicationName());
  }

  @Test
  void rejectsInvalidApplicationNames() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    assertThrows(IllegalArgumentException.class, () -> config.createApplication(null));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication(""));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("  "));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication(".."));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("."));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("a/b"));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("a\\b"));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("C:foo"));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("CON"));
    assertThrows(IllegalArgumentException.class, () -> config.createApplication("nul.txt"));
    assertThrows(IllegalArgumentException.class, () -> config.findApplication("a/b"));
  }

  @Test
  void trimsApplicationName() throws IOException {
    UserConfiguration config = UserConfiguration.open(tempHome);
    AppConfigurationFolder app = config.createApplication("  trimmed  ");
    assertEquals("trimmed", app.getApplicationName());
    assertTrue(config.applicationExists("trimmed"));
  }

  @Test
  void openDefaultUsesUserHome() throws IOException {
    // Uses real user.home but only ensures .intsof exists under it; does not write app files.
    UserConfiguration config = UserConfiguration.openDefault();
    assertTrue(Files.isDirectory(config.getRootPath()));
    assertEquals(
        Path.of(System.getProperty("user.home"))
            .toAbsolutePath()
            .normalize()
            .resolve(UserConfiguration.ROOT_DIR_NAME),
        config.getRootPath());
  }
}
