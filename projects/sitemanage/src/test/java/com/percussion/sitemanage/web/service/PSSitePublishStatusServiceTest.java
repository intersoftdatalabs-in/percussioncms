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
package com.percussion.sitemanage.web.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.sitemanage.data.PSSitePublishLogDetailsRequest;
import com.percussion.sitemanage.data.PSSitePublishLogRequest;
import com.percussion.sitemanage.data.PSSitePublishPurgeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

/** Integration tests for site publish status REST service. // REFACTORED: CP-JAVA11 */
@Tag("IntegrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PSSitePublishStatusServiceTest {

  private PSSitePublishStatusRestClient publishStatusClient;
  private static final Logger log = LogManager.getLogger(PSSitePublishStatusServiceTest.class);

  @BeforeEach
  public void setUp() throws Exception {
    publishStatusClient = new PSSitePublishStatusRestClient();
    // PSRestTestCase.setupClient(publishStatusClient); // Uncomment if needed for setup
  }

  @Test
  public void testGetCurrentJobs() throws Exception {
    var jobs = publishStatusClient.getCurrentJobs();
    log.debug("Jobs: {}", jobs);
    assertNotNull(jobs);
  }

  @Test
  public void testGetJobDetails() throws Exception {
    var r = new PSSitePublishLogDetailsRequest();
    r.setJobid(100);
    r.setShowOnlyFailures(false);
    r.setSkipCount(0);

    var items = publishStatusClient.getJobDetails(r);
    log.debug("Items: {}", items);
    assertNotNull(items);
  }

  @Test
  public void testGetLogs() throws Exception {
    var lr = new PSSitePublishLogRequest();
    lr.setDays(0);
    lr.setMaxcount(100);
    lr.setShowOnlyFailures(false);
    lr.setSkipCount(0);
    var logs = publishStatusClient.getLogs(lr);
    log.debug("logs: {}", logs);
    assertNotNull(logs);
  }

  @Test
  public void testPurgeLog() throws Exception {
    var pr = new PSSitePublishPurgeRequest();
    pr.setJobids(asList(100L, 200L));
    publishStatusClient.purgeLog(pr);
  }
}
