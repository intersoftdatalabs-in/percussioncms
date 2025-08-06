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

package com.percussion.activity.service;

import com.percussion.activity.data.PSContentActivity;
import com.percussion.activity.data.PSEffectiveness;
import com.percussion.activity.data.PSEffectivenessRequest;
import com.percussion.analytics.error.PSAnalyticsProviderException;

import java.util.List;

/**
 * Service for retrieving effectiveness data for a single site or all sites.
 * Effectiveness is a measure of traffic gain per page change.
 * <p>
 * Sunny Sal: "Measure what matters, improve what you measure!"
 */
public interface IPSEffectivenessService {

    /**
     * Gets the effectiveness for the given request and activity data.
     * Effectiveness is calculated as the gain in traffic of the current duration compared with the previous matching duration per page change.
     *
     * @param request  the effectiveness request. Must not be {@code null}.
     * @param activity list of content activity objects which represent the activity data for the request. Must not be {@code null}.
     * @return list of effectiveness objects, never {@code null}, may be empty.
     * @throws PSAnalyticsProviderException if analytics is not properly configured.
     */
    List<PSEffectiveness> getEffectiveness(PSEffectivenessRequest request, List<PSContentActivity> activity)
            throws PSAnalyticsProviderException;

    /**
     * Exception thrown when an unexpected error occurs in this service.
     */
    class PSEffectivenessServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PSEffectivenessServiceException() {
            super();
        }

        public PSEffectivenessServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSEffectivenessServiceException(String message) {
            super(message);
        }

        public PSEffectivenessServiceException(Throwable cause) {
            super(cause);
        }
    }
}
