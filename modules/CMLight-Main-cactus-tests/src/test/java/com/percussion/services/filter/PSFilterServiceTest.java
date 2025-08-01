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
package com.percussion.services.filter;

import com.percussion.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.filter.data.PSFilterItem;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.filter.data.PSItemFilterRuleDef;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.publisher.IPSPubItemStatus;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.publisher.data.PSContentListItem;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.xml.PSInvalidXmlException;
import org.apache.cactus.ServletTestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.experimental.categories.Category;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author dougrand
 * 
 */
@Category(IntegrationTest.class)
public class PSFilterServiceTest extends ServletTestCase
{
   private static final Logger log = LogManager.getLogger(PSFilterServiceTest.class);

   /**
    * Fixed authtype
    */
   private static final int AUTH = 10111;


   /**
    * Cleanup
    * 
    * @throws Exception
    */
   public void testCleanupFilters() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();

      var filters = fsvc.findAllFilters();
      for (var f : filters)
      {
         if (f.getName().equals("f1") || f.getName().equals("vFilter"))
            fsvc.deleteFilter(f);
      }
   }
   
   /**
    * Test executing Content Lists.
    * @throws Exception if any error occurs.
    */
   public void testExecuteContentList() throws Exception
   {
      var clistId = new PSGuid(PSTypeEnum.CONTENT_LIST, 310);
      var siteId = new PSGuid(PSTypeEnum.SITE, 301); // EI site
      var deliveryContextId = new PSGuid(PSTypeEnum.CONTEXT, 1); // publish
      var ps = PSPublisherServiceLocator.getPublisherService();
      var cList = ps.loadContentList(clistId);
      var clistItems = ps.executeContentList(cList, null,
            true, deliveryContextId, siteId);

      assertTrue(clistItems != null);
      assertTrue(clistItems.size() > 0);

      clistId = new PSGuid(PSTypeEnum.CONTENT_LIST, 311);
      cList = ps.loadContentList(clistId);
      clistItems = ps.executeContentList(cList, null, true, deliveryContextId,
            siteId);
      assertTrue(clistItems != null);
      assertTrue(clistItems.size() > 0);
   }

   public void testFindLastPublishedItem() throws Exception
   {
      var ps = PSPublisherServiceLocator.getPublisherService();
      var lgId = new PSLegacyGuid(1, 1);
      var itemStatus = ps.findLastPublishedItemStatus(lgId);
      assertTrue(itemStatus == null);
   }
   
   /**
    * Util method for create, save filter, then load serialize and load and save
    * See {@link #testLoadAndDeserializeAndSaveFilter()} for more info.
    * 
    * @param fsvc the filter service never <code>null</code>
    * @return the actual saved filter
    * @throws Exception
    */
   private IPSItemFilter createAndSaveFilter(IPSFilterService fsvc)
         throws Exception
   {
      var ifilter = fsvc.createFilter("vFilter", "vfilter Desc");
      ifilter.setLegacyAuthtypeId(AUTH);
      var params = new HashMap<String, String>();
      params.put("sys_folderPaths_v", "//Sites/EnterpriseInvestments/v%");
      var rule = fsvc.createRuleDef(
            "sys_filterByFolderPaths_v", params);
      ifilter.addRuleDef(rule);
      fsvc.saveFilter(ifilter);
      return ifilter;
   }


   /**
    * Helper method to load and save. Can be loaded in two ways:
    * 1. provide filter name only OR
    * 2. provide the filter as a string
    * @param fsvc the filter service
    * @param name the name of the filter may be <code>null</code>
    * @param filterStr the serialized data for filter may be <code>null</code>
    * @param doSave to save or not to save a boolean
    * @throws PSFilterException
    * @throws IOException
    * @throws SAXException
    * @throws PSInvalidXmlException
    */
   private void loadAndSaveFilter(IPSFilterService fsvc, String name,
         String filterStr, boolean doSave) throws PSFilterException,
         IOException, SAXException, PSInvalidXmlException
   {
      IPSItemFilter ifilter = null;
      if (org.apache.commons.lang.StringUtils.isNotBlank(name))
         ifilter = fsvc.findFilterByName(name);
      else if (org.apache.commons.lang.StringUtils.isNotBlank(filterStr))
      {
         ifilter = new PSItemFilter();
         ifilter.fromXML(filterStr);
      }
      Integer ver = ((PSItemFilter) ifilter).getVersion();

      var rules = ifilter.getRuleDefs();
      for (var def : rules)
         ((PSItemFilterRuleDef) def).setVersion(null);

      ((PSItemFilter) ifilter).setVersion(null);
      ((PSItemFilter) ifilter).setVersion(ver);

      if (doSave)
      {
         try
         {
            fsvc.saveFilter(ifilter);
         }
         catch (Exception e)
         {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            assert (false);
         }
      }
      assert (true);
   }



   /**
    * Testing of msm functionality for: creating a filter archive
    *    1. create, save a filter 
    *    2. load that filter and serialize 
    * @throws Exception if there's a problem 
    */
   public void testSerializeFilter() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var ifilter = createAndSaveFilter(fsvc);
      loadAndSaveFilter(fsvc, ifilter.getName(), null, false);
      ifilter.toXML();
   }
   /**
    * Testing of msm functionality for: deployment of a filter archive: 
    *    1. Load an existing filter 
    *    2. add  a new rule 
    *    3. save the filter with the new rule
    * 
    * @throws Exception
    */
   public void testLoadAndDeserializeAndSaveFilter() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();

      var filter = createAndSaveFilter(fsvc);

      var foo1 =
         "<item-filter id=\"1\">" +
            "<guid>" + filter.getGUID().toString() + "</guid>" +
            "<description>vfilter Desc</description>" +
            "<label>vFilter</label>" +
            "<legacy-authtype-id>10111</legacy-authtype-id>" +
            "<name>vFilter</name>" +
            "<parent-filter-id/>" +
            "<rule-defs>"+
               "<rule-def id=\"2\">" +
                  "<filter idref=\"1\"/>" +
                  "<params>"+
                     "<entry id=\"3\">" +
                        "<key>sys_folderPaths_v</key>" +
                        "<value>//Sites/EnterpriseInvestments/v%</value>" +
                     "</entry>" +
                  "</params>" +
                  "<rule-name>sys_filterByFolderPaths_v</rule-name>" +
                "</rule-def>" +
                "<rule-def id=\"4\">" + 
                   "<filter idref=\"1\"/>" +
                   "<params>" +
                      "<entry id=\"5\">" + 
                      "<key>new_rule_arg1</key>" +
                      "<value>new_rule_arg1_value</value>" +
                      "</entry>" +
                   "</params>" +
                   "<rule-name>newRule</rule-name>" +
                "</rule-def>" +
             "</rule-defs>" +
          "</item-filter>";

      loadAndSaveFilter(fsvc, null, foo1, true);
   }

   /**
    * @throws Exception
    */
   public void testCreateFilter() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var ifilter = fsvc.createFilter("f1", "filter 1");
      ifilter.setLegacyAuthtypeId(AUTH);
      var params = new HashMap<String, String>();
      params.put("sys_folderPaths", "//Sites/EnterpriseInvestments/%");
      var rule = fsvc
            .createRuleDef(
                  "Java/global/percussion/itemfilter/sys_filterByFolderPaths",
                  params);
      ifilter.addRuleDef(rule);
      params = new HashMap<String, String>();
      params.put("sys_flagValues", "y");
      rule = fsvc.createRuleDef(
            "Java/global/percussion/itemfilter/sys_filterByPublishableFlag",
            params);
      ifilter.addRuleDef(rule);
      fsvc.saveFilter(ifilter);
   }

   /**
    * @throws Exception
    */
   public void testFinders() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      assertNotNull(fsvc.findFilterByAuthType(AUTH));
      assertNotNull(fsvc.findFilterByName("f1"));

      try
      {
         fsvc.findFilterByAuthType(111111111);
         assertTrue(false);
      }
      catch (PSFilterException f)
      {
         // Correct
      }

      try
      {
         fsvc.findFilterByName("non existant filter****");
         assertTrue(false);
      }
      catch (PSFilterException f)
      {
         // Correct
      }
   }

   /**
    * @throws Exception
    */
   public void testActiveRule() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var ifilter = fsvc.findFilterByName("f1");
      var itemsToFilter = new ArrayList<IPSFilterItem>();
      itemsToFilter.add(new PSFilterItem(new PSLegacyGuid(466, 1),
            new PSLegacyGuid(301, 1), null));
      itemsToFilter.add(new PSFilterItem(new PSLegacyGuid(504, 1),
            new PSLegacyGuid(302, 1), null));
      itemsToFilter.add(new PSFilterItem(new PSLegacyGuid(442, 1),
            new PSLegacyGuid(441, 1), null));
      var filteredIds = ifilter.filter(itemsToFilter, null);
      assertNotNull(filteredIds);
   }

   /**
    * @throws Exception
    */
   public void testMutateFilterData() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var ifilter = fsvc.findFilterByName("f1");
      ifilter = fsvc.loadFilter(ifilter.getGUID());
      var defs = ifilter.getRuleDefs();
      assertNotNull(defs);
      assertNotNull(defs.iterator());
      var thedef = defs.iterator().next();
      assertNotNull(thedef);
      thedef.setParam("test", "testvalue");
      fsvc.saveFilter(ifilter);
      ifilter = fsvc.findFilterByName("f1");
      ifilter = fsvc.loadFilter(ifilter.getGUID());
      defs = ifilter.getRuleDefs();
      thedef = defs.iterator().next();
      assertEquals(thedef.getParam("test"), "testvalue");
      thedef.removeParam("test");
      fsvc.saveFilter(ifilter);
      ifilter = fsvc.findFilterByName("f1");
      ifilter = fsvc.loadFilter(ifilter.getGUID());
      defs = ifilter.getRuleDefs();
      thedef = defs.iterator().next();
      assertNull(thedef.getParam("test"));
   }

   /**
    * @throws Exception
    */
   public void testDeleteFilter() throws Exception
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      var ifilter = fsvc.findFilterByName("f1");
      ifilter = fsvc.loadFilter(ifilter.getGUID());
      assertNotNull(ifilter);

      ifilter.setRuleDefs(new HashSet<>());
      fsvc.saveFilter(ifilter);

      ifilter = fsvc.findFilterByName("f1");
      ifilter = fsvc.loadFilter(ifilter.getGUID());
      assertTrue(ifilter != null && ifilter.getRuleDefs().size() == 0);

      fsvc.deleteFilter(ifilter);
   }
}
