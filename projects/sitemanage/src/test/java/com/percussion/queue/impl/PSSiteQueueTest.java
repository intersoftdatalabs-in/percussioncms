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

package com.percussion.queue.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.monitor.process.PSImportProcessMonitor;
import com.percussion.sitemanage.data.PSSite;
import java.util.ArrayList;
import org.junit.jupiter.api.*;

/** Unit tests for {@link PSSiteQueue}. Sunny Sal: "Queue ka hero ban gaya tu!" */
class PSSiteQueueTest {

  private static final String USER_AGENT = "FAKE AGENT";
  private static final PSImportProcessMonitor monitor = new PSImportProcessMonitor();

  @Test
  void testInit() {
    var site = new PSSite();
    var queue = new PSSiteQueue(site, USER_AGENT);

    assertEquals(0, queue.getImportingIds().size());
    assertTrue(queue.getCatalogedIds().isEmpty());
    assertTrue(queue.getImportedIds().isEmpty());
    assertEquals(0, monitor.getCatalogCount());
  }

  @Test
  void testAllButMaxCount() {
    var queue = createSiteQueue();

    assertEquals(2, queue.getCatalogedIds().size());
    assertEquals(10, queue.getCatalogedIds().get(0));
    assertEquals(20, queue.getCatalogedIds().get(1));
    assertEquals(0, queue.getImportingIds().size());
    assertEquals(2, monitor.getCatalogCount());

    queue.setMaxImportCount(-1);
    var importingId = queue.getNextId();

    assertEquals(10, importingId);
    assertEquals(1, queue.getCatalogedIds().size());
    assertEquals(20, queue.getCatalogedIds().get(0));
    assertEquals(1, monitor.getCatalogCount());

    importingId = queue.getNextId();
    assertEquals(20, importingId);
    assertEquals(0, queue.getCatalogedIds().size());
    assertEquals(0, monitor.getCatalogCount());

    importingId = queue.getNextId();
    assertNull(importingId);

    assertEquals(2, queue.getImportingIds().size());
  }

  @Test
  void testRemoveImportingId() {
    var queue = createSiteQueue();
    assertEquals(2, queue.getCatalogedIds().size());

    queue.setMaxImportCount(-1);
    assertEquals(0, queue.getImportingIds().size());

    var id = queue.getNextId();
    assertNotNull(id);

    assertEquals(1, queue.getCatalogedIds().size());

    queue.removeImportingId(id);

    assertNotNull(queue.getNextId());
    assertTrue(queue.getImportingIds().size() > 0);

    assertEquals(0, queue.getCatalogedIds().size());

    assertNull(queue.getNextId());
    assertEquals(0, queue.getCatalogedIds().size());
  }

  @Test
  void testRemoveImportedPageId() {
    var queue = createSiteQueue(2, 10, 20);
    assertEquals(10, queue.getImportedIds().size());

    var ids = queue.getImportedIds();
    assertEquals(1, ids.get(0));
    assertEquals(6, ids.get(5));

    queue.removeImportedId(6);
    assertEquals(9, queue.getImportedIds().size());
  }

  @Test
  void testMaxImportCountNoLimit() {
    var queue = createSiteQueue();
    assertEquals(2, queue.getCatalogedIds().size());

    queue.setMaxImportCount(-1);
    while (queue.getNextId() != null) {
      // Loop until all cataloged IDs are imported
    }
    assertEquals(0, queue.getCatalogedIds().size());
  }

  @Test
  void testMaxImportCount() {
    var queue = createSiteQueue();

    assertEquals(0, queue.getMaxImportCount());
    assertEquals(2, queue.getCatalogedIds().size());
    assertEquals(2, monitor.getCatalogCount());

    assertNull(queue.getNextId());
    assertEquals(2, queue.getCatalogedIds().size());
    assertEquals(0, monitor.getCatalogCount());

    queue.setMaxImportCount(1);
    assertNull(queue.getNextId());
    assertEquals(2, queue.getCatalogedIds().size());

    queue.setMaxImportCount(2);
    assertNull(queue.getNextId());
    assertEquals(2, queue.getCatalogedIds().size());

    queue.setMaxImportCount(4);
    assertNotNull(queue.getNextId());
    assertNotNull(queue.getNextId());
    assertEquals(0, queue.getCatalogedIds().size());

    assertNull(queue.getNextId());
    assertEquals(0, queue.getCatalogedIds().size());
  }

  @Test
  void testContainsPagesForImport() {
    var queue = createSiteQueue(10, 2, 2);
    assertEquals(0, queue.getImportingIds().size());
    assertFalse(queue.containsPagesForImport());
    assertEquals(10, monitor.getCatalogCount());

    queue = createSiteQueue(10, 2, 4);
    assertEquals(0, queue.getImportingIds().size());
    assertTrue(queue.containsPagesForImport());
    assertEquals(10, monitor.getCatalogCount());

    var importingId = queue.getNextId();
    assertTrue(queue.getImportingIds().size() > 0);
    assertNotNull(importingId);
    assertTrue(queue.containsPagesForImport());
    assertEquals(9, monitor.getCatalogCount());

    importingId = queue.getNextId();
    assertTrue(queue.getImportingIds().size() > 0);
    assertNotNull(importingId);
    assertEquals(8, monitor.getCatalogCount());
  }

  private PSSiteQueue createSiteQueue() {
    return createSiteQueue(2, 2, 0);
  }

  private PSSiteQueue createSiteQueue(int catalogCount, int importCount, int maxImport) {
    var site = new PSSite();
    var queue = new PSSiteQueue(site, USER_AGENT);
    queue.setMaxImportCount(maxImport);

    var catalogedIds = new ArrayList<Integer>();
    for (int i = 1; i <= catalogCount; i++) {
      catalogedIds.add(i * 10);
    }
    queue.addCatalogedIds(catalogedIds);

    var importedIds = new ArrayList<Integer>();
    for (int i = 1; i <= importCount; i++) {
      importedIds.add(i);
    }
    queue.addImportedIds(importedIds);
    return queue;
  }
}
