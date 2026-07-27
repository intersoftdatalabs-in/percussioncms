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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exclusive migrator lock (T045 / QC-019). */
@Tag("UnitTest")
public class PSMigratorLockTest {

  @TempDir Path installRoot;

  @Test
  void secondAcquireBlockedWhileFirstHeld() throws Exception {
    try (PSMigratorLock first = PSMigratorLock.tryAcquire(installRoot)) {
      assertNotNull(first.getLockPath());
      assertTrue(Files.isRegularFile(first.getLockPath()));
      assertThrows(
          PSMigratorLock.MigratorLockException.class,
          () -> {
            try (PSMigratorLock second = PSMigratorLock.tryAcquire(installRoot)) {
              // should not reach
            }
          });
    }
    // After release, acquire succeeds again
    try (PSMigratorLock third = PSMigratorLock.tryAcquire(installRoot)) {
      assertNotNull(third);
    }
  }

  @Test
  void lockPathIsUnderInstallerConfig() throws Exception {
    try (PSMigratorLock lock = PSMigratorLock.tryAcquire(installRoot)) {
      Path path = lock.getLockPath();
      assertTrue(path.toString().contains("rxconfig"));
      assertTrue(path.getFileName().toString().endsWith(".lock"));
    }
  }
}
