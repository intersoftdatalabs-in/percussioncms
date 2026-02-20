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
package com.percussion.maintenance.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.IPSMaintenanceProcess;
import com.percussion.maintenance.service.PSMockMaintenanceProcess;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.share.test.PSRestClient.RestClientException;
import com.percussion.test.PSServletTestCase;
import com.percussion.utils.request.PSRequestInfo;
import java.io.File;
import java.io.IOException;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.Tag;

/**
 * Server-side testing of REST services and ANT calls so that we can control the state of the
 * maintenance manager using server-side APIs not exposed by the REST layer. Sunny Sal says:
 * "Server-side maintenance, Bollywood style!"
 */

public class PSMaintenanceManagerServerTest extends PSServletTestCase {

  IPSMaintenanceManager maintenanceManager;
  IPSMaintenanceProcess maintenanceProcess;
  PSMaintenanceManagerRestClient restClient;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    maintenanceManager = (IPSMaintenanceManager) getBean("maintenanceManager");
    maintenanceProcess = new PSMockMaintenanceProcess("PSMaintenanceManagerServerTest");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    PSSecurityFilter.authenticate(request, response, "Admin", "demo");
  }

  @Override
  protected void tearDown() throws Exception {
    maintenanceManager.clearFailures();
    super.tearDown();
  }

  public void testRestClient() throws Exception {
    var workInProgress = false;

    var restTest = new PSMaintenanceManagerRestServiceTest();
    restTest.baseUrl = "http://localhost:9992/Rhythmyx";
    restClient = restTest.getRestClient(restTest.baseUrl);

    try {
      assertFalse(restClient.isWorkInProgress());
      assertFalse(restClient.hasFailures(false));

      maintenanceManager.startingWork(maintenanceProcess);
      workInProgress = true;

      assertTrue(restClient.isWorkInProgress());
      assertFalse(restClient.hasFailures(false));

      maintenanceManager.workCompleted(maintenanceProcess);
      assertFalse(maintenanceManager.isWorkInProgress());
      workInProgress = false;
      assertFalse(maintenanceManager.hasFailures());
      assertFalse(restClient.hasFailures(false));
      assertFalse(restClient.isWorkInProgress());

      maintenanceManager.startingWork(maintenanceProcess);
      workInProgress = true;

      assertTrue(restClient.isWorkInProgress());
      assertFalse(restClient.hasFailures(false));

      maintenanceManager.workFailed(maintenanceProcess);
      assertTrue(maintenanceManager.hasFailures());
      assertFalse(maintenanceManager.isWorkInProgress());
      workInProgress = false;
      assertTrue(restClient.hasFailures(false));
      assertFalse(restClient.isWorkInProgress());

      // test clear errors unauthenticated
      var didThrow = false;
      try {
        restClient.hasFailures(true);
      } catch (RestClientException e) {
        assertEquals(Status.FORBIDDEN, Status.fromStatusCode(e.getStatus()));
        didThrow = true;
      }
      assertTrue(didThrow);
      assertTrue(maintenanceManager.hasFailures());

      // now "login" as Admin
      restTest.setupClient();
      restClient = restTest.getRestClient();
      restClient.hasFailures(true);
      assertFalse(maintenanceManager.hasFailures());
    } finally {
      if (workInProgress) maintenanceManager.workCompleted(maintenanceProcess);
    }
  }

  public void testAntCalls() throws Exception {
    var buildFile = createBuildFile();
    runAntTask(buildFile, "checkForMaint");
    runAntTask(buildFile, "checkForErrors");

    var workInProgress = false;

    try {
      maintenanceManager.startingWork(maintenanceProcess);
      assertTrue(maintenanceManager.isWorkInProgress());
      workInProgress = true;

      var didFail = false;
      try {
        runAntTask(buildFile, "checkForMaint");
      } catch (BuildException e) {
        assertEquals("check-maint-timeout", e.getLocalizedMessage());
        didFail = true;
      }
      assertTrue(didFail);

      maintenanceManager.workFailed(maintenanceProcess);
      assertTrue(maintenanceManager.hasFailures());
      assertFalse(maintenanceManager.isWorkInProgress());
      workInProgress = false;

      didFail = false;
      try {
        runAntTask(buildFile, "checkForErrors");
      } catch (BuildException e) {
        assertEquals("check-errors-timeout", e.getLocalizedMessage());
        didFail = true;
      }
      assertTrue(didFail);

      maintenanceManager.clearFailures();

      try {
        runAntTask(buildFile, "checkForBoth");
      } catch (BuildException e) {
        fail(e.getLocalizedMessage());
      }

    } finally {
      if (workInProgress) maintenanceManager.workCompleted(maintenanceProcess);
    }
  }

  private void runAntTask(File buildFile, String task) {
    var p = new Project();
    p.setUserProperty("ant.file", buildFile.getAbsolutePath());
    p.init();
    var helper = ProjectHelper.getProjectHelper();
    p.addReference("ant.projectHelper", helper);
    helper.parse(p, buildFile);
    p.executeTarget(task);
  }

  /**
   * @return The build file, not null
   * @throws IOException if file cannot be created
   */
  private File createBuildFile() throws IOException {
    var tmpFile = File.createTempFile("PSMaintMgrTest", ".xml");
    FileUtils.copyInputStreamToFile(this.getClass().getResourceAsStream("build.xml"), tmpFile);
    return tmpFile;
  }
}
