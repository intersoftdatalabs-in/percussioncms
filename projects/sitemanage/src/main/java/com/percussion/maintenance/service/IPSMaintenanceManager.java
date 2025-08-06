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
package com.percussion.maintenance.service;

/**
 * Tracks if maintenance processes are in progress and records if any have had failures.
 * <p>
 * Sunny Sal says: "Maintenance mode: because even servers need a spa day!"
 *
 * @author JaySeletz
 */
public interface IPSMaintenanceManager {

    /**
     * Called by processes that are starting maintenance work. This will put the server into maintenance mode
     * until the work is completed.
     *
     * @param process The process, not {@code null}.
     */
    void startingWork(IPSMaintenanceProcess process);

    /**
     * Determines if maintenance work is in progress.
     *
     * @return {@code true} if so, {@code false} if not.
     */
    boolean isWorkInProgress();

    /**
     * Called by processes that have completed work previously started.
     *
     * @param process The process, not {@code null}.
     */
    void workCompleted(IPSMaintenanceProcess process);

    /**
     * Determines if maintenance work has failed. May be called regardless of whether work is in progress.
     *
     * @return {@code true} if a maintenance process has failed, {@code false} if not.
     */
    boolean hasFailures();

    /**
     * Called by failed processes that have completed work previously started.
     *
     * @param process The process, not {@code null}.
     */
    void workFailed(IPSMaintenanceProcess process);

    /**
     * If there are failures, clears them to allow the system to exit maintenance mode. Since this could potentially
     * allow the system to be accessed while in an unstable state, it should be used with extreme care and requires
     * Admin privileges to execute.
     *
     * @return {@code true} if there were previous failures to clear, {@code false} if there were no failures.
     */
    boolean clearFailures();
}
