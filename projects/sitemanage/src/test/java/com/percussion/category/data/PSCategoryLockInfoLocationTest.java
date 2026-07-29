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

package com.percussion.category.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.server.PSServer;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies GH-1565: the lock file is written under the CMS installation directory
 * (PSServer.getRxDir()), not the JVM current working directory, and pre-existing cwd-relative
 * installations remain readable.
 */
class PSCategoryLockInfoLocationTest {

  @TempDir Path tempDir;

  private File previousRxDir;
  private Path previousLegacyOverride;

  @BeforeEach
  void setRxRoot() {
    previousRxDir = PSServer.getRxDir();
    previousLegacyOverride = PSCategoryLockInfo.legacyLockInfoOverride;
    PSServer.setRxDir(tempDir.toFile());
    PSCategoryLockInfo.legacyLockInfoOverride = null;
  }

  @AfterEach
  void restoreRxRoot() {
    if (previousRxDir != null) {
      PSServer.setRxDir(previousRxDir);
    }
    PSCategoryLockInfo.legacyLockInfoOverride = previousLegacyOverride;
  }

  @Test
  void resolveLockInfoFileLivesUnderRxDir() {
    var resolved = PSCategoryLockInfo.resolveLockInfoFile();
    assertEquals(tempDir.resolve("lock_info.json"), resolved);
    assertTrue(resolved.startsWith(tempDir), "lock file must live under the CMS installation dir");
  }

  @Test
  void removeLockInfoDeletesCanonicalFile() throws Exception {
    var canonical = tempDir.resolve("lock_info.json");
    Files.writeString(canonical, "{}", StandardCharsets.UTF_8);

    PSCategoryLockInfo.removeLockInfo();

    assertFalse(Files.exists(canonical), "canonical lock file should be deleted");
  }

  @Test
  void removeLockInfoDeletesLegacyFile() throws Exception {
    var legacyDir = Files.createTempDirectory("ps-legacy-lockinfo-rm");
    var legacyFile = legacyDir.resolve("lock_info.json");
    Files.writeString(legacyFile, "{}", StandardCharsets.UTF_8);
    PSCategoryLockInfo.legacyLockInfoOverride = legacyFile;

    PSCategoryLockInfo.removeLockInfo();

    assertFalse(Files.exists(legacyFile), "legacy lock file should be deleted");
  }

  @Test
  void getLockInfoReturnsNullWhenAbsent() {
    assertNull(PSCategoryLockInfo.getLockInfo());
    assertFalse(PSCategoryLockInfo.isFileLocked());
  }

  @Test
  void getLockInfoReadsCanonicalFile() throws Exception {
    var canonical = tempDir.resolve("lock_info.json");
    var json = new JSONObject();
    json.put("sessionId", "");
    json.put("userName", "canonical-tester");
    Files.writeString(canonical, json.toString(), StandardCharsets.UTF_8);

    var result = PSCategoryLockInfo.getLockInfo();
    assertNotNull(result, "canonical lock file must be readable");
    assertEquals("canonical-tester", result.getString("userName"));
  }

  @Test
  void getLockInfoFallsBackToLegacyLocation() throws Exception {
    // No canonical file, but a legacy lock_info.json exists at the override path.
    var legacyDir = Files.createTempDirectory("ps-legacy-lockinfo-fallback");
    var legacyFile = legacyDir.resolve("lock_info.json");
    var json = new JSONObject();
    json.put("sessionId", "");
    json.put("userName", "legacy-tester");
    Files.writeString(legacyFile, json.toString(), StandardCharsets.UTF_8);
    PSCategoryLockInfo.legacyLockInfoOverride = legacyFile;

    var result = PSCategoryLockInfo.getLockInfo();
    assertNotNull(result, "legacy lock file must remain readable for backward compat");
    assertEquals("legacy-tester", result.getString("userName"));
  }

  @Test
  void canonicalTakesPrecedenceOverLegacy() throws Exception {
    // Both present -> canonical wins.
    var canonical = tempDir.resolve("lock_info.json");
    var canonicalJson = new JSONObject();
    canonicalJson.put("sessionId", "");
    canonicalJson.put("userName", "canonical-tester");
    Files.writeString(canonical, canonicalJson.toString(), StandardCharsets.UTF_8);

    var legacyDir = Files.createTempDirectory("ps-legacy-lockinfo-precedence");
    var legacyFile = legacyDir.resolve("lock_info.json");
    var legacyJson = new JSONObject();
    legacyJson.put("sessionId", "");
    legacyJson.put("userName", "legacy-tester");
    Files.writeString(legacyFile, legacyJson.toString(), StandardCharsets.UTF_8);
    PSCategoryLockInfo.legacyLockInfoOverride = legacyFile;

    var result = PSCategoryLockInfo.getLockInfo();
    assertNotNull(result);
    assertEquals("canonical-tester", result.getString("userName"));
  }
}
