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
package com.percussion.integritymanagement.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.integritymanagement.data.PSIntegrityStatus;
import com.percussion.integritymanagement.data.PSIntegrityStatus.Status;
import com.percussion.integritymanagement.data.PSIntegrityTask;
import com.percussion.integritymanagement.data.PSIntegrityTask.TaskStatus;
import com.percussion.integritymanagement.service.IPSIntegrityCheckerService.IntegrityTaskType;
import com.percussion.server.PSRequest;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;

/**
 * Integration tests for Integrity Checker Service. Sunny Sal says: "Integrity is doing the right
 * thing, even when no one is watching... or testing!"
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSIntegrityCheckerServiceTest extends PSServletTestCase {

  private PSIntegrityCheckerService service;
  private IPSSecurityWs securityWs;
  private IPSUtilityService utilityService;

  @Override
  protected void setUp() throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
    init("Admin", "demo", "Default");
    super.setUp();
  }

  @SuppressWarnings("unchecked")
  public void init(String uid, String pwd, String community) throws Exception {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
    PSRequestInfo.resetRequestInfo();
    var req = PSRequest.getContextForRequest();
    PSRequestInfo.initRequestInfo((Map<?, ?>) null);
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_PSREQUEST, req);
    setSecurityWs(PSSecurityWsLocator.getSecurityWebservice());
    securityWs.login(request, response, uid, pwd, null, community, null);
  }

  @Test
  void testIntegrityService() throws PSDataServiceException {
    if (utilityService.isSaaSEnvironment()) {
      var status = start();
      assertNotNull(status);
      assertEquals(Status.SUCCESS, status.getStatus());
      Set<PSIntegrityTask> tasks = status.getTasks();
      for (var task : tasks) {
        assertEquals(TaskStatus.SUCCESS, task.getStatus());
      }
      service.delete(status.getToken());
      status = service.getStatus(status.getToken());
      assertNull(status);
    }
  }

  @Test
  void testIntegrityServiceHistory() throws PSDataServiceException {
    if (utilityService.isSaaSEnvironment()) {
      List<PSIntegrityStatus> statuses = service.getHistory();
      int initialSize = statuses.size();
      start();
      statuses = service.getHistory();
      int curSize = statuses.size();
      assertEquals(initialSize + 1, curSize);
      start();
      statuses = service.getHistory();
      curSize = statuses.size();
      assertEquals(initialSize + 2, curSize);
      for (var status : statuses) {
        service.delete(status.getToken());
      }
      statuses = service.getHistory();
      assertEquals(0, statuses.size());
    }
  }

  private PSIntegrityStatus start() throws PSDataServiceException {
    var token = service.start(IntegrityTaskType.cm1);
    long startTime = new Date().getTime();
    long endTime = startTime;
    boolean processed = false;
    PSIntegrityStatus status = null;
    while (!processed && endTime - startTime < 10000) {
      status = service.getStatus(token);
      if (!Status.RUNNING.equals(status.getStatus())) {
        processed = true;
      } else {
        endTime = new Date().getTime();
      }
    }
    return status;
  }

  public PSIntegrityCheckerService getService() {
    return service;
  }

  public void setService(PSIntegrityCheckerService service) {
    this.service = service;
  }

  public IPSSecurityWs getSecurityWs() {
    return securityWs;
  }

  public void setSecurityWs(IPSSecurityWs securityWs) {
    this.securityWs = securityWs;
  }

  public IPSUtilityService getUtilityService() {
    return utilityService;
  }

  public void setUtilityService(IPSUtilityService utilityService) {
    this.utilityService = utilityService;
  }
}
