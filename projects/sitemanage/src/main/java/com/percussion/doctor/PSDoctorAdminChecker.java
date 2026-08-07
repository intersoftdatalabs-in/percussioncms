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
package com.percussion.doctor;

import com.intsof.percussioncms.doctor.api.DoctorAdminChecker;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.service.IPSUserService;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Admin gate for perc-doctor HTTP API using the CMS user service.
 *
 * <p>Returns {@code false} for anonymous callers, missing principals, and non-Admin users.
 */
public class PSDoctorAdminChecker implements DoctorAdminChecker {

  private final IPSUserService userService;

  /**
   * @param userService CMS user service; never null
   */
  public PSDoctorAdminChecker(IPSUserService userService) {
    this.userService = Objects.requireNonNull(userService, "userService");
  }

  @Override
  public boolean isCurrentUserAdmin() {
    try {
      PSCurrentUser current = userService.getCurrentUser();
      if (current == null) {
        return false;
      }
      String name = current.getName();
      if (StringUtils.isBlank(name)) {
        return false;
      }
      return userService.isAdminUser(name);
    } catch (PSDataServiceException e) {
      log.warn("Doctor admin check failed: {}", e.getMessage());
      return false;
    } catch (RuntimeException e) {
      // Includes PSNoCurrentUserException when no authenticated principal.
      log.debug("Doctor admin check: no current admin user ({})", e.toString());
      return false;
    }
  }

  private static final Logger log = LogManager.getLogger(PSDoctorAdminChecker.class);
}
