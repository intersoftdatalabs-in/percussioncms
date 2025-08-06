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
package com.percussion.services.publisher;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.rx.publisher.IPSPublisherItemStatus;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.rx.publisher.data.PSPubItemStatus;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSAssemblyWorkItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.datasource.PSDatasourceMgrLocator;
import com.percussion.services.datasource.impl.PSDatasourceManager;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.publisher.data.PSEdition;
import com.percussion.services.publisher.data.PSEditionType;
import com.percussion.services.publisher.data.PSSortCriterion;
import com.percussion.util.PSStopwatch;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.MethodOrderer.MethodName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.percussion.rx.publisher.impl.PSPublishingJob.NEXTNUMBER_PUBLICATIONS;

/**
 * Test the publisher service
 * 
 * @author dougrand
 */
// REFACTORED: CP-JAVA11
@TestMethodOrder(MethodName.class)
@Tag(IntegrationTest.class)
public class PSPublisherServiceTest extends ServletTestCase
{
   /**
    * 
    */
   private static final int TEST_EDITION = 55501;

   /**
    * 
    */
   private static final PSGuid TEST_SITE_GUID = new PSGuid(PSTypeEnum.SITE, 3301);
   
   private static final Long TEST_PUB_SERVER_ID = 100L;
   
   /**
    * 
    */
   private static final PSGuid TEST_EDITION_GUID = 
        new PSGuid(PSTypeEnum.EDITION, TEST_EDITION);

   /**
    * 
    */
   private static final String TEST_1 = "TEST_1";
   
   /**
    * 
    */
   private static boolean initialized = false;

   /**
    * 
    */
   private static Random rand = new Random(0);
   
   /**
    * 
    */
   static IPSPublisherService ps = null;
   
   /** (non-Javadoc) 
    * @see junit.framework.TestCase#setUp()
    */
   @Override
   protected void setUp() throws Exception
   {
      if (!initialized)
      {
         initialized = true;
         ps = PSPublisherServiceLocator.getPublisherService();
         try
         {
            var l = ps.loadContentList(TEST_1);
            var cls = new ArrayList<IPSContentList>();
            while (true)
            {
               cls.clear();
               cls.add(l);
               ps.deleteContentLists(cls);
               l = ps.findContentListByName(TEST_1);
            }
         }
         catch (Exception e)
         {
            // Expected
         }
      }

      createTestEdition();
   }

   protected void tearDown()
   {
      deleteTestEdition();
   }

   private void deleteTestEdition()
   {
      try
      {
         var ed = ps.loadEdition(TEST_EDITION_GUID);
         ps.deleteEdition(ed);
      }
      catch (PSNotFoundException e)
      {
         // ignore the error if not found
      }
   }

   private void createTestEdition()
   {
      var ed = new PSEdition();
      ed.setGUID(TEST_EDITION_GUID);
      ed.setComment("one");
      ed.setSiteId(PSGuidUtils.makeGuid(234, PSTypeEnum.SITE));
      ed.setEditionType(PSEditionType.AUTOMATIC);
      try
      {
         ed.setPriority(null);
         fail();
      }
      catch (Exception e)
      {
         ed.setPriority(IPSEdition.Priority.HIGH);
      }

      ps.saveEdition(ed);
   }
   
   /**
    * Test finder for unused content lists
    */
   @Test
   public void test10UnusedContentListFinder()
   {
      var cls = ps.findAllUnusedContentLists();
      assertNotNull(cls);
      assertTrue(cls.size() >= 0);
   }
   
   /**
    * Test CRUD operations on editions and edition tasks.
    */
   @Test
   public void test20EditionCRUD() throws PSNotFoundException {
      var eds = ps.findAllEditions("");
      assertTrue(eds.size() > 0);
      
      deleteTestEdition();
      
      createTestEdition();
      
      var ed = ps.loadEdition(TEST_EDITION_GUID);
      assertNotNull(ed);
      assertEquals(TEST_EDITION_GUID, ed.getGUID());
      assertEquals("one", ed.getComment());
      assertTrue(234 == ed.getSiteId().getUUID());
      assertEquals(PSEditionType.AUTOMATIC, ed.getEditionType());
      assertTrue(IPSEdition.Priority.HIGH.equals(ed.getPriority()));
      
      var task1 = ps.createEditionTask();
      task1.setEditionId(ed.getGUID());
      var task2 = ps.createEditionTask();
      task2.setEditionId(ed.getGUID());
      
      task1.setContinueOnFailure(true);
      task1.setExtensionName("daffy duck");
      task1.setParam("one", "aaa");
      task1.setParam("two", "bbb");
      task1.setSequence(-1);
      ps.saveEditionTask(task1);
      
      task2.setContinueOnFailure(false);
      task2.setExtensionName("porky pig");
      task2.setParam("one", "ccc");
      task2.setSequence(-2);
      ps.saveEditionTask(task2);
      
      var tasks = ps.loadEditionTasks(ed.getGUID());
      assertEquals(2, tasks.size());
      
      task1 = ps.findEditionTaskById(task1.getTaskId());
      task2 = ps.findEditionTaskById(task2.getTaskId());
      
      assertTrue(task1.getContinueOnFailure());
      assertFalse(task2.getContinueOnFailure());
      
      assertEquals("daffy duck", task1.getExtensionName());
      assertEquals("porky pig", task2.getExtensionName());
      
      assertEquals("aaa", task1.getParams().get("one"));
      assertEquals("bbb", task1.getParams().get("two"));
      assertEquals("ccc", task2.getParams().get("one"));
      assertNull(task2.getParams().get("two"));
      
      assertTrue(-1 == task1.getSequence());
      assertTrue(-2 == task2.getSequence());
      
      ed = ps.loadEditionModifiable(ed.getGUID());
      ed.setDisplayTitle("Test edition");
      task1.setSequence(-4);
      ps.saveEdition(ed);
      ps.saveEditionTask(task1);
      
      ed = ps.loadEdition(ed.getGUID());
      task1 = ps.findEditionTaskById(task1.getTaskId());
      
      assertTrue(-4 == task1.getSequence());
      assertEquals("Test edition", ed.getDisplayTitle());
      
      var ed_1 = ps.loadEdition(ed.getGUID());
      assertTrue(ed == ed_1);
      ed_1 = ps.loadEditionModifiable(ed.getGUID());
      assertFalse(ed == ed_1);
      var ed_2 = ps.loadEdition(ed.getGUID());
      assertTrue(ed == ed_2);
      
      ps.deleteEdition(ed);
      
      try
      {
         ps.loadEdition(TEST_EDITION_GUID);
         fail();
      }
      catch (PSNotFoundException e)
      {
      }
      
      assertNull(ps.findEditionTaskById(task1.getTaskId()));
      assertNull(ps.findEditionTaskById(task2.getTaskId()));
   }
   
   /**
    * 
    */
   @Test
   public void test30EditionTaskLogging()
   {
      var log = ps.createEditionTaskLog();
      log.setElapsed(123);
      log.setJobId(10001L);
      log.setMessage("Hello world");
      log.setStatus(true);
      var taskId = PSGuidUtils.makeGuid(2002L, PSTypeEnum.EDITION_TASK_DEF);
      log.setTaskId(taskId);
      ps.saveEditionTaskLog(log);
      
      log = ps.loadEditionTaskLog(log.getReferenceId());
      assertEquals(Integer.valueOf(123), log.getElapsed());
      assertTrue(10001 == log.getJobId());
      assertEquals("Hello world", log.getMessage());
      assertEquals(taskId, log.getTaskId());
      assertTrue(log.getStatus());
      
      var entries = ps.findEditionTaskLogEntriesByJobId(10001L);
      assertNotNull(entries);
      assertTrue(entries.size() > 0);
      
      ps.purgeJobLog(10001);
      
      entries = ps.findEditionTaskLogEntriesByJobId(10001L);
      assertEquals(0, entries.size());
   }
   

   
   /**
    *
    */
   @Test
   public void test40EditionContentLists()
   {
      var lists = ps.loadEditionContentLists(new PSGuid(PSTypeEnum.EDITION, 301));
      assertNotNull(lists);
      assertTrue(lists.size() > 0);
   }
   
   /**
    * @throws Exception
    */
   @Test
   public void test50CreationAndInsert() throws Exception
   {
      var cl = ps.createContentList(TEST_1);
      setupContentList(cl);
      var cls = new ArrayList<IPSContentList>();
      cls.add(cl);
      ps.saveContentLists(cls);
   }
   
   /**
    * Create an artificial setup of data in site items and pub status to
    * test the unpublishing and site items purging code.
    * @throws Exception
    */
   @Test
   public void test60Unpublish() throws Exception
   {
      var gmgr = PSGuidManagerLocator.getGuidMgr();
      var asm = PSAssemblyServiceLocator.getAssemblyService();
      var job = gmgr.createId(NEXTNUMBER_PUBLICATIONS);
      var template = asm.findTemplateByName("rffBnImage");

      var dmgr = (PSDatasourceManager) PSDatasourceMgrLocator.getDatasourceMgr();
      var session = dmgr.getHibernateSession();

      cleanupSiteItems(session);
      
      long ref = 20000;
      
      ps.initPublishingStatus(job, new Date(), TEST_EDITION_GUID);
      var stati = new ArrayList<IPSPublisherItemStatus>();

      var stat = createPubStatus(job, ref++, 342, template);
      stat.setFolderId(gmgr.makeGuid(new PSLocator(310)));
      stati.add(stat);
      
      stat = createPubStatus(job, ref++, 343, template);
      stat.setFolderId(gmgr.makeGuid(new PSLocator(10101)));
      stati.add(stat);
      
      var parent = gmgr.makeGuid(new PSLocator(311));
      stat = createPubStatus(job, ref++, 11111344, template);
      stat.setFolderId(parent);
      stati.add(stat);
      
      stat = createPubStatus(job, ref++, 345, template);
      stat.setFolderId(parent);
      stati.add(stat);
      ps.updatePublishingInfo(stati);
      
      var cms = PSCmsObjectMgrLocator.getObjectManager();
      var sum345 = cms.loadComponentSummary(345);
      sum345.setContentStateId(7);
      try
      {
         cms.saveComponentSummaries(Collections.singletonList(sum345));
         
         var rids = ps.findReferenceIdsToUnpublish(TEST_SITE_GUID, "u");
         assertTrue(rids.size() > 0);
         assertTrue(rids.contains(20002L));
         assertTrue(rids.contains(20003L));
         var unique = new HashSet<Long>(rids);
         assertEquals(2, unique.size());
      }
      finally
      {
         sum345.setContentStateId(5);
         cms.saveComponentSummaries(Collections.singletonList(sum345));
         cleanupSiteItems(session);
      }
   }

   /**
    * Cleanup items inserted into the site items table and status log
    * during testing.
    * 
    * @param session the session assumed never <code>null</code>
    */
   private void cleanupSiteItems(Session session)
   {
      var q = session.createQuery("delete from PSSiteItem where referenceId > 9999");
      q.executeUpdate();

      q = session.createQuery("delete from PSPubItem where referenceId > 9999");
      q.executeUpdate();

      q = session.createQuery("delete from PSPubStatus where editionId = "
            + TEST_EDITION);
      q.executeUpdate();
   }

   /**
    * @throws Exception
    */
   @Test
   public void test70LoadOneWithParams() throws Exception
   {
      var cl = ps.loadContentList(TEST_1);
      assertNotNull(cl.getExpanderParams());
      assertNotNull(cl.getGeneratorParams());
      assertTrue(cl.getExpanderParams().size() > 0);
      assertTrue(cl.getGeneratorParams().size() > 0);
      assertNotNull(cl.getFilter());
   }
   
   /**
    * @throws Exception
    */
   @Test
   public void test80Update() throws Exception
   {
      var cl = ps.loadContentList(TEST_1);
      cl.removeExpanderParam("siteid");
      cl.addExpanderParam("fooid", "12010");
      var cls = new ArrayList<IPSContentList>();
      cls.add(cl);
      ps.saveContentLists(cls);
   }

   /**
    * @throws Exception
    */
   @Test
   public void test90ContentListSerialization() throws Exception
   {
      var cl = ps.loadContentList(TEST_1);
      var ser = cl.toXML();

      var copy = ps.createContentList(TEST_1);
      copy.fromXML(ser);
      
      assertEquals(cl, copy);
   }

   /**
    * @param cl
    * @throws PSFilterException
    */
   private void setupContentList(IPSContentList cl) throws PSFilterException
   {
      var fsvc = PSFilterServiceLocator.getFilterService();
      cl.setDescription("Test description");
      cl.setEditionType(PSEditionType.AUTOMATIC);
      cl.setExpander("sys_SiteTemplateExpander");
      cl.setGenerator("sys_SearchGenerator");
      cl.addExpanderParam("siteid", "301");
      cl.addGeneratorParam("query",
            "select rx:sys_contentid,rx:sys_contenttypeid from rx:generic");
      cl.setFilterId(fsvc.findFilterByName("public").getGUID());
      cl.setUrl("/contentlistservlettest");
   }

   /**
    * @throws Exception
    */
   @Test
   public void test92Deletion() throws Exception
   {
      var cl = ps.loadContentList(TEST_1);
      var cls = new ArrayList<IPSContentList>();
      cls.add(cl);
      ps.deleteContentLists(cls);
   }
   
   /**
    * @throws Exception
    */
   @Test
   public void test94LoadExisting() throws Exception
   {
      var ids = new ArrayList<IPSGuid>();
      var id = new PSGuid(PSTypeEnum.CONTENT_LIST, 310);
      ids.add(id);
      var cls = ps.loadContentLists(ids);
      assertEquals(cls.size(), 1);
      var clist = cls.get(0);
      assertEquals("rffEiFullBinary", clist.getName());
      assertNotNull(clist.getExpander());
      assertNotNull(clist.getFilter());
      
      clist = ps.loadContentList(id);
      assertNotNull(clist.getFilter());
      
      clist = ps.loadContentListModifiable(id);
      assertNull(clist.getFilter());
      
      clist = ps.loadContentList("rffEiIncremental");
      assertEquals("rffEiIncremental", clist.getName());
      assertNotNull(clist.getFilter());
      
      clist = ps.findContentListByName("Unknown ContentList");
      assertNull(clist);
      
      try
      {
         ps.loadContentList("Unknown ContentList");
         fail();
      }
      catch (PSNotFoundException e)
      {
         // should be here.
      }
   }
   
   /**
    * @throws Exception
    */
   @Test
   public void test96SiteItems() throws Exception
   {
      var gmgr = PSGuidManagerLocator.getGuidMgr();
      var asm = PSAssemblyServiceLocator.getAssemblyService();
      var job1 = gmgr.createId(NEXTNUMBER_PUBLICATIONS);
      var template = asm.findTemplateByName("rffBnImage");

      var dmgr = (PSDatasourceManager) PSDatasourceMgrLocator.getDatasourceMgr();
      var session = dmgr.getHibernateSession();
      cleanupSiteItems(session);
   
      var editionguid = gmgr.makeGuid(TEST_EDITION, PSTypeEnum.EDITION);
      ps.initPublishingStatus(job1, new Date(), editionguid);
      
      var sw = new PSStopwatch();
      int count = 1000;
      var stati = new ArrayList<IPSPublisherItemStatus>();
      sw.start();
      for (int i = 0; i < count; i++)
      {
         var stat = createPubStatus(job1, i + 10000, i + 300,
               template);
         stati.add(stat);
      }
      ps.updatePublishingInfo(stati);
      sw.stop();
      
      ps.finishedPublishingStatus(job1, new Date(),
            IPSPubStatus.EndingState.COMPLETED);
      
      System.out.println("Updating " + count + " pub status items took " + sw);
      
      var items = ps.findSiteItems(TEST_SITE_GUID, 0);
      assertNotNull(items);

      var refIds = ps.findRefIdForJob(job1, null);
      assertNotNull(refIds);
      assertTrue(refIds.size() > 0);
            
      var guid = new PSLegacyGuid(305, 1);
      var dummyPath = "/dummypath305";

      assertEmptyUnpublishInfo(guid, dummyPath, template);
      
      var item = new PSAssemblyWorkItem();
      item.setId(guid);
      item.setSiteId(TEST_SITE_GUID);
      item.setTemplate(template);
      item.setDeliveryPath(dummyPath);
      
      var data = ps.findUnpublishInfoForAssemblyItem(item.getId(), item
            .getDeliveryContextId(), item.getTemplate().getGUID(), item
            .getSiteId(), null, item.getDeliveryPath());
      assertNotNull(data);
      assertEquals(4, data.length);
      assertEquals("filesystem", data[0]);
      assertTrue((Long) data[1] > 0L);
      assertNull(data[2]);
      assertEquals(351, data[3]);

      var sort = new ArrayList<PSSortCriterion>();
      sort.add(new PSSortCriterion("location", true));
      refIds = ps.findRefIdForJob(job1, sort);
      assertNotNull(refIds);
      assertTrue(refIds.size() > 0);
      
      var results = ps.findPubStatusByEdition(new PSGuid(PSTypeEnum.EDITION, TEST_EDITION));
      assertNotNull(results);
      assertTrue(results.size() > 0);
      var stat = results.iterator().next();
      assertTrue(stat.getDeliveredCount() > 0);
      assertTrue(stat.getDeliveredCount() > stat.getFailedCount());
      assertTrue(stat.getFailedCount() > 0);
      assertTrue(stat.getRemovedCount() > 0);
      sw.stop();
      System.out.println("Retrieved status data " + results.size() +
            " elements in " + sw);

      results = ps.findAllPubStatus();
      assertNotNull(results);
      assertTrue(results.size() > 0);
      
      var jobids = ps.findExpiredJobs(new Date());
      assertNotNull(jobids);
      assertTrue(jobids.contains(job1));
      
      jobids = ps.findExpiredAndHiddenJobs(new Date());
      assertNotNull(jobids);
      assertTrue(jobids.contains(job1));
      
      for (long jobid : jobids)
      {
         ps.purgeJobLog(jobid);
      }
      
      var job2Date = new Date();

      long job2 = gmgr.createId(NEXTNUMBER_PUBLICATIONS);
      ps.initPublishingStatus(job2, job2Date, editionguid);
      
      ps.finishedPublishingStatus(job2, new Date(),
            IPSPubStatus.EndingState.COMPLETED);
      
      job2Date.setTime(job2Date.getTime() - 100000);
      jobids = ps.findExpiredAndHiddenJobs(job2Date);
      assertNotNull(jobids);
      assertTrue(jobids.contains(job1));
      assertFalse(jobids.contains(job2));
            
      jobids = ps.findExpiredAndHiddenJobs(new Date());
      assertNotNull(jobids);
      assertTrue(jobids.contains(job1));
      assertTrue(jobids.contains(job2));
      
      cleanupSiteItems(session);
   }
   
   private void assertEmptyUnpublishInfo(IPSGuid guid, String dummyPath, IPSAssemblyTemplate template)
   {
      var item = new PSAssemblyWorkItem();
      item.setId(guid);
      item.setSiteId(TEST_SITE_GUID);
      item.setPubServerId(TEST_PUB_SERVER_ID);
      item.setTemplate(template);
      item.setDeliveryPath(dummyPath);
      
      var data = ps.findUnpublishInfoForAssemblyItem(item.getId(), item
            .getDeliveryContextId(), item.getTemplate().getGUID(), item
            .getSiteId(), item.getPubServerId(), item.getDeliveryPath());
      assertTrue(data == null || data.length == 0);
   }

   /**
    * 
    * @param job 
    * @param ref 
    * @param cid  
    * @param templ 
    * @return a new status object
    */
   private PSPubItemStatus createPubStatus(long job, long ref,
         int cid, IPSAssemblyTemplate templ)
   {
      var s = IPSPublisherJobStatus.ItemState.DELIVERED;
      String message = null;

      if (rand.nextInt(25) == 1)
      {
         s = IPSPublisherJobStatus.ItemState.FAILED;
         message = "random failure " + rand.nextInt(5);
      }

      var rval = new PSPubItemStatus(ref, job, -1, 1, s);
      var item = new PSAssemblyWorkItem();
      item.setAssemblyUrl("/dummy");
      item.setElapsed(rand.nextInt(1000) + 500);
      item.setId(new PSLegacyGuid(cid, 1));
      item.setPublish(rand.nextInt(50) != 1);
      item.setDeliveryPath("/dummypath" + cid);
      item.setSiteId(TEST_SITE_GUID);
      item.setTemplate(templ);
      item.setDeliveryType("filesystem");
      item.setFolderId(351);
      rval.extractInfo(item);
      if (message != null)
      {
         rval.addMessage(message);
         if (rand.nextInt(5) == 1)
         {
            rval.addMessage("Another message");
         }
      }
      return rval;
   }

   /**
    * Get the delivery type by name.
    * @param tname the name of the delivery type, may be <code>null</code> or
    *    empty.
    * @return the delivery type with the specified name. It may be 
    *    <code>null</code> if the delivery type is not found.
    */
   private IPSDeliveryType findDeliveryType(String tname)
   {
      try
      {
         return ps.loadDeliveryType("dt1");
      }
      catch (PSNotFoundException e)
      {
         return null;
      }
   }
   
   /**
    * Test CRUD operations on delivery types
    */
   @Test
   public void tes98tDeliveryTypes() throws PSNotFoundException {
      IPSDeliveryType ttd = null;

      ttd = findDeliveryType("dt1");

      while (ttd != null)
      {
         ps.deleteDeliveryType(ttd);
         ttd = findDeliveryType("dt1");
      }

      var type = ps.createDeliveryType();
      type.setBeanName("abc");
      type.setDescription("abc dt");
      type.setName("dt1");
      type.setUnpublishingRequiresAssembly(false);
      ps.saveDeliveryType(type);

      type = ps.loadDeliveryType(type.getGUID());
      assertEquals("abc", type.getBeanName());
      assertEquals("abc dt", type.getDescription());
      assertEquals("dt1", type.getName());
      assertEquals(false, type.isUnpublishingRequiresAssembly());

      var type2 = ps.loadDeliveryType(type.getGUID());
      assertTrue(type == type2);

      var type3 = ps.loadDeliveryTypeModifiable(type.getGUID());
      assertTrue(type != type3);

      ps.saveDeliveryType(type3);

      ps.deleteDeliveryType(type);
   }
}
