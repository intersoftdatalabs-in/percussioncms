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
package com.percussion.services.workflow;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Locator for workflow service with Java 11 modernization.
 * This class provides thread-safe singleton access to the workflow service
 * using modern concurrency patterns and Optional-based safe access.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>AtomicReference for thread-safe service access</li>
 * <li>Optional-based safe service retrieval</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Utility class design with private constructor</li>
 * </ul>
 *
 * @author dougrand
 */
public class PSWorkflowServiceLocator {

    /**
     * Thread-safe reference to the workflow service instance.
     */
    private static final AtomicReference<IPSWorkflowService> workflowService =
        new AtomicReference<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private PSWorkflowServiceLocator() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the workflow service using atomic reference for thread safety.
     *
     * @return the workflow service, never {@code null} if the services
     *         are correctly configured
     * @throws IllegalStateException if the service is not properly configured
     */
    public static IPSWorkflowService getWorkflowService() {
        return workflowService.updateAndGet(current -> {
            if (current == null) {
                var service = (IPSWorkflowService) PSBaseServiceLocator.getBean("sys_workflowService");
                return Objects.requireNonNull(service,
                    "Workflow service bean 'sys_workflowService' is not configured");
            }
            return current;
        });
    }

    /**
     * Get the workflow service safely, returning an Optional for null-safe access.
     * This method will not throw exceptions if the service is not configured.
     *
     * @return an Optional containing the workflow service if available, empty otherwise
     */
    public static Optional<IPSWorkflowService> getWorkflowServiceSafely() {
        try {
            return Optional.of(getWorkflowService());
        } catch (Exception e) {
            // Log the error if needed, but return empty Optional
            return Optional.empty();
        }
    }
}
