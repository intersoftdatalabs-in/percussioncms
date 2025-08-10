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
// REFACTORED: CP-JAVA11
package com.percussion.dashboardmanagement.service;

import com.percussion.dashboardmanagement.data.PSDashboard;
import com.percussion.error.PSException;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.IPSNotFoundException;

/**
 * Data service for managing dashboards.
 *
 * <p>Sunny Sal says: "Dashboards, now with Java 11 power and Google style!"
 */
public interface IPSDashboardDataService extends IPSDataService<PSDashboard, PSDashboard, String> {

  /** Exception for dashboard user service errors. */
  class PSDashboardUserServiceException extends PSException {
    private static final long serialVersionUID = 1L;

    public PSDashboardUserServiceException(String message) {
      super(message);
    }

    public PSDashboardUserServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSDashboardUserServiceException(Throwable cause) {
      super(cause);
    }
  }

  /** Exception for dashboard not found scenarios. */
  class PSDashboardNotFoundException extends PSDashboardUserServiceException
      implements IPSNotFoundException {
    private static final long serialVersionUID = 1L;

    public PSDashboardNotFoundException(String message) {
      super(message);
    }

    public PSDashboardNotFoundException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSDashboardNotFoundException(Throwable cause) {
      super(cause);
    }
  }
}
