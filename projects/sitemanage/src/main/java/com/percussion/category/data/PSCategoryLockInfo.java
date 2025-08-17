// REFACTORED: CP-JAVA11

/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.service.IPSUserService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** Utility for managing lock info for category editing. */
public class PSCategoryLockInfo {

  private static final Logger log = LogManager.getLogger(PSCategoryLockInfo.class);
  private static final String LOCK_INFO_FILE = "lock_info.json";

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
    var file = new File(LOCK_INFO_FILE);

    var userName = userService.getCurrentUser().getName();
    var sessionId = PSRequest.getContextForRequest().getUserSessionId();
    try (var os = new FileOutputStream(file)) {
      lockInfo.put("userName", userName);
      lockInfo.put("sessionId", sessionId);
      lockInfo.put("creationDate", date);

      os.write(lockInfo.toString().getBytes(StandardCharsets.UTF_8));
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
   * Reads lock info from file.
   *
   * @return lock info as JSONObject, or null if not found/invalid
   */
  public static JSONObject getLockInfo() {
    var file = new File(LOCK_INFO_FILE);
    if (!file.exists()) {
      return null;
    }
    try (var is = new FileInputStream(file)) {
      var lockInfoBytes = is.readAllBytes();
      if (lockInfoBytes.length == 0) {
        return null;
      }
      return new JSONObject(new String(lockInfoBytes, StandardCharsets.UTF_8));
    } catch (IOException | JSONException e) {
      log.error("Exception reading lock info file - PSCategoryLockInfo.getLockInfo()", e);
      return null;
    }
  }

  /** Removes the lock info file if it exists. */
  public static void removeLockInfo() {
    var jsonObject = getLockInfo();
    if (jsonObject != null) {
      var file = new File(LOCK_INFO_FILE);
      if (file.exists() && file.delete()) {
        log.debug("Lock info file deleted successfully.");
      }
    }
  }
}
