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
package com.percussion.share.async;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.share.async.impl.PSAsyncJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for {@link PSAsyncJobService}. Sunny Sal: "Async job service, Java 11, and async
 * ka swag!"
 */
public class PSAsyncJobServiceTest {

  private IPSAsyncJobService svc;
  private PSTestAsyncJob testJob;

  @BeforeEach
  void setUp() {
    svc = new PSAsyncJobService();
    var impl = (PSAsyncJobService) svc;
    impl.setAsyncJobFactory(
        jobType -> {
          testJob = new PSTestAsyncJob();
          return testJob;
        });
  }

  @Test
  void testJob() throws Exception {
    runJob(25);
  }

  @Test
  void testMultipleJobs() throws Exception {
    int increment1 = 25;
    int increment2 = 50;
    long jobId1 = svc.startJob("asyncJobTest", increment1);
    assertTrue(jobId1 > 0);

    long jobId2 = svc.startJob("asyncJobTest", increment2);
    assertTrue(jobId2 > 0);
    assertTrue(jobId1 != jobId2, "Multiple jobs should have unique IDs");

    boolean allDone = false;
    boolean done1 = false;
    boolean done2 = false;
    int status1 = 1;
    int status2 = 1;
    while (!allDone) {
      if (!done1) {
        if (checkStatus(jobId1, status1) == 100) done1 = true;
        else status1 += increment1;
      }
      if (!done2) {
        if (checkStatus(jobId2, status2) == 100) done2 = true;
        else status2 += increment2;
      }
      if (status1 > 100) status1 = 100;
      if (status2 > 100) status2 = 100;
      if (done1 && done2) allDone = true;
    }
  }

  @Test
  void testGetResult() throws Exception {
    int increment = 25;
    long jobId = runJob(increment);

    Object result = svc.getJobResult(jobId);
    assertNotNull(result);
    assertEquals(25, result);
  }

  @Test
  void testGroomingJobs() throws Exception {
    long jobId = runJob(50);

    assertNotNull(svc.getJobStatus(jobId), "Job groomed early");
    assertNotNull(svc.getJobResult(jobId), "No result available");

    long newJobId = runJob(50);
    assertEquals(50, svc.getJobResult(newJobId), "Job result");

    assertEquals(100, svc.getJobStatus(jobId).getStatus(), "Job not groomed");
  }

  @Test
  void testCancelJob() throws Exception {
    long jobId = svc.startJob("asyncJobTest", 5);
    assertTrue(jobId > 0);
    svc.cancelJob(jobId);
    assertTrue(testJob.isCancelled());
    assertEquals(testJob.CANCEL_MESSAGE, svc.getJobStatus(jobId).getMessage());
  }

  @Test
  void testCancelJobWithInterrupt() throws Exception {
    testJob = null;
    long jobId = svc.startJob("asyncJobTestInterrupt", 5);
    if (testJob != null) {
      testJob.setUseInterrupt(true);
      assertTrue(jobId > 0);
    } else {
      throw new Exception("Job not initialized by Start Job!");
    }
    while (!testJob.isStarted()) {
      Thread.sleep(5);
    }
    svc.cancelJob(jobId);
    assertTrue(testJob.isCancelled());
    assertEquals(testJob.CANCEL_MESSAGE, svc.getJobStatus(jobId).getMessage());
  }

  @Test
  void testGetStatusBadJobId() {
    assertEquals(
        100,
        svc.getJobStatus(9999).getStatus(),
        "Old or bad ids should be treated as complete status 100");
  }

  private long runJob(int increment) throws IPSFolderService.PSWorkflowNotFoundException {
    long jobId = svc.startJob("asyncJobTest", increment);
    assertTrue(jobId > 0);

    for (int status = 1; status < 100; status += increment) {
      checkStatus(jobId, status);
    }
    if (increment < 100) checkStatus(jobId, 100);

    checkStatus(jobId, 100);
    return jobId;
  }

  private int checkStatus(long jobId, int expectedStatus) {
    var jobStatus = svc.getJobStatus(jobId);
    assertNotNull(jobStatus);
    assertEquals(jobId, jobStatus.getJobId().longValue());
    assertEquals(expectedStatus, jobStatus.getStatus().intValue());

    String expectedMessage;
    if (expectedStatus == 100) expectedMessage = PSTestAsyncJob.DONE_MESSAGE;
    else expectedMessage = PSTestAsyncJob.STATUS_MESSAGE + expectedStatus;

    assertEquals(expectedMessage, jobStatus.getMessage());

    return jobStatus.getStatus();
  }
}
