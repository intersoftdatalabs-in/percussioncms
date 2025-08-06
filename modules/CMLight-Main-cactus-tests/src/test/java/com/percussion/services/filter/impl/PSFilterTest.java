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
package com.percussion.services.filter.impl;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.server.PSRequest;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.filter.IPSFilterItem;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.filter.data.PSFilterItem;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.timing.PSStopwatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cactus.ServletTestCase;
import org.junit.experimental.categories.Category;

/**
 * Test item filters
 * 
 * @author dougrand
 */
@Category(IntegrationTest.class)
public class PSFilterTest extends ServletTestCase
{
   /**
    * 
    */
   private static final String TEST_FILTER_NAME = "junitTestFilter";

   
   /**
    * Not really a test, just cleanup anything left over from an earlier
    * test to make sure we're clean.
    * 
    * @throws Exception
    */
   public void testCleanup() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();

      try
      {
         while(true)
         {
            var filter = fsvc.findFilterByName(TEST_FILTER_NAME);
            fsvc.deleteFilter(filter);
         }
      }
      catch(Exception e)
      {
         // Ignore
      }
   }

   /**
    * This test creates and mutates an item filter instance. It makes sure to
    * cover the following cases:
    * <ul>
    * <li>Adding a rule
    * <li>Removing a rule
    * <li>Modifying a rule
    * </ul>
    * 
    * @throws Exception
    */
   public void testFolderSavesAndMerges() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var filter = fsvc.createFilter(TEST_FILTER_NAME,
            "Item filter just to test");
      var params = new HashMap<String, String>();
      params.put("a", "1");
      var def = fsvc.createRuleDef("mytestrule1", params);
      filter.addRuleDef(def);
      fsvc.saveFilter(filter);

      Thread.sleep(200);

      filter = fsvc.findFilterByName(TEST_FILTER_NAME);
      params = new HashMap<>();
      params.put("b", "2");
      params.put("c", "3");
      def = fsvc.createRuleDef("mytestrule2", params);
      filter.addRuleDef(def);
      fsvc.saveFilter(filter);

      filter = fsvc.findFilterByName(TEST_FILTER_NAME);
      var defs = new HashMap<String, IPSItemFilterRuleDef>();
      for (var frdef : filter.getRuleDefs())
      {
         defs.put(frdef.getRuleName(), frdef);
      }
      assertNotNull(defs.get("mytestrule1"));
      assertNotNull(defs.get("mytestrule2"));
      var deftoTest = defs.get("mytestrule1");
      assertEquals("1", deftoTest.getParam("a"));
      deftoTest = defs.get("mytestrule2");
      assertEquals("2", deftoTest.getParam("b"));
      assertEquals("3", deftoTest.getParam("c"));
      filter.removeRuleDef(deftoTest);
      fsvc.saveFilter(filter);

      filter = fsvc.findFilterByName(TEST_FILTER_NAME);
      defs = new HashMap<>();
      for (var frdef : filter.getRuleDefs())
      {
         defs.put(frdef.getRuleName(), frdef);
      }
      assertNull(defs.get("mytestrule2"));
      deftoTest = defs.get("mytestrule1");
      deftoTest.setParam("a", "5");
      fsvc.saveFilter(filter);

      filter = fsvc.findFilterByName(TEST_FILTER_NAME);
      defs = new HashMap<>();
      for (var frdef : filter.getRuleDefs())
      {
         defs.put(frdef.getRuleName(), frdef);
      }
      deftoTest = defs.get("mytestrule1");
      assertEquals("5", deftoTest.getParam("a"));
      deftoTest.setParam("d", "6");
      deftoTest.removeParam("a");
      fsvc.saveFilter(filter);
   }

   /**
    * Test preview filter
    * 
    * @throws Exception
    */
   public void testPreviewFilter() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var w = new PSStopwatch();
      var items = new ArrayList<IPSFilterItem>();
      items.addAll(getFolderItems("//Sites/EnterpriseInvestments", 1));
      items.addAll(getFolderItems(
            "//Sites/EnterpriseInvestments/ProductsAndServices", 1));
      items.addAll(getFolderItems(
            "//Sites/EnterpriseInvestments/ProductsAndServices/Funds", 1));

      var filter = fsvc.findFilterByName("preview");

      var params = new HashMap<String, String>();
      params.put(IPSHtmlParameters.SYS_USER, "doug");
      w.start();
      var results = filter.filter(items, params);
      w.stop();
      assertNotNull(results);
      assertTrue(results.size() > 0);
      System.out.println("Preview filtering " + results.size() + " took " + w);

      var currentmap = new HashMap<Integer, Integer>();
      for (var item : results)
      {
         var lg = (PSLegacyGuid) item.getItemId();
         currentmap.put(lg.getContentId(), lg.getRevision());
      }
      items.clear();
      items.addAll(getFolderItems("//Sites/EnterpriseInvestments", 0));
      items.addAll(getFolderItems(
            "//Sites/EnterpriseInvestments/ProductsAndServices", 0));
      items.addAll(getFolderItems(
            "//Sites/EnterpriseInvestments/ProductsAndServices/Funds", 0));
      var revisionmap = new HashMap<Integer, Integer>();
      for (var item : results)
      {
         var lg = (PSLegacyGuid) item.getItemId();
         revisionmap.put(lg.getContentId(), lg.getRevision());
      }
      assertEquals(currentmap, revisionmap);
   }

   /**
    * Test folder filter
    * 
    * @throws Exception
    */
   public void testFolderFilter1() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var filter = fsvc.findFilterByName("sitefolder");
      var items = new ArrayList<IPSFilterItem>();
      items.addAll(getFolderItems("//Sites/EnterpriseInvestments", 0));
      items.addAll(getFolderItems("//Sites/CorporateInvestments", 0));

      var params = new HashMap<String, String>();
      params.put(IPSHtmlParameters.SYS_SITEID, "301");
      var results = filter.filter(items, params);
      assertNotNull(results);
      assertTrue(results.size() > 0);
   }

   /**
    * Test folder filter and print out performance information, the prior test
    * will prime the pump with data.
    * 
    * @throws Exception
    */
   public void testFolderFilter2() throws Exception
   {
      var w = new PSStopwatch();
      var fsvc = PSFilterServiceLocator.getFilterService();
      var filter = fsvc.findFilterByName("sitefolder");
      var items = new ArrayList<IPSFilterItem>();
      items.addAll(getFolderItems(
            "//Sites/EnterpriseInvestments/ProductsAndServices", 0));
      items.addAll(getFolderItems("//Sites/", 0));

      var params = new HashMap<String, String>();
      params.put(IPSHtmlParameters.SYS_SITEID, "301");
      w.start();
      var results = filter.filter(items, params);
      w.stop();
      assertNotNull(results);
      assertTrue(results.size() > 0);
      System.out.println("Folder filtering " + results.size() + " took " + w);
   }

   /**
    * Test publishable filter
    * 
    * @throws Exception
    */
   public void testPublishableFilter() throws Exception
   {
      var w = new PSStopwatch();
      var fsvc = PSFilterServiceLocator.getFilterService();
      var filter = fsvc.findFilterByName("public");

      var items = new ArrayList<IPSFilterItem>();
      items.addAll(getFolderItems(
            "//Sites/EnterpriseInvestments/ProductsAndServices", 0));

      w.start();
      var results = filter.filter(items, null);
      w.stop();
      assertNotNull(results);
      assertTrue(results.size() > 0);
   }

   public void testSetLegacyAuthtypeId() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var filter = fsvc.findFilterByName("public");

      var xmlString = ((PSItemFilter) filter).toXML();
      var pubFilter = new PSItemFilter();
      pubFilter.fromXML(xmlString);
      assertTrue(pubFilter.getLegacyAuthtypeId() != null);

      pubFilter.setLegacyAuthtypeId(null);
      xmlString = pubFilter.toXML();
      var pubFilter_2 = new PSItemFilter();
      pubFilter_2.fromXML(xmlString);
      assertTrue(pubFilter_2.getLegacyAuthtypeId() == null);
   }

   /**
    * Test Find methods
    * @throws Exception
    */
   public void testFindFilter() throws Exception
   {
      var filterName = "junitTestFilterForFinder";
      var fsvc = PSFilterServiceLocator.getFilterService();
      var filter = fsvc.createFilter(filterName,
            "Item filter just to test");
      var params = new HashMap<String, String>();
      params.put("a", "1");
      var def = fsvc.createRuleDef("mytestrule1", params);
      filter.addRuleDef(def);
      fsvc.saveFilter(filter);

      Thread.sleep(200);

      filter = fsvc.findFilterByName(filterName);
      assertNotNull(filter);
      filter = fsvc.findFilterByID(filter.getGUID());
      assertNotNull(filter);
      fsvc.deleteFilter(filter);
   }

   /**
    * Get the folder items for the test
    * 
    * @param folderPath the path for the items
    * @param rev if more than zero, use this revision instead of the current
    *           revision
    * @return the filter items
    * @throws PSCmsException
    */
   private List<IPSFilterItem> getFolderItems(String folderPath, int rev)
         throws PSCmsException
   {
      var req = PSRequest.getContextForRequest();
      var proc = PSServerFolderProcessor.getInstance();
      var folder = proc.getSummary(folderPath);
      if (folder == null)
      {
         throw new RuntimeException("Couldn't find folder " + folderPath);
      }
      var folderLocator = folder.getCurrentLocator();
      var children = proc.getChildSummaries(folderLocator);
      var rval = new ArrayList<IPSFilterItem>();
      for (var child : children)
      {
         var contentid = child.getContentId();
         var revision = rev == 0
               ? child.getCurrentLocator().getRevision()
               : rev;
         var item = new PSFilterItem(new PSLegacyGuid(contentid,
               revision), new PSLegacyGuid(folderLocator), null);
         rval.add(item);
      }
      return rval;
   }
   
   
   
}
