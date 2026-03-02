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

import java.util.ArrayList;
import java.util.List;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class PSMultiLevelCacheTest
{
   // see base class



   /**
    * Test creating caches
    *
    * @throws Exception if the test fails or anything goes wrong.
    */

   @Test
   public void testCtor() throws Exception
   {
      PSMultiLevelCache testCache = null;
      boolean didThrow;

      // should succeed
      testCache = new PSMultiLevelCache(1, -1);
      testCache = new PSMultiLevelCache(1, 5);
      testCache = new PSMultiLevelCache(6, 25);

      // test invalid keysize
      try
      {
         didThrow = false;
         testCache = new PSMultiLevelCache(0, -1);
      }
      catch (Exception e)
      {
         didThrow = true;
      }
      assertTrue(didThrow, "allowed invalid keysize");

      try
      {
         didThrow = false;
         testCache = new PSMultiLevelCache(-1, 5);
      }
      catch (Exception e)
      {
         didThrow = true;
      }
      assertTrue(didThrow, "allowed invalid keysize");


      // test invalid aging
      try
      {
         didThrow = false;
         testCache = new PSMultiLevelCache(1, 0);
      }
      catch (Exception e)
      {
         didThrow = true;
      }
      assertTrue(didThrow, "allowed invalid agingTime");

   }

   /**
    * Test adding and retrieving items from the cache
    *
    * @throws Exception if the test fails or anything goes wrong.
    */

   @Test
   public void testAccess() throws Exception
   {
      // test one level
      PSMultiLevelCache cache;
      cache = new PSMultiLevelCache(1, -1);
      addAndRetrieve(cache, new Object[] {"a"}, "itema", 1);
      addAndRetrieve(cache, new Object[] {"b"}, "itemb", 0);
      addAndRetrieve(cache, new Object[] {"b"}, "itemb2", 0);
      addAndRetrieve(cache, new Object[] {"c"}, Integer.valueOf(5), 256);

      // test invalid params
      addBadItem(cache, null, "bad", 1);
      addBadItem(cache, new Object[] {"a", "b"}, "bad", 1);
      addBadItem(cache, new Object[] {null}, "bad", 1);
      addBadItem(cache, new Object[] {"a"}, null, 1);
      addBadItem(cache, new Object[] {"b"}, "itemb", -1);

      getBadItem(cache, new Object[] {null});
      getBadItem(cache, new Object[] {"a", "b"});
      getBadItem(cache, new Object[] {});

      // test 3 levels
      cache = new PSMultiLevelCache(3, -1);
      int x = 0;
      for (int i = 0; i < 3; i++)
      {
         for (int j = 0; j < 3; j++)
         {
            for (int k = 0; k < 3; k++, x++)
            {
               addAndRetrieve(cache, new Object[] {i + "", j + "", k + ""},
                  Integer.valueOf(x), x + 1);
            }
         }
      }

      // test miss
      assertNull(cache.retrieveItem(new Object[] {"1", "2", "4"}, CACHE_ITEM_TYPE));

      // test invalid keys
      addBadItem(cache, null, "bad", 1);
      addBadItem(cache, new Object[] {"a", "b"}, "bad", 2);
      addBadItem(cache, new Object[] {"a", null, "c"}, "bad", 3);
      addBadItem(cache, new Object[] {null, "b", "c"}, "bad", 4);
      addBadItem(cache, new Object[] {"a", "b", null}, "bad", 5);
      addBadItem(cache, new Object[] {"a", "b", "c", "d"}, "bad", 6);

      getBadItem(cache, new Object[] {"a", "b"});
      getBadItem(cache, new Object[] {"a", "b", "c", "d"});
      getBadItem(cache, new Object[] {"a", null, "c"});
      getBadItem(cache, new Object[] {null, "b", "c"});
      getBadItem(cache, new Object[] {"a", "b", null});
   }

   /**
    * Test various flush operations
    *
    * @throws Exception
    */

   @Test
   public void testFlush() throws Exception
   {
      PSMultiLevelCache cache;
      cache = new PSMultiLevelCache(3, -1);

      // load cache
      int x = 0;
      for (int i = 0; i < 3; i++)
      {
         for (int j = 0; j < 3; j++)
         {
            for (int k = 0; k < 3; k++, x++)
            {
               addAndRetrieve(cache, new Object[] {i + "", j + "", k + ""},
                  Integer.valueOf(x), x + 1);
            }
         }
      }

      // now flush a leaf and be sure we can still get others in that branch
      Object[] keys = {"0", "1", "2"};
      cache.flush(keys);
      assertNull(cache.retrieveItem(keys, CACHE_ITEM_TYPE), "retrieve flushed item returns null");

      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "1", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "1", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "1", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "1", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "1", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "1", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "1", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "1", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");

      // flush a branch
      cache.flush(new Object[] {"0", "2", null});
      assertNull(cache.retrieveItem(new Object[] {"0", "2", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"0", "2", "1"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"0", "2", "2"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "1", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "1", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "1", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "1", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "1", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "1", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "1", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "1", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");


      // flush middle branch
      cache.flush(new Object[] {null, "1", null});
      assertNull(cache.retrieveItem(new Object[] {"0", "1", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"0", "1", "1"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "1", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "1", "1"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "1", "2"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "1", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "1", "1"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "1", "2"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");

      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"0", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");



      // flush top branch
      cache.flush(new Object[] {"0", null, null});
      assertNull(cache.retrieveItem(new Object[] {"0", "0", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"0", "0", "1"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"0", "0", "2"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");

      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");

      // flush based on partial key
      cache.flush(new Object[] {"2", null, "0"});
      assertNull(cache.retrieveItem(new Object[] {"2", "0", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "2", "0"}, CACHE_ITEM_TYPE), "retreive item in flushed branch returns null");

      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"1", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");
      assertNotNull(cache.retrieveItem(new Object[] {"2", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flushing another");

      // flush whole thing
      cache.flush();
      assertNull(cache.retrieveItem(new Object[] {"0", "0", "0"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "2", "0"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"1", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "0", "1"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "0", "2"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "2", "1"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
      assertNull(cache.retrieveItem(new Object[] {"2", "2", "2"}, CACHE_ITEM_TYPE), "retrieve item after flush returns null");
   }


   /**
    * Tests using listeners for access and modified events.
    *
    * @throws Exception if any errors occur.
    */

   public void testListeners() throws Exception
   {
      PSMultiLevelCache cache;
      cache = new PSMultiLevelCache(3, -1);

      IPSCacheAccessedListener accessListener = new IPSCacheAccessedListener()
      {
         public void cacheAccessed(PSCacheEvent e)
         {
            m_accessAction = e.getAction();
            m_accessObject = e.getObject();
         }
      };
      cache.addCacheAccessedListener(accessListener);

      IPSCacheModifiedListener modifyListener = new IPSCacheModifiedListener()
      {
         public void cacheModified(PSCacheEvent e)
         {
            m_modifyActions.add(e.getAction());
            m_modifyObjects.add(e.getObject());
         }

         public void setCache(PSMultiLevelCache cache)
         {
            m_modifyCache = cache;
         }
      };
      m_modifyCache = null;
      cache.addCacheModifiedListener(modifyListener);

      assertSame(cache, m_modifyCache);

      // add an item and see if we get the event.
      m_modifyActions.clear();
      m_modifyObjects.clear();
      Object[] keys = {"a", "b", "c"};
      cache.addItem(keys, "test", 10000, CACHE_ITEM_TYPE);
      assertEquals(Integer.valueOf(PSCacheEvent.CACHE_ITEM_ADDED), m_modifyActions.get(0), "add event action");
      assertEquals("test", ((PSCacheItem)m_modifyObjects.get(0)).getObject(), "add event object");

      // replace the item, should get both a flush and an add
      m_modifyActions.clear();
      m_modifyObjects.clear();
      cache.addItem(keys, "test2", 10000, CACHE_ITEM_TYPE);
      assertEquals(Integer.valueOf(PSCacheEvent.CACHE_ITEM_REMOVED), m_modifyActions.get(0), "replace event action");
      assertNull(((PSCacheItem)m_modifyObjects.get(0)).getObject(), "replace event object");
      assertArrayEquals(keys, ((PSCacheItem)m_modifyObjects.get(0)).getKeys(), "replace event keys");
      assertEquals(Integer.valueOf(PSCacheEvent.CACHE_ITEM_ADDED), m_modifyActions.get(1), "replace event action");
      assertEquals("test2", ((PSCacheItem)m_modifyObjects.get(1)).getObject(), "replace event object");


      // flush an item, should get flush event
      m_modifyActions.clear();
      m_modifyObjects.clear();
      cache.flush(keys);
      assertEquals(Integer.valueOf(PSCacheEvent.CACHE_ITEM_REMOVED), m_modifyActions.get(0), "flush event action");
      assertNull(((PSCacheItem)m_modifyObjects.get(0)).getObject(), "flush event object");
      assertArrayEquals(keys, ((PSCacheItem)m_modifyObjects.get(0)).getKeys(), "flush event keys");

      // retrieve non-existant item, should get miss event.
      m_accessAction = -1;
      m_accessObject = "test";
      cache.retrieveItem(new Object[] {"1", "2", "3"}, CACHE_ITEM_TYPE);
      assertEquals(m_accessAction, PSCacheEvent.CACHE_ITEM_NOT_FOUND);
      assertNull(m_accessObject);   }


   /**
    * Add and retrieves the supplied object using the supplied keys, comparing
    * the result to be sure it is the same object.
    *
    * @param cache The cache to add to, assumed not <code>null</code>.
    * @param keys The keys to use, assumed to be valid keys.
    * @param item The item to add, assumed not <code>null</code>.
    * @param size The size of the item to add.
    *
    * @throws Exception if the comparison fails or anything goes wrong.
    */
   private void addAndRetrieve(PSMultiLevelCache cache, Object[] keys,
      Object item, long size) throws Exception
   {
      cache.addItem(keys, item, size, CACHE_ITEM_TYPE);
      Object result = cache.retrieveItem(keys, CACHE_ITEM_TYPE);
      assertSame(item, result, "add and retrieve did not return same object");
   }

   /**
    * Ensure that adding the specified item fails.
    *
    * @param cache The cache to add to, assumed not <code>null</code>.
    * @param keys The keys to use, assumed to be valid keys.
    * @param item The item to add, assumed not <code>null</code>.
    * @param size The size of the item to add.
    *
    * @throws Exception if the test fails or anything goes wrong.
    */
   private void addBadItem(PSMultiLevelCache cache, Object[] keys,
      Object item, long size) throws Exception
   {
      boolean didThrow = false;
      try
      {
         cache.addItem(keys, item, size, CACHE_ITEM_TYPE);
      }
      catch (Exception ex)
      {
         didThrow = true;
      }
      assertTrue(didThrow, "add bad item did not throw");
   }

   /**
    * Ensure that retrieving the specified item fails.
    *
    * @param cache The cache to add to, assumed not <code>null</code>.
    * @param keys The keys to use, assumed to be valid keys.
    *
    * @throws Exception if the test fails or anything goes wrong.
    */
   private void getBadItem(PSMultiLevelCache cache, Object[] keys)
      throws Exception
   {
      boolean didThrow = false;
      try
      {
         cache.retrieveItem(keys, CACHE_ITEM_TYPE);
      }
      catch (Exception ex)
      {
         didThrow = true;
      }
      assertTrue(didThrow, "get bad item did not throw");
   }


   // collect all tests into a TestSuite and return it - see base class


   /**
    * Stores action caused by access listener event.
    */
   private int m_accessAction = -1;

   /**
    * Stores object passed by access listener event.
    */
   private Object m_accessObject = null;

   /**
    * Stores actions caused by modify listener event.
    */
   private List<Integer> m_modifyActions = new ArrayList<>();

   /**
    * Stores objects passed by access listener event.
    */
   private List<Object> m_modifyObjects = new ArrayList<>();

   /**
    * Stores cache set by the callback after adding a modified listener.
    */
   private PSMultiLevelCache m_modifyCache = null;

   /**
    * Constant for type of item in the cache.
    */
   private static final String CACHE_ITEM_TYPE = "foo";
}
