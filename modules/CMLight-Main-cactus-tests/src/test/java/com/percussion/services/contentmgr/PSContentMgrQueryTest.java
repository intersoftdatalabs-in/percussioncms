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
package com.percussion.services.contentmgr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.junit.jupiter.api.Disabled;

import com.percussion.utils.jsr170.PSLongValue;
import com.percussion.utils.jsr170.PSStringValue;
import org.junit.jupiter.api.Tag;

/**
 * Test jcr query facilities
 * 
 * @author dougrand
 */
@Tag(IntegrationTest.class)
public class PSContentMgrQueryTest extends ServletTestCase
{
   /**
    * Content manager instance
    */
   private static IPSContentMgr mgr = null;

   public RowIterator performTest(String testquery, String lang) throws Exception
   {
      mgr = PSContentMgrLocator.getContentMgr();
      var q = mgr.createQuery(testquery, lang);
      var r = q.execute();
      var riter = r.getRows();
      return riter;
   }
   
   public RowIterator performTest(String testquery, String lang, int numResult)
      throws Exception
   {
      var riter = performTest(testquery, lang);
      if (numResult == 0 )
         assertTrue(riter.getSize() == 0);
      else
         assertTrue(riter.getSize() >= numResult);
      return riter;
   }
   
   public void performTestZeroResults(String testquery, String lang)
   throws Exception
   {
      performTest(testquery, lang, 0);
   }   
   
   public void testSimpleQuery1() throws Exception
   {
      mgr = PSContentMgrLocator.getContentMgr();
      var q = mgr.createQuery("SELECT rx:sys_title FROM rx:rffFile "
            + "WHERE rx:filename like '%.pdf'", Query.SQL);

      var r = q.execute();
      var riter = r.getRows();
      assertTrue(riter.getSize() > 0);
      
      var row = riter.nextRow();
      assertNotNull(row.getValue("rx:sys_title"));
      
      var niter = r.getNodes();
      var node = niter.nextNode();
      assertNotNull(node);
   }

   public void testCrossSiteItems() throws Exception
   {
      mgr = PSContentMgrLocator.getContentMgr();
      var q = mgr.createQuery("SELECT rx:sys_contentid, rx:sys_folderid, " +
            "jcr:path, rx:sys_communityid " +
            "FROM nt:base " + 
            "WHERE jcr:path like '//Sites/CorporateInvestments/%' AND " + 
            "rx:sys_communityid = 1002 ORDER BY rx:sys_contentid", Query.SQL);
      
      var r = q.execute();
      var riter = r.getRows();
      assertTrue(riter.getSize() == 7);

      var row = riter.nextRow();
      validateCrossSiteRow(row, 442, 538, 1002,
            "//Sites/CorporateInvestments/Images/Icons");
      row = riter.nextRow();
      validateCrossSiteRow(row, 449, 537, 1002,
            "//Sites/CorporateInvestments/Images/Housing");
   }

   @Disabled("junit.framework.AssertionFailedError: null")
   public void testMultiFolderPaths() throws Exception
   {
      mgr = PSContentMgrLocator.getContentMgr();
      var q = mgr.createQuery("SELECT rx:sys_contentid, rx:sys_folderid, " +
            "jcr:path, rx:sys_communityid " +
            "FROM nt:base " + 
            "WHERE rx:sys_contentid = 460", Query.SQL);
      
      var r = q.execute();
      var riter = r.getRows();
      assertTrue(riter.getSize() == 2);

      var row = riter.nextRow();
      validateCrossSiteRow(row, 460, 446, 1002,
            "//Sites/EnterpriseInvestments/Images/People");
      row = riter.nextRow();
      validateCrossSiteRow(row, 460, 539, 1002,
            "//Sites/CorporateInvestments/Images/People");
   }

   private void validateCrossSiteRow(Row r, long contentID, long folderID,
         long communityID, String folderPath) throws Exception
   {
      var id = (PSLongValue) r.getValue("rx:sys_contentid");
      assertTrue(id.getLong() == contentID);
      id = (PSLongValue) r.getValue("rx:sys_folderid");
      assertTrue(id.getLong() == folderID);
      id = (PSLongValue) r.getValue("rx:sys_communityid");
      assertTrue(id.getLong() == communityID);
      var path = (PSStringValue) r.getValue("jcr:path");
      assertTrue(path.getString().equals(folderPath));
   }
   
   public void disabled_testSimpleQuery2() throws Exception
   {
      performTest("SELECT rx:sys_title FROM nt:base "
            + "WHERE rx:displaytitle like '%fund%'", Query.SQL);
   }

   @Disabled("org.hibernate.exception.SQLGrammarException: could not execute query on Derby")
   public void testSimpleQuery3() throws Exception
   {
      performTest("SELECT rx:sys_title FROM nt:base "
            + "WHERE rx:sys_contentstartdate > '2004/8/1' "
            + "ORDER BY rx:sys_title asc, rx:sys_folderid desc",
            Query.SQL, 250);
   }

   public void testSimpleQueryPath1() throws Exception
   {
      performTest("SELECT rx:sys_title FROM rx:rffBrief "
                  + "WHERE jcr:path like '/jcr:root/Sites/EnterpriseInvestments/%'",
                  Query.SQL, 1);
   }

   public void testSimpleQueryPath2() throws Exception
   {
      performTest("SELECT rx:sys_title FROM rx:rfffile "
            + "WHERE jcr:path like '/jcr:root/Sites/EnterpriseInvestments/Files/%'",
            Query.SQL, 2);
   }
   
   @Disabled("org.hibernate.exception.SQLGrammarException: could not execute query")
   public void testMissingProp() throws Exception
   {
      performTest("SELECT rx:sys_title, rx:filename FROM nt:base "
            + "WHERE rx:filename is not null",
            Query.SQL, 37);     
   }
   
   public void testProjections() throws Exception
   {
      performTest("SELECT jcr:path from rx:rffgeneric", Query.SQL, 108);
      performTest("SELECT rx:sys_title from rx:rffgeneric", Query.SQL, 108);
      performTest("SELECT rx:sys_contentmodifieddate, rx:sys_contenttypeid " +
            "from rx:rffgeneric", Query.SQL, 108);
   }
   
   public void fixme_testNodeResults() throws Exception
   {
      performTest("select * from rx:rffgeneric " +
            "where rx:displaytitle='EI Insurance'", Query.SQL, 4);
      performTest("select * from rx:rffevent", Query.SQL, 17);
      performTest("select * from rx:rffpressrelease", Query.SQL, 20);
   }
   
   public void fixme_testLobs() throws Exception
   {
      performTest("SELECT jcr:path from rx:rffgeneric " +
            "where rx:body is not null", Query.SQL, 108);
      performTest("SELECT jcr:path from rx:rffgeneric " +
            "where rx:body is not null order by rx:displaytitle", Query.SQL, 108);
      performTest("SELECT jcr:path, rx:body from rx:rffgeneric " +
            "order by rx:displaytitle", Query.SQL, 200);
      performTest("SELECT jcr:path from rx:rffimage " +
            "where rx:img1 is not null", Query.SQL, 31);
      
      performTestZeroResults("SELECT jcr:path from rx:rffimage " +
            "where rx:img1 is null", Query.SQL);
      performTestZeroResults("SELECT jcr:path from rx:rffgeneric " +
            "where rx:body is null", Query.SQL);
   }
}
