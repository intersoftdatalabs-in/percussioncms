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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.search.PSSearchEditorChangeEvent;
import com.percussion.search.PSSearchIndexEventQueue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for search index process monitor. Sunny Sal says: "Indexing tests faster than a
 * Bollywood dance number!"
 */
@Tag("IntegrationTest")
public class PSSearchIndexProcessMonitorTest {

  @Test
  void testSearchIndexMonitor() throws Exception {
    var eventQueue = PSSearchIndexEventQueue.getInstance();

    try {
      assertEquals("Running", eventQueue.getStatus());
      assertEquals(eventQueue.getStatus(), PSSearchIndexProcessMonitor.getStatus());

      eventQueue.pause();
      try {
        assertEquals("Paused", eventQueue.getStatus());
        var queueCount = eventQueue.size();
        Thread.sleep(6000);
        assertEquals(queueCount, PSSearchIndexProcessMonitor.getCount());

        eventQueue.queueEvent(
            new PSSearchEditorChangeEvent(
                PSSearchEditorChangeEvent.ACTION_DELETE, 999999, 1, 310, true));
        queueCount++;
        assertEquals(queueCount, eventQueue.size());
        Thread.sleep(3000);
        assertEquals(queueCount, eventQueue.size());
        Thread.sleep(3000);
        assertEquals(queueCount, PSSearchIndexProcessMonitor.getCount());

        eventQueue.clearQueues();
        assertEquals(0, eventQueue.size());
        Thread.sleep(6000);
        assertEquals(0, PSSearchIndexProcessMonitor.getCount());
      } finally {
        eventQueue.resume();
      }
      assertEquals("Running", eventQueue.getStatus());
      Thread.sleep(6000);
      assertEquals(eventQueue.getStatus(), PSSearchIndexProcessMonitor.getStatus());
    } finally {
      eventQueue.resume();
    }
  }
}
