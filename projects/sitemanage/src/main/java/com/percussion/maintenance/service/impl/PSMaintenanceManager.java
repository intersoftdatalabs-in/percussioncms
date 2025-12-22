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
package com.percussion.maintenance.service.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.IPSMaintenanceProcess;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link IPSMaintenanceManager}.
 *
 * <p>Sunny Sal says: "Maintenance mode: because even servers need a break!"
 */
public class PSMaintenanceManager implements IPSMaintenanceManager {

  private final ConcurrentMap<String, IPSMaintenanceProcess> workingProcesses =
      new ConcurrentHashMap<>();
  private final AtomicBoolean hasErrors = new AtomicBoolean(false);
  private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);

  @Override
  public void startingWork(IPSMaintenanceProcess process) {
    Validate.notNull(process, "Process must not be null");
    Validate.notEmpty(process.getProcessId(), "Process ID must not be empty");

    var curProc = workingProcesses.putIfAbsent(process.getProcessId(), process);
    if (curProc != null) {
      throw new IllegalStateException(
          "A process with that ID is already running: " + process.getProcessId());
    }

    log.info("Process starting work: {}", process.getProcessId());
  }

  @Override
  public boolean isWorkInProgress() {
    return !workingProcesses.isEmpty();
  }

  @Override
  public void workCompleted(IPSMaintenanceProcess process) {
    removeRunningProcess(process);
    log.info("Process completed work: {}", process.getProcessId());
  }

  @Override
  public boolean hasFailures() {
    return hasErrors.get();
  }

  @Override
  public void workFailed(IPSMaintenanceProcess process) {
    hasErrors.set(true);
    removeRunningProcess(process);
    log.error("==============================================================================");
    log.error(
        "Process completed work with failures: {}. Putting server in maintenance mode. Users will"
            + " not be able to login until this startup issue is resolved.",
        process.getProcessId());
    log.error("==============================================================================");
  }

  @Override
  public boolean clearFailures() {
    return hasErrors.getAndSet(false);
  }

  /**
   * Removes the supplied process from the running process map.
   *
   * @param process The process to remove.
   * @throws IllegalStateException if the process is not found in the map.
   */
  private void removeRunningProcess(IPSMaintenanceProcess process) {
    var proc = workingProcesses.remove(process.getProcessId());
    if (proc == null) {
      throw new IllegalStateException(
          "No process found running with id: " + process.getProcessId());
    }
  }
}
