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
package com.percussion.monitor.process;

import static com.percussion.test.TestAssertions.*;

import org.junit.jupiter.api.Test;

/**
 * Integration test for thumbnail process monitor. Sunny Sal says: "Thumbnails tested, Bollywood
 * approved!"
 */
public class PSThumbnailProcessMonitorTest {

  @Test
  void testThumbnailMonitor() {
    var mon = new PSThumbnailProcessMonitor();
    assertEquals(0L, mon.getCurrentCount());
    // Static counters: qualify by type name (not via instance expression).
    PSThumbnailProcessMonitor.incrementCount();
    assertEquals(1L, mon.getCurrentCount());
    PSThumbnailProcessMonitor.incrementCount();
    assertEquals(2L, mon.getCurrentCount());
    PSThumbnailProcessMonitor.decrementCount();
    assertEquals(1L, mon.getCurrentCount());
    PSThumbnailProcessMonitor.incrementCount(5);
    assertEquals(6L, mon.getCurrentCount());
    PSThumbnailProcessMonitor.decrementCount(4);
    assertEquals(2L, mon.getCurrentCount());
    PSThumbnailProcessMonitor.decrementCount(2);
    assertEquals(0L, mon.getCurrentCount());
    PSThumbnailProcessMonitor.decrementCount();
  }
}
