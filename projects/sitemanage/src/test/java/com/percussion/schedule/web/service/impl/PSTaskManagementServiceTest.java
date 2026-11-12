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
package com.percussion.schedule.web.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.schedule.IPSSchedulingService;
import com.percussion.services.schedule.data.PSScheduledTask;
import com.percussion.services.schedule.data.PSScheduledTaskLog;
import com.percussion.utils.guid.IPSGuid;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link PSTaskManagementService#getTaskLogs} must surface the owning schedule name
 * per log row (#6160: React per-log detail affordance replaces
 * {@code ui/admin/TaskDetailLog.jsp}) and degrade to a null {@code taskName} when
 * the schedule no longer exists. Guids are mocked: the static
 * {@code PSGuidManagerLocator} behind {@code PSGuidUtils.makeGuid} has no Spring
 * backing on the unit-test classpath.
 */
@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class PSTaskManagementServiceTest {

  @Mock private IPSSchedulingService schedulingService;

  @Mock private IPSGuidManager guidManager;

  private PSTaskManagementService service;

  @BeforeEach
  void setUp() {
    service = new PSTaskManagementService(schedulingService, guidManager);
  }

  @Test
  void getTaskLogsIncludesTaskNameFromScheduleJoin() throws Exception {
    IPSGuid taskId = taskGuid("task-guid-42");
    PSScheduledTask task = new PSScheduledTask();
    task.setId(taskId);
    task.setName("Nightly publish");
    PSScheduledTaskLog log =
        log(
            logGuid("log-guid-7"),
            taskId,
            new Date(1_000L),
            new Date(61_000L),
            true,
            null,
            "server-1");

    when(schedulingService.findAllSchedules()).thenReturn(List.of(task));
    when(schedulingService.findAllTaskLogs(100)).thenReturn(List.of(log));

    List<Map<String, Object>> logs = service.getTaskLogs();

    assertEquals(1, logs.size());
    assertEquals("Nightly publish", logs.get(0).get("taskName"));
    assertEquals("task-guid-42", logs.get(0).get("taskId"));
  }

  @Test
  void getTaskLogsNullsTaskNameWhenScheduleDeleted() throws Exception {
    IPSGuid orphanTaskId = logGuid("task-guid-99");
    PSScheduledTaskLog log =
        log(
            logGuid("log-guid-8"),
            orphanTaskId,
            new Date(1_000L),
            new Date(2_000L),
            false,
            "boom",
            "server-1");

    when(schedulingService.findAllSchedules()).thenReturn(List.of());
    when(schedulingService.findAllTaskLogs(100)).thenReturn(List.of(log));

    List<Map<String, Object>> logs = service.getTaskLogs();

    assertEquals(1, logs.size());
    assertNull(logs.get(0).get("taskName"));
  }

  @Test
  void getTaskLogsSurvivesScheduleStoreFailure() throws Exception {
    IPSGuid taskId = logGuid("task-guid-42");
    PSScheduledTaskLog log =
        log(
            logGuid("log-guid-9"),
            taskId,
            new Date(1_000L),
            new Date(2_000L),
            true,
            null,
            null);

    when(schedulingService.findAllSchedules())
        .thenThrow(new RuntimeException("schedule store down"));
    when(schedulingService.findAllTaskLogs(100)).thenReturn(List.of(log));

    List<Map<String, Object>> logs = service.getTaskLogs();

    assertEquals(1, logs.size());
    assertNull(logs.get(0).get("taskName"));
  }

  /** Schedule id: also stubs the type ordinal checked by {@code PSJob.setId}. */
  private static IPSGuid taskGuid(String text) {
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.getType()).thenReturn(PSTypeEnum.SCHEDULED_TASK.getOrdinal());
    when(guid.toString()).thenReturn(text);
    return guid;
  }

  /** Log/task id carried by a mocked {@link PSScheduledTaskLog}: text form only. */
  private static IPSGuid logGuid(String text) {
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn(text);
    return guid;
  }

  private static PSScheduledTaskLog log(
      IPSGuid id,
      IPSGuid taskId,
      Date start,
      Date end,
      boolean success,
      String problem,
      String server) {
    PSScheduledTaskLog log = mock(PSScheduledTaskLog.class);
    when(log.getId()).thenReturn(id);
    when(log.getTaskId()).thenReturn(taskId);
    when(log.getStartTime()).thenReturn(start);
    when(log.getEndTime()).thenReturn(end);
    when(log.isSuccess()).thenReturn(success);
    when(log.getProblemDesc()).thenReturn(problem);
    when(log.getServer()).thenReturn(server);
    return log;
  }
}
