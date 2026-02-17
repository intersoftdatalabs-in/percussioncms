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
package com.percussion.search;

import com.percussion.testing.IPSServerBasedJunitTest;
import com.percussion.xml.PSXmlDocumentBuilder;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Unit tests for the full-text search engine. Tests very basic functionality,
 * that the server will start and stop and the standard objects (admin, query,
 * and indexer) can all be obtained.
 *
 * @author paulhoward
 */

public class PSSearchEngineTest
   implements IPSServerBasedJunitTest
{



   /**
    * Must be called once after all tests requiring the engine are complete.
    *
    * @throws PSSearchException
    */
   @Test
   public void testShutdown()
      throws PSSearchException
   {
      PSSearchEngine eng = PSSearchEngine.getInstance();
      eng.shutdown(false);
      assertTrue(eng.getStateCode() == PSSearchEngine.STATUS_TERMINATED, "Incorrect status.");
      assertTrue(!eng.isAvailable(), "Engine falsely claims available.");
      assertTrue(eng.getStateString().trim().length() != 0, "Status string is empty.");
      //leave in running state for other tests
      eng.start();
      assertTrue(eng.isAvailable(), "Engine failed to restart");
   }


   /**
    * Validates that a status element is returned.
    * <p>Assumes the engine has already been initialized.
    * @throws PSSearchException
    */
   @Test
   public void testGetStatus()
      throws PSSearchException
   {
      PSSearchEngine eng = PSSearchEngine.getInstance();
      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      Element e = eng.getStatus(doc);
      assertNotNull(e, "Missing status.");
      String status = e.getAttribute("runningStatus");
      assertEquals(eng.getStateString(), status, "Incorrect status string");
      assertEquals("SearchStatus", e.getNodeName(), "Incorrect root tag name.");
      // Get engine status
      NodeList nodes = e.getElementsByTagName("status");
      assertNotNull(nodes, "Missing children");
      Element engine = (Element) nodes.item(0);
      assertNotNull(engine, "Missing engine status");
      assertNotNull(engine.getAttribute("state"), "Missing attribute for state");
      Element indexer = (Element) nodes.item(1);
      assertNotNull(engine, "Missing indexer status");
      assertNotNull(indexer.getAttribute("state"), "Missing attribute for state");
      assertNotNull(indexer.getAttribute("uncommited-libs-count"), "Missing library count");
      assertNotNull(indexer.getAttribute("file-delete-count"), "Missing file count");
   }

   /**
    * Validates that all getters return a valid object and don't throw.
    * <p>Assumes the engine has already been initialized.
    *
    * @throws PSSearchException
    * @throws PSAdminLockedException
    */
   @Test
   public void testGetters()
      throws PSSearchException, PSAdminLockedException
   {
      PSSearchEngine eng = PSSearchEngine.getInstance();

      PSSearchAdmin sa = eng.getSearchAdmin(true);
      assertNotNull(sa, "Missing admin.");
      eng.releaseSearchAdmin(sa);

      sa = eng.getSearchAdmin(false);
      assertNotNull(sa, "Missing read-only admin.");

      PSSearchIndexer si = eng.getSearchIndexer();
      assertNotNull(si, "Missing indexer.");
      eng.releaseSearchIndexer(si);

      PSSearchQuery sq = eng.getSearchQuery();
      assertNotNull(sq, "Missing query.");
      eng.releaseSearchQuery(sq);
   }



   /**
    * The loadable handler will call this method once before any test method.
    *
    * @param req The request that was passed to the loadable handler.
    *            Never <code>null</code>;
    */
   @Override
   public void oneTimeSetUp(Object req) {

   }

   /**
    * Unused.
    * @param req unused
    */
   public void oneTimeTearDown()
   {}
}
