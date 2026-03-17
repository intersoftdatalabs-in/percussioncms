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

package com.percussion.server.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Date;
import org.junit.jupiter.api.Test;

/** Unit test for the {@link PSCacheItem} class. */
public class PSCacheItemTest {
  // see base class

  /**
   * Test constructing the <code>PSCacheItem</code> class.
   *
   * @throws Exception if any errors occur.
   */
  @Test
  public void testCtor() throws Exception {
    PSCacheItem item = null;
    boolean didThrow = false;

    // test valid object
    String test = "myTest";
    Object[] keys = {"one", "two", "Three"};
    long size = 256;
    item = new PSCacheItem(0, test, keys, size);

    // test null object
    didThrow = false;
    try {
      item = new PSCacheItem(0, null, keys, size);
    } catch (Exception ex) {
      didThrow = true;
    }
    assertTrue(didThrow, "ctor with null object");

    // test null keys
    didThrow = false;
    try {
      item = new PSCacheItem(0, test, null, size);
    } catch (Exception ex) {
      didThrow = true;
    }
    assertTrue(didThrow, "ctor with null keys");

    // test negative size
    didThrow = false;
    try {
      item = new PSCacheItem(0, test, keys, -1);
    } catch (Exception ex) {
      didThrow = true;
    }
    assertTrue(didThrow, "ctor with negative size");
  }

  /**
   * Tests accessing a <code>PSCacheItem</code> once it is constructed.
   *
   * @throws Exception if any errors occur.
   */
  public void dontTestAccess() throws Exception {
    PSCacheItem item = null;
    String test = "myTest";
    Object[] keys = {"one", "two", "Three"};
    long size = 256;
    int id = 25;
    Date start = new Date();
    item = new PSCacheItem(id, test, keys, size);
    Date end = new Date();
    assertTrue(item.isInMemory(), "test is in memory after ctor");
    assertTrue(!item.isOnDisk(), "test is on disk after ctor");
    assertEquals(test, item.getObject(), "test getObject from memory");
    assertEquals(id, item.getCacheId(), "test getCacheid");
    assertEquals(keys, item.getKeys(), "test getKeys");
    assertEquals(size, item.getSize(), "test getSize");
    Date created = item.getCreatedDate();

    assertTrue(!created.before(start) && !created.after(end), "test getCreatedDate");

    Date accessed = item.getLastAccessedDate();
    assertTrue(!accessed.before(start) && !accessed.after(end), "test last accessed from ctor");

    start = new Date();
    Object o = item.getObject();
    end = new Date();
    accessed = item.getLastAccessedDate();
    assertTrue(
        !accessed.before(start) && !accessed.after(end), "test last accessed after getObject");

    assertTrue(item.isInMemory(), "test is in memory after access from memory");
    assertTrue(!item.isOnDisk(), "test is on disk after access from memory");

    item.release();
    assertNull(item.getObject());
  }

  /**
   * Tests storing a <code>PSCacheItem</code> on disk and then accessing it.
   *
   * @throws Exception if any errors occur.
   */
  @Test
  public void testDiskOps() throws Exception {
    PSCacheItem item = null;
    String test = "myTest";
    Object[] keys = {"one", "two", "Three"};
    long size = 256;
    int id = 25;
    item = new PSCacheItem(id, test, keys, size);

    assertTrue(item.isInMemory(), "item is in memory");

    item.toDisk(new File("."));
    assertTrue(item.isOnDisk(), "item is on disk");
    assertTrue(!item.isInMemory(), "item is not in memory");

    Date start = new Date();
    Object o = item.getObject();
    Date end = new Date();
    assertEquals(test, o, "item from disk is equal");
    assertTrue(item.isInMemory(), "item is in memory after getObject from disk");
    assertTrue(!item.isOnDisk(), "item is not on disk after getObject from disk");
    Date accessed = item.getLastAccessedDate();
    assertTrue(
        !accessed.before(start) && !accessed.after(end),
        "test last accessed after getObject from disk");

    item.toDisk(new File("."));
    item.release();
    assertNull(item.getObject());
  }

  /**
   * Tests using listeners for access and modified events.
   *
   * @throws Exception if any errors occur.
   */
  @Test
  public void testListeners() throws Exception {
    PSCacheItem item = null;
    String test = "myTest";
    Object[] keys = {"one", "two", "Three"};
    long size = 256;
    int id = 25;
    item = new PSCacheItem(id, test, keys, size);

    IPSCacheAccessedListener accessListener =
        new IPSCacheAccessedListener() {
          public void cacheAccessed(PSCacheEvent e) {
            m_accessAction = e.getAction();
            m_accessItem = (PSCacheItem) e.getObject();
          }
        };
    item.addCacheAccessedListener(accessListener);

    IPSCacheModifiedListener modifyListener =
        new IPSCacheModifiedListener() {
          public void cacheModified(PSCacheEvent e) {
            m_modifyAction = e.getAction();
            m_modifyItem = (PSCacheItem) e.getObject();
          }

          public void setCache(PSMultiLevelCache cache) {
            // noop
          }
        };
    item.addCacheModifiedListener(modifyListener);

    m_accessAction = -1;
    m_accessItem = null;
    item.getObject();
    assertEquals(
        PSCacheEvent.CACHE_ITEM_ACCESSED_FROM_MEMORY,
        m_accessAction,
        "access event should be CACHE_ITEM_ACCESSED_FROM_MEMORY");
    assertTrue(item == m_accessItem, "access listener should have received the item");

    m_modifyAction = -1;
    m_modifyItem = null;
    item.toDisk(new File("."));
    assertEquals(
        PSCacheEvent.CACHE_ITEM_STORED_TO_DISK,
        m_modifyAction,
        "modify event should be CACHE_ITEM_STORED_TO_DISK");
    assertTrue(item == m_modifyItem, "modify listener should have received the item");

    m_accessAction = -1;
    m_accessItem = null;
    m_modifyAction = -1;
    m_modifyItem = null;
    item.getObject();
    assertEquals(
        PSCacheEvent.CACHE_ITEM_ACCESSED_FROM_DISK,
        m_accessAction,
        "access event should be CACHE_ITEM_ACCESSED_FROM_DISK");
    assertTrue(item == m_accessItem, "access listener should have received the item");
  }

  // collect all tests into a TestSuite and return it - see base class

  /** Stores action caused by access listener event. */
  private int m_accessAction = -1;

  /** Stores item passed by access listener event. */
  private PSCacheItem m_accessItem = null;

  /** Stores action caused by modify listener event. */
  private int m_modifyAction = -1;

  /** Stores item passed by access listener event. */
  private PSCacheItem m_modifyItem = null;
}
