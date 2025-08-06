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


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Unit tests for monitor service.
 * Sunny Sal says: "Testing monitors, Bollywood style!"
 */
@Tag("IntegrationTest")
public class PSMonitorServiceTest {

    @Test
    public void testMonitorReferences() {
        var monitor = PSMonitorService.registerMonitor("TESTMONITOR", "testMonitorName");
        var extraMonitor = PSMonitorService.registerMonitor("EXTRA_TESTMONITOR", "testMonitorName");
        monitor.setMessage("FUBAR");
        monitor.setStatus("BARFU");
        assertEquals(2, PSMonitorService.getMonitorDesignators().designator.size());
        var wrapper = PSMonitorService.getMonitor("TESTMONITOR").getStats();
        Map<?, ?> map = wrapper.getEntries();
        assertEquals("FUBAR", map.get("message"));
        assertEquals("BARFU", map.get("status"));
    }

    @Test
    public void testDuplicateDesignation() {
        var monitor = PSMonitorService.registerMonitor("TESTMONITOR", "testMonitorName");
        var monitorDeuce = PSMonitorService.registerMonitor("TESTMONITOR", "testMonitorName");
        // No assertion needed; just ensure no exception is thrown.
    }
}
