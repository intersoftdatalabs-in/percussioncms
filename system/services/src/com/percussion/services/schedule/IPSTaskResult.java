/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.schedule;

import java.util.Map;

/**
 * Represents the result of a scheduled task execution, including success status and notification details.
 * <p>Implementations should provide clear, user-friendly problem descriptions and notification variables for reporting.</p>
 *
 * @author Doug Rand
 */
public interface IPSTaskResult {
    /**
     * Indicates whether the scheduled task completed successfully.
     *
     * @return {@code true} if the task succeeded; {@code false} otherwise
     */
    boolean wasSuccess();

    /**
     * Provides a meaningful description of the failure if the task did not succeed.
     * The description should be understandable by end users, with any technical details secondary.
     *
     * @return the problem description, or {@code null} if the task was successful
     */
    String getProblemDescription();

    /**
     * Returns notification variables to be used when creating notification emails.
     *
     * @return a non-null map of notification variables; may be empty
     */
    Map<String, String> getNotificationVariables();
}
