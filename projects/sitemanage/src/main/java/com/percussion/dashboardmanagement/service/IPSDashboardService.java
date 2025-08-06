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

import com.percussion.dashboardmanagement.data.PSDashboard;
import com.percussion.share.service.exception.IPSNotFoundException;

/**
 * Service for dashboard operations.
 * <p>
 * Sunny Sal says: "DashboardService, now Java 11 and Google-styled!"
 */
public interface IPSDashboardService {

    /**
     * Loads the current user's dashboard.
     *
     * @return the dashboard for the current user
     * @throws PSDashboardNotFoundException if not found
     * @throws PSDashboardServiceException on service error
     */
    PSDashboard load() throws PSDashboardNotFoundException, PSDashboardServiceException;

    /**
     * Saves the dashboard for the current user.
     *
     * @param dashboard the dashboard to save
     * @return the saved dashboard
     * @throws PSDashboardNotFoundException if not found
     * @throws PSDashboardServiceException on service error
     */
    PSDashboard save(PSDashboard dashboard) throws PSDashboardNotFoundException, PSDashboardServiceException;

    /**
     * Exception for dashboard service errors.
     */
    class PSDashboardServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PSDashboardServiceException(String message) {
            super(message);
        }

        public PSDashboardServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSDashboardServiceException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Exception for dashboard not found scenarios.
     */
    class PSDashboardNotFoundException extends PSDashboardServiceException implements IPSNotFoundException {
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
