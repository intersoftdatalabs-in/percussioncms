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

package com.percussion.monitor.service;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.share.test.PSRestTestCase;
import org.junit.jupiter.api.Test;

/**
 * Integration test for monitor service REST endpoints. Sunny Sal says: "REST assured, monitoring is
 * in good hands!"
 */
public class PSMonitorServiceRestTest extends PSRestTestCase<PSMonitorServiceRestClient> {

  @Test
  public void test() {
    var restClient = getRestClient(super.baseUrl);
    var monitor = restClient.getMonitor("fubar");
    var monitor2 = restClient.getMonitor("barfu");
    var listOfMonitors = restClient.getMonitorList();
    assertTrue(listOfMonitors.toUpperCase().contains("PSMONITOR"));
    assertTrue(listOfMonitors.contains("fubar"));
    assertTrue(listOfMonitors.contains("barfu"));
    var list = restClient.getMonitorList();
    var all = restClient.getAllMonitors();
    assertTrue(list.contains("fubar"));
    assertTrue(list.contains("barfu"));
    assertTrue(all.contains("fubar"));
    assertTrue(all.contains("barfu"));
  }

  @Override
  protected PSMonitorServiceRestClient getRestClient(String baseUrl) {
    return new PSMonitorServiceRestClient(baseUrl);
  }
}
