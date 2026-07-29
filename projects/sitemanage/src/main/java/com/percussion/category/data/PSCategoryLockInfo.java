// REFACTORED: CP-JAVA11

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

import com.percussion.server.PSRequest;
import com.percussion.server.PSServer;
import com.percussion.server.PSUserSessionManager;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.service.IPSUserService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** Utility for managing lock info for category editing. */
public class PSCategoryLockInfo {

  private static final Logger log = LogManager.getLogger(PSCategoryLockInfo.class);
  private static final String LOCK_INFO_FILE = "lock_info.json";

  /**
   * Test-only override for {@link #resolveLegacyLockInfoFile()}. The legacy file was historically
   * resolved against the JVM current working directory (e.g. {@code new File("lock_info.json")}),
   * which the JVM computes once from {@code user.dir} at startup and does not re-read. To exercise
   * the backward-compat branch deterministically in unit tests we inject the path explicitly.
   */
  static volatile Path legacyLockInfoOverride;

  /**
   * Resolves the canonical lock-info path under the CMS installation directory. The lock file lives
   * under {@code $rxDir/lock_info.json} so that its location is independent of the JVM current
   * working directory (e.g. when running as a Windows service with a non-default cwd). GH-1565.
   *
   * @return the lock-file path, never {@code null}
   */
  static Path resolveLockInfoFile() {
    return Paths.get(PSServer.getRxDir().toURI()).resolve(LOCK_INFO_FILE);
  }

  /**
   * Legacy lock-info path: {@code lock_info.json} resolved against the JVM current working
   * directory. Used by installations created before GH-1565. Kept for backward-compatible reads
   * only — all writes go to {@link #resolveLockInfoFile()}.
   */
  static Path resolveLegacyLockInfoFile() {
    var override = legacyLockInfoOverride;
    return override != null ? override : Paths.get(LOCK_INFO_FILE);
  }

  /**
   * Writes lock info to file for the current user/session.
   *
   * @param userService user service, not null
   * @param date lock creation date
   * @throws PSDataServiceException on error
   */
  public static void writeLockInfoToFile(IPSUserService userService, String date)
      throws PSDataServiceException {
    var lockInfo = new JSONObject();
    var file = resolveLockInfoFile();

    var userName = userService.getCurrentUser().getName();
    var sessionId = PSRequest.getContextForRequest().getUserSessionId();
    try {
      var parent = file.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      lockInfo.put("userName", userName);
      lockInfo.put("sessionId", sessionId);
      lockInfo.put("creationDate", date);

      Files.writeString(file, lockInfo.toString(), StandardCharsets.UTF_8);
      log.debug("Created file with user information who has locked the Categories Tab.");
    } catch (IOException | JSONException e) {
      log.error("Exception writing lock info file - PSCategoryLockInfo.writeLockInfoToFile()", e);
    }
  }

  /**
   * Checks if the lock info file exists and is valid.
   *
   * @return true if locked, false otherwise
   */
  public static boolean isFileLocked() {
    return getLockInfo() != null;
  }

  /**
   * Reads lock info from file. Clears and returns null when the lock session is stale (no longer
   * exists) — GH-1182 / v8.1.7 PR #1173. Reads from the canonical CMS-installation location first
   * ({@link #resolveLockInfoFile()}); if that file is absent, falls back to the legacy cwd-relative
   * location for backward compatibility with pre-8.2 installs — GH-1565.
   *
   * @return lock info as JSONObject, or null if not found/invalid/stale
   */
  public static JSONObject getLockInfo() {
    var canonical = resolveLockInfoFile();
    var file = Files.exists(canonical) ? canonical : resolveLegacyLockInfoFile();
    if (!Files.exists(file)) {
      return null;
    }
    try {
      var lockInfoBytes = Files.readAllBytes(file);
      if (lockInfoBytes.length == 0) {
        return null;
      }
      var jsonObject = new JSONObject(new String(lockInfoBytes, StandardCharsets.UTF_8));
      if (isLockStale(jsonObject)) {
        removeLockInfo();
        return null;
      }
      return jsonObject;
    } catch (IOException | JSONException e) {
      log.error("Exception reading lock info file - PSCategoryLockInfo.getLockInfo()", e);
      return null;
    }
  }

  /**
   * Returns true when the lock's sessionId no longer maps to a live user session.
   *
   * <p>Package-visible for unit tests.
   */
  static boolean isLockStale(JSONObject jsonObject) {
    if (jsonObject == null) {
      return false;
    }
    try {
      var sessionId = jsonObject.getString("sessionId");
      if (StringUtils.isNotBlank(sessionId)
          && PSUserSessionManager.getUserSession(sessionId) == null) {
        log.debug(
            "Removing stale category tab lock for sessionId: {} - session no longer exists",
            sessionId);
        return true;
      }
    } catch (JSONException e) {
      log.error(
          "JSON Exception occurred while validating lock - PSCategoryLockInfo.isLockStale()", e);
    }
    return false;
  }

  /**
   * Removes the lock info file if it exists. Removes both the canonical and the legacy cwd-relative
   * locations (GH-1565). Does not call {@link #getLockInfo()} (would recurse into stale-session
   * checks).
   */
  public static void removeLockInfo() {
    for (var path : new Path[] {resolveLockInfoFile(), resolveLegacyLockInfoFile()}) {
      try {
        Files.deleteIfExists(path);
        log.debug("Lock info file deleted successfully.");
      } catch (IOException e) {
        log.warn("Unable to delete lock info file at {} - {}", path, e.getMessage());
      }
    }
  }
}
