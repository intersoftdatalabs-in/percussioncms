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
package com.percussion.services.contentmgr.impl.legacy;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.PSContentMgrOption;
import com.percussion.services.contentmgr.data.PSContentNode;
import com.percussion.services.contentmgr.impl.IPSContentRepository;
import com.percussion.services.contentmgr.impl.PSContentInternalLocator;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jsr170.IPSPropertyInterceptor;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.timing.PSStopwatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;

import org.apache.cactus.ServletTestCase;
import org.junit.experimental.categories.Category;

/**
 * Simple testers for the content repository
 *
 * @author dougrand
 */
@Category(IntegrationTest.class)
public class PSContentRepositoryTest extends ServletTestCase
{
   static final String TESTRESULT = "<Proxy result>";

   /**
    * Dummy body access class for test purposes
    */
   static public class BodyAccessTester implements IPSPropertyInterceptor
   {
      /**
       * Ctor
       */
      public BodyAccessTester() {
      }

      public Object translate(@SuppressWarnings("unused")
      Object val)
      {
         return TESTRESULT;
      }
   }

   /**
    * Test item load
    * @throws Exception
    */
   public void testLoadSimpleItem() throws Exception
   {
      var rep = PSContentInternalLocator.getLegacyRepository();
      var guids = new ArrayList<IPSGuid>();
      // Add items of several content types, including at least one with
      // children and one with a blob column for this test. This test
      // uses the default fast forward content. You will need to modify
      // one of the standard FF content types to include a simple child
      // and a multi-valued child to make this a complete test.
      //
      // Also required is to populate the child data for the test content
      // type
      guids.add(new PSLegacyGuid(390, 1)); // Image
      guids.add(new PSLegacyGuid(335, 1)); // Generic
      guids.add(new PSLegacyGuid(375, 1)); // Generic word
      guids.add(new PSLegacyGuid(503, 1)); // Brief
      guids.add(new PSLegacyGuid(501, 1)); // Press release
      long start = System.nanoTime();
      var results = rep.loadByGUID(guids, null);
      long end = System.nanoTime();
      System.out.println("Loaded elements in "
            + ((end - start) / (guids.size() * 1000)) + " micros per node");
      var image = results.get(0);
      var generic = results.get(1);
      var genword = results.get(2);
      var brief = results.get(3);
      var press = results.get(4);
      assertEquals("390", image.getUUID());
      assertEquals("335", generic.getUUID());
      assertEquals("375", genword.getUUID());
      assertEquals("503", brief.getUUID());
      assertEquals("501", press.getUUID());
      // Check simple child handling - commented out as there are no builtin
      // simple children now
      // Property category = genword.getProperty("rx:category");
      // assertNotNull(category);
      // assertTrue(category.getLengths().length > 0);
      // assertTrue(category.getValues().length > 0);
      // Spot test property values. Pick values from contentstatus and
      // each separate table. Make sure to test at least one long text and
      // one binary field in some fashion
      assertEquals(1002, image.getProperty("rx:sys_communityid").getLong());
      assertEquals("EI Global Financial Service Fund - regional mix.jpg", image
            .getProperty("rx:sys_title").getString());
      var cal = image.getProperty("rx:sys_contentstartdate").getDate();
      assertEquals(8, cal.get(Calendar.MONTH) + 1);
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(2007, cal.get(Calendar.YEAR));
      assertEquals("EI Global Health Sciences - asset mix.jpg", image
            .getProperty("rx:filename").getString());
      assertEquals(".jpg", image.getProperty("rx:img1_ext").getString());
      assertEquals(1, image.getProperty("rx:img_category").getLong());
      assertTrue(image.getProperty("rx:img1").getStream().available() > 0);
      // Generic - just a couple of fields
      assertTrue(generic.getProperty("rx:body").getString().length() > 0);
      assertTrue(generic.getProperty("rx:callout").getString().length() > 0);
      assertTrue(generic.getProperty("rx:callout").getString().toLowerCase()
            .startsWith("<div"));
      assertEquals(5, generic.getProperty("rx:sys_contentstateid").getLong());
   }

   /**
    * This test requires adding a simple child and multivalued child onto some
    * content type, creating an item of that type with children, and then
    * loading it. Since this is very specific, this test will normally be
    * commented out. (or expect a failure)
    *
    * To test, rename the test without "skip" and create multiple authors
    * children of Brief 503, with a "First" and "Last" local field and make it
    * sequenced. Check the order against the results of the node iterator.
    *
    * @throws Exception
    */

   public void skipTestLoadChildren() throws Exception
   {
      var rep = PSContentInternalLocator.getLegacyRepository();
      var guids = new ArrayList<IPSGuid>();
      guids.add(new PSLegacyGuid(503, 4)); // Brief with children
      var results = rep.loadByGUID(guids, null);
      assertEquals(1, results.size());
      var brief = results.get(0); // Check children
      var niter = brief.getNodes("authors");
      assertTrue(niter.getSize() > 0);
      var author = niter.nextNode();
      var first = author.getProperty("rx:First");
      var last = author.getProperty("rx:Last");
      var rank = author.getProperty("rx:sys_sortrank");
      assertNotNull(first);
      assertNotNull(last);
      assertNotNull(first.getString());
      assertNotNull(last.getString());
      assertNotNull(rank.getString());
      var cfg = new PSContentMgrConfig();
      cfg.addOption(PSContentMgrOption.LOAD_MINIMAL);
      results = rep.loadByGUID(guids, cfg);
      brief = results.get(0);
      niter = brief.getNodes("authors");
      assertTrue(niter.getSize() > 0);
      long lastrank = -1;
      while (niter.hasNext())
      {
         author = niter.nextNode();
         long r = author.getProperty("rx:sys_sortrank").getLong();
         assertTrue(r > lastrank);
         lastrank = r;
      }
   }

   /**
    * The body accessor allows filtering of body content in assembly or other
    * users of the service. These interceptors can do things like processing
    * inline links or cleaning up the namespaces on the body "document".
    * @throws Exception
    */
   public void testBodyAccessor() throws Exception
   {
      var rep = PSContentInternalLocator.getLegacyRepository();
      var guids = new ArrayList<IPSGuid>();
      guids.add(new PSLegacyGuid(335, 1)); // Generic
      var cfg = new PSContentMgrConfig();
      cfg.setBodyAccess(new BodyAccessTester());
      var results = rep.loadByGUID(guids, cfg);
      var generic = results.get(0);
      var body = generic.getProperty("rx:body").getString();
      assertEquals(TESTRESULT, body);
   }

   /**
    * Test loading by path
    * @throws Exception
    */
   public void testLoadByPath() throws Exception
   {
      var mgr = PSContentMgrLocator.getContentMgr();
      var paths = new ArrayList<String>();
      paths.add("//Sites/EnterpriseInvestments/Files/EI Sample PDF.pdf");
      paths
         .add("//Sites/EnterpriseInvestments/Briefs/Rates are down, have you refinanced?");
      var results = mgr.findItemsByPath(null, paths, null);
      assertTrue(results.size() > 0);

      // Get time for loading specific item both ways
      long start, end;
      paths.clear();
      paths.add("//Sites/EnterpriseInvestments/Files/EI Sample PDF.pdf");
      start = System.nanoTime();
      results = mgr.findItemsByPath(null, paths, null);
      end = System.nanoTime();
      var n1 = results.iterator().next();
      System.out.println("Loaded item " + n1.getUUID() + " by path in "
            + ((end - start) / 1000) + " microseconds");

      var guids = new ArrayList<IPSGuid>();
      guids.add(((PSContentNode) n1).getGuid());
      start = System.nanoTime();
      results = mgr.findItemsByGUID(guids, null);
      end = System.nanoTime();
      System.out.println("Loaded item " + n1.getUUID() + " by guid in "
            + ((end - start) / 1000) + " microseconds");
   }

   /**
    * Eponymously named
    * @throws Exception
    */
   public void testLoadRevisionsByPath() throws Exception
   {
      var mgr = PSContentMgrLocator.getContentMgr();
      var paths = new ArrayList<String>();
      long start = System.nanoTime();
      paths
            .add("//Sites/EnterpriseInvestments/AboutEnterpriseInvestments/Page - About Enterprise Investments#1");
      paths
            .add("//Sites/EnterpriseInvestments/AboutEnterpriseInvestments/Page - About Enterprise Investments#2");
      paths
            .add("//Sites/EnterpriseInvestments/AboutEnterpriseInvestments/Page - About Enterprise Investments#3");
      var results = mgr.findItemsByPath(null, paths, null);
      var n1 = results.iterator().next();
      long end = System.nanoTime();
      System.out.println("Loaded 3 items " + n1.getUUID() + " by guid in "
            + ((end - start) / 3000) + " microseconds per item");
      for (var n : results)
      {
         System.out.println("Item guid: " + n.getUUID());
      }
      assertEquals(3, results.size());
   }

   /**
    * Eponymously named
    * @throws Exception
    */
   public void testItemNotFoundException() throws Exception
   {
      var mgr = PSContentMgrLocator.getContentMgr();
      var paths = new ArrayList<String>();
      paths.add("//Sites/EnterpriseInvestments/item that does not exist");
      try
      {
         mgr.findItemsByPath(null, paths, null);
         assertTrue("No exception where one expected", false);
      }
      catch (ItemNotFoundException e)
      {
         // Correct behavior
      }
      catch (Exception e)
      {
         assertTrue("Wrong exception thrown", false);
      }

      var ids = new ArrayList<IPSGuid>();
      ids.add(new PSLegacyGuid(1000000000, 100000000));
      var nodes = mgr.findItemsByGUID(ids, null);
      assertTrue("No exception where one expected", nodes.isEmpty());
   }

   /**
    * Eponymously named
    * @throws Exception
    */
   public void testLoadTimes() throws Exception
   {
      var cms = PSCmsObjectMgrLocator.getObjectManager();
      var generics = cms
            .findComponentSummariesByType(311);
      var briefs = cms
            .findComponentSummariesByType(302);
      var guids = new ArrayList<IPSGuid>();

      for (var s : generics)
      {
         guids.add(new PSLegacyGuid(s.getContentId(), s.getCurrentLocator()
               .getRevision()));
      }
      for (var s : briefs)
      {
         guids.add(new PSLegacyGuid(s.getContentId(), s.getCurrentLocator()
               .getRevision()));
      }
      var mgr = PSContentMgrLocator.getContentMgr();

      var sw = new PSStopwatch();
      sw.start();
      var cfg = new PSContentMgrConfig();
      cfg.addOption(PSContentMgrOption.LAZY_LOAD_CHILDREN);
      var nodes = mgr.findItemsByGUID(guids, cfg);
      sw.stop();
      System.out.println("Loading " + guids.size() + " items. " + nodes.size()
            + " items were actually loaded in " + sw);
   }
}
