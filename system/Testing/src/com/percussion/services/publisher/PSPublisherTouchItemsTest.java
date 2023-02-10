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
package com.percussion.services.publisher;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.timing.PSStopwatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The tests in this class assume the content and relationships in the fast
 * forward sample content. The exact ids may need to be updated over time.
 * 
 * @author dougrand
 */
@Category(IntegrationTest.class)
public class PSPublisherTouchItemsTest
{
    private static final Logger ms_log = LogManager.getLogger(PSPublisherTouchItemsTest.class);

   static IPSPublisherService ps = PSPublisherServiceLocator
         .getPublisherService();

   /**
    * @throws Exception
    */
   @Test
   public void testTouchItems() throws Exception
   {
      Date d = new Date();

      Collection<Integer> items = new ArrayList<Integer>();

      items.add(319);
      PSStopwatch sw = new PSStopwatch();
      sw.start();
      ps.touchContentItems(items);
      sw.stop();
      System.out.println("Touch parents took " + sw + " for one item");

      checkAtLeastAsNew(319, d);
   }

   private void checkAtLeastAsNew(int i, Date d)
   {
      long millis = d.getTime();

      // Provide a little fuzz (one second) to prevent meaningless failures from
      // the database rounding down the time.
      millis -= 1000;

      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
      PSComponentSummary s = cms.loadComponentSummary(i);
      Date lm = s.getContentLastModifiedDate();

      long modmillis = lm.getTime();

      assertTrue(modmillis >= millis);
      assertNotNull(s.getContentLastModifier());
   }

   /**
    * @throws Exception
    */
   @Test
   public void testTouchParentItems() throws Exception
   {
      Date d = new Date();

      Collection<Integer> items = new ArrayList<Integer>();

      items.add(337);
      PSStopwatch sw = new PSStopwatch();
      sw.start();
      ps.touchContentItems(items);
      ps.touchActiveAssemblyParents(items);
      sw.stop();
      ms_log.info("Touch parents took " + sw);
      checkAtLeastAsNew(337, d);
      checkAtLeastAsNew(397, d);
   }

   /**
    * @throws Exception
    */
   @Test
   public void testTouchContentTypeItems() throws Exception
   {
      Date d = new Date();

      Collection<IPSGuid> ctypes = new ArrayList<IPSGuid>();

      ctypes.add(new PSGuid(PSTypeEnum.NODEDEF, 301));
      PSStopwatch sw = new PSStopwatch();
      sw.start();
      ps.touchContentTypeItems(ctypes);
      sw.stop();
      ms_log.info("Touch content type items took " + sw);

      checkAtLeastAsNew(496, d);
      checkAtLeastAsNew(596, d);
   }

   /**
    * To find candidates for this test, run this. Note that this relies on slot
    * ids < 300 being inline. Change if needed. In the current FF implementation,
    * this doesn't return anything out of the box.
    * 
    * SELECT R.RID, C.RID, R.CONFIG_ID, R.SLOT_ID, R.OWNER_ID, R.OWNER_REVISION, CS.EDITREVISION, R.DEPENDENT_ID, R.DEPENDENT_REVISION, C.DEPENDENT_ID GRANDCHILD
    * FROM PSX_OBJECTRELATIONSHIP R INNER JOIN PSX_OBJECTRELATIONSHIP C
    * ON C.OWNER_ID = R.DEPENDENT_ID INNER JOIN CONTENTSTATUS CS
    * ON CS.CONTENTID = C.OWNER_ID
    * WHERE (R.CONFIG_ID IN (1,2)) AND
    * cs.CONTENTID = R.OWNER_ID AND
    * ((cs.EDITREVISION > 0 and r.OWNER_REVISION = cs.EDITREVISION)
    * or (R.OWNER_REVISION = CS.CURRENTREVISION)) AND
    * (R.SLOT_ID < 300) AND (R.DEPENDENT_ID IN (SELECT P.OWNER_ID FROM
    * PSX_OBJECTRELATIONSHIP P)) ORDER BY R.RID, R.DEPENDENT_ID
    * 
    * @throws Exception
    */
   @Test
   public void testInlineLinkGrandparentCase() throws Exception
   {
      Date d = new Date();

      Collection<Integer> items = new ArrayList<Integer>();
      items.add(351);
      PSStopwatch sw = new PSStopwatch();
      sw.start();
      ps.touchContentItems(items);
      ps.touchActiveAssemblyParents(items);
      sw.stop();
      ms_log.info("Touch grandparent case 1 items took " + sw);

      // checkAtLeastAsNew(466, d); // Fill in with a candidate when possible
      checkAtLeastAsNew(335, d); // Parent
   }

   /**
    * To find candidates for this test, run this query.
    * 
    * SELECT * FROM PSX_OBJECTRELATIONSHIP WHERE (CONFIG_ID IN (1,2)) AND
    * (VARIANT_ID IN (SELECT DISTINCT VARIANTID FROM RXVARIANTSLOTTYPE)) AND
    * (DEPENDENT_ID IN (SELECT OWNER_ID FROM PSX_OBJECTRELATIONSHIP AS
    * PSX_OBJECTRELATIONSHIP_1)) ORDER BY DEPENDENT_ID
    * 
    * @throws Exception
    */
   @Test
   public void testVariantSlotGrandparentCase() throws Exception
   {
      Date d = new Date();

      Collection<Integer> items = new ArrayList<Integer>();
      items.add(350);
      PSStopwatch sw = new PSStopwatch();
      sw.start();
      ps.touchContentItems(items);
      ps.touchActiveAssemblyParents(items);
      sw.stop();
      ms_log.info("Touch grandparent case 2 items took " + sw);

      checkAtLeastAsNew(324, d); // GP
      checkAtLeastAsNew(326, d); // Parent
   }

   /**
    * Just touch a lot of items to see what the performance is like
    * 
    * @throws Exception
    */
   @Test
   public void testPerformance() throws Exception
   {
      Date d = new Date();

      Collection<Integer> items = new ArrayList<Integer>();
      IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();

      items.addAll(cms.findContentIdsByType(314));
      items.addAll(cms.findContentIdsByType(313));
      items.addAll(cms.findContentIdsByType(311));
      items.addAll(cms.findContentIdsByType(310));
      items.addAll(cms.findContentIdsByType(307));
      items.addAll(cms.findContentIdsByType(304));
      PSStopwatch sw = new PSStopwatch();
      sw.start();
      ps.touchContentItems(items);
      sw.stop();
      ms_log.info("Touch multiple items took " + sw + " for " + items.size()
            + " items");

      checkAtLeastAsNew(324, d); // GP
      checkAtLeastAsNew(326, d); // Parent
   }
}
