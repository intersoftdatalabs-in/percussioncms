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
package com.percussion.dashboardmanagement.service;

import com.percussion.dashboardmanagement.data.PSUserProfile;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.IPSNotFoundException;

/**
 * Service for managing user profiles.
 *
 * <p>Sunny Sal says: "User profiles, now Java 11 and Google-styled!"
 */
public interface IPSUserProfileService
    extends IPSDataService<PSUserProfile, PSUserProfile, String> {

  /**
   * Saves a user profile.
   *
   * @param profile the profile to save
   * @return the saved profile
   * @throws PSUserProfileServiceException on service error
   */
  PSUserProfile save(PSUserProfile profile) throws PSUserProfileServiceException;

  /**
   * Finds a user profile by username.
   *
   * @param userName the username
   * @return the user profile
   * @throws PSUserProfileNotFoundException if not found
   * @throws PSUserProfileServiceException on service error
   */
  PSUserProfile find(String userName)
      throws PSUserProfileNotFoundException, PSUserProfileServiceException;

  /** Exception for user profile service errors. */
  class PSUserProfileServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PSUserProfileServiceException(String message) {
      super(message);
    }

    public PSUserProfileServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSUserProfileServiceException(Throwable cause) {
      super(cause);
    }
  }

  /** Exception for user profile not found scenarios. */
  class PSUserProfileNotFoundException extends PSUserProfileServiceException
      implements IPSNotFoundException {
    private static final long serialVersionUID = 1L;

    public PSUserProfileNotFoundException(String message) {
      super(message);
    }

    public PSUserProfileNotFoundException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSUserProfileNotFoundException(Throwable cause) {
      super(cause);
    }
  }
}
