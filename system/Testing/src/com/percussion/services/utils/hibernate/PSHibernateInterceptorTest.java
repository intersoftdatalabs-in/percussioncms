/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.utils.hibernate;

import com.percussion.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.memory.PSCacheAccessLocator;
import com.percussion.utils.exceptions.PSORMException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.security.SecureRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * Test that the event listener is evicting things from the object cache for all
 * applicable hibernate events. Use a cheap, disposable object for this purpose.
 * 
 * @author dougrand
 */
@Category(IntegrationTest.class)
public class PSHibernateInterceptorTest
{

   private static final Logger log = LogManager.getLogger(PSHibernateInterceptorTest.class);

   /**
    * How long to wait before continuing to allow notification to complete
    */
   private static final long NOTIFICATION_DELAY = 100L;

   /**
    * Filter service
    */
   private static IPSFilterService ms_fmgr = PSFilterServiceLocator
         .getFilterService();

   /**
    * Generate some random numbers for the test
    */
   private static SecureRandom ms_rand = new SecureRandom();

   /**
    * Filter name for mutable filter instance
    */
   private static final String FNAME = "__testfilter";

   /**
    * Filter guid
    */
   private static IPSGuid ms_fid = null;

   /**
    * Remove any existing test filter and create a new one.
    */
   @BeforeClass
   public void setupInitialData()
   {
      // Remove any existing mutable filter instances from db
      cleanupData();

      // Create the initial filter and setup data for it
      IPSItemFilter filter = ms_fmgr.createFilter(FNAME,
            "The initial description");
      filter.setLegacyAuthtypeId(ms_rand.nextInt());
      ms_fid = filter.getGUID();
      try
      {
         ms_fmgr.saveFilter(filter);
         System.out.println("Saved filter: " + filter.getGUID());
      }
      catch (PSFilterException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
   }

   /**
    * Test that the initial conditions hold, and then do a modification and see
    * that the update works properly. This tests the notification mechanism both
    * from the hibernate event as well as the specific listeners in the filter
    * service.
    * 
    * @throws PSFilterException
    * @throws InterruptedException
    */
   @Test
   public void testInitialConditions() throws PSFilterException,
           InterruptedException, PSNotFoundException {
      // Get the filter and the unmodifiable filter. Compare for equality
      IPSItemFilter db_filter = ms_fmgr.loadFilter(ms_fid);
      IPSItemFilter ro_filter = ms_fmgr.loadUnmodifiableFilter(ms_fid);
      IPSItemFilter ro_byname_filter = ms_fmgr.findFilterByName(FNAME);
      assertEquals(db_filter, ro_filter);
      assertEquals(db_filter, ro_byname_filter);

      // Modify the db copy and verify not equal
      db_filter.setLegacyAuthtypeId(db_filter.getLegacyAuthtypeId() + 1);
      assertNotSame(db_filter, ro_filter);
      
      // Save the db filter
      ms_fmgr.saveFilter(db_filter);
      Thread.sleep(NOTIFICATION_DELAY);
      
      // Check not equals still
      assertNotSame(db_filter, ro_filter);
   }

   /**
    * Check that the eviction code used by the update handlers also causes any
    * in-memory objects to be flushed.
    * 
    * @throws PSFilterException
    * @throws InterruptedException
    * @throws PSORMException
    */
   @Test
   public void testCacheEviction() throws PSFilterException,
           InterruptedException, PSORMException, PSNotFoundException {
      // Get the filter and the unmodifiable filter. Compare for equality
      IPSItemFilter db_filter = ms_fmgr.loadFilter(ms_fid);
      IPSItemFilter ro_filter = ms_fmgr.loadUnmodifiableFilter(ms_fid);
      IPSItemFilter ro_byname_filter = ms_fmgr.findFilterByName(FNAME);
      System.out.println("Db val: " + db_filter.getLegacyAuthtypeId());
      System.out.println("Ro val: " + ro_filter.getLegacyAuthtypeId());
      assertEquals(db_filter, ro_filter);
      assertEquals(db_filter, ro_byname_filter);

      // Evict from the second level cache - should force eviction from
      // the in-memory cache
      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
      cms.handleDataEviction(PSItemFilter.class, ms_fid);
      Thread.sleep(NOTIFICATION_DELAY);

      IPSCacheAccess cache = PSCacheAccessLocator.getCacheAccess();
      assertNull(cache.get(ms_fid, IPSCacheAccess.IN_MEMORY_STORE));

      // Check reload
      ro_filter = ms_fmgr.loadUnmodifiableFilter(ms_fid);
      assertNotNull(cache.get(ms_fid, IPSCacheAccess.IN_MEMORY_STORE));
   }

   /**
    * Try the cache clearance code
    * 
    * @throws PSFilterException
    */
   @Test
   public void testCacheClear() throws PSFilterException, PSNotFoundException {
      IPSItemFilter db_filter = ms_fmgr.loadUnmodifiableFilter(ms_fid);
      assertNotNull(db_filter);

      IPSCacheAccess cache = PSCacheAccessLocator.getCacheAccess();
      cache.clear();
      assertNull(cache.get(ms_fid, IPSCacheAccess.IN_MEMORY_STORE));
   }

   /**
    * Check that deletion causes the object to be removed.
    * 
    * @throws PSFilterException
    * @throws InterruptedException
    * @throws PSORMException
    */
   @Test
   public void testDeletion() throws PSFilterException, InterruptedException,
           PSORMException, PSNotFoundException {
      IPSItemFilter db_filter = ms_fmgr.loadFilter(ms_fid);
      ms_fmgr.deleteFilter(db_filter);
      System.out.println("Removed filter: " + db_filter.getGUID());
      Thread.sleep(NOTIFICATION_DELAY);
      
      IPSCacheAccess cache = PSCacheAccessLocator.getCacheAccess();
      assertNull(cache.get(ms_fid, IPSCacheAccess.IN_MEMORY_STORE));
   }

   /**
    * Cleanup object created in testing - should be a noop
    */
   @AfterClass
   public void cleanupData()
   {
      // Remove any existing mutable filter instances from db
      try
      {
         IPSItemFilter filter = ms_fmgr.findFilterByName(FNAME);
         ms_fmgr.deleteFilter(filter);
         System.out.println("Removed filter: " + filter.getGUID());
      }
      catch (PSFilterException e)
      {
         // This is ok
      }
   }


}
