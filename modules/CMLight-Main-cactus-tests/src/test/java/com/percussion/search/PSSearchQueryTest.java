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

import com.percussion.cms.objectstore.PSKey;
import com.percussion.testing.IPSServerBasedJunitTest;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.Tag;


/**
 * JUnit test that validates the query capability of the RW search engine.
 * The indexer handler is used to submit test documents.
 * <p>This test must be run in the server context and requires the rx_generic_ce
 * content type to be present and running. This method adds data to the index.
 *
 * @author paulhoward
 */

@TestInstance(Lifecycle.PER_CLASS)
@Disabled("Temporarily disabled — failing in perc-system test run")
public class PSSearchQueryTest
   implements IPSServerBasedJunitTest
{


   /**
    * Add some docs and perform a field based query.
    *
    * @throws PSSearchException If any unexpected problems occur.
    * @throws SQLException If content id can't be generated for test fragments.
    */
   @Test
   public void testFieldQuery()
      throws PSSearchException, SQLException
   {
      boolean success = false;
      PSSearchEngine eng = PSSearchEngine.getInstance();
      PSKey ctypeKey = PSSearchIndexerTest.createContentTypeKey();
      PSSearchIndexer si = eng.getSearchIndexer();
      si.clearIndex(ctypeKey);
      String specialWord = "qqqyy";

      int originalDocCount = getDocCount(eng, ctypeKey, specialWord);

      // add test docs
      String testField = "sys_title";
      Map data = PSSearchIndexerTest.getDocData(specialWord);
      data.put(testField, "Red grEEn");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);
      data.put(testField, "Red BLUE");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);
      data.put(testField, "blue yellow");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);
      data.put(testField, "purple");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);

      assertEquals(originalDocCount+4, getDocCount(eng, ctypeKey, specialWord), "Wrong number of docs after adding test docs.");

      PSSearchQuery sq = null;
      try
      {
         sq = eng.getSearchQuery();
         Collection ctypeIds = new ArrayList();
         ctypeIds.add(ctypeKey);

         Map fq = new HashMap();
         fq.put(testField, "blue");
         List results = sq.performSearch(ctypeIds, null, fq);
         assertEquals(2, results.size(), "Wrong doc count for field search on 'blue'");

         fq.put(testField, "purple");
         results = sq.performSearch(ctypeIds, null, fq);
         assertEquals(1, results.size(), "Wrong doc count for field search on 'purple'");

         fq.put(testField, "red green");
         results = sq.performSearch(ctypeIds, null, fq);
         assertEquals(1, results.size(), "Wrong doc count for field search on 'red green'");
         success = true;
      }
      finally
      {
         PSSearchException pse = null;
         try
         {
            if (null != sq)
               eng.releaseSearchQuery(sq);
         }
         catch (PSSearchException se)
         {
            pse = se;
         }
         try
         {
            if (null != si)
               eng.releaseSearchIndexer(si);
         }
         catch (PSSearchException se)
         {
            pse = se;
         }
         //we don't want to hide the exception if currently unwinding stack
         if (success && null != pse)
            throw pse;
      }
   }


   /**
    * Add some docs and perform a body based query. Limit the results using the
    * maxResults property.
    *
    * @throws PSSearchException If any unexpected problems occur.
    * @throws SQLException If content id can't be generated for test fragments.
    */
   @Test
   public void testMaxResults()
      throws PSSearchException, SQLException
   {
      boolean success = false;
      PSSearchEngine eng = PSSearchEngine.getInstance();
      PSKey ctypeKey = PSSearchIndexerTest.createContentTypeKey();
      PSSearchIndexer si = eng.getSearchIndexer();
      si.clearIndex(ctypeKey);
      String specialWord = "qqqyy";

      String testField = BODY_FIELD_NAME;
      Map data = PSSearchIndexerTest.getDocData(specialWord);
      data.put(testField, "Red grEEn");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);
      data.put(testField, "Red blue");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);
      data.put(testField, "Red yellow");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);
      data.put(testField, "Red white");
      PSSearchIndexerTest.addDocs(null, data, 1, -1, true);

      PSSearchQuery sq = null;
      try
      {
         sq = eng.getSearchQuery();
         Collection ctypeIds = new ArrayList();
         ctypeIds.add(ctypeKey);

         List results = sq.performSearch(ctypeIds, "red", null);
         assertEquals(4, results.size(), "Wrong doc count for field search on 'red'");
         //now limit the result set size
         Map control = new HashMap();
         int limit = 2;
         control.put(PSSearchQuery.QUERYPROP_MAXRESULTS, Integer.valueOf(limit));
         results = sq.performSearch(ctypeIds, "red", null, control);
         assertEquals(limit, results.size(), "Too many docs when maxResults set");

         success = true;
      }
      finally
      {
         PSSearchException pse = null;
         try
         {
            if (null != sq)
               eng.releaseSearchQuery(sq);
         }
         catch (PSSearchException se)
         {
            pse = se;
         }
         try
         {
            if (null != si)
               eng.releaseSearchIndexer(si);
         }
         catch (PSSearchException se)
         {
            pse = se;
         }
         //we don't want to hide the exception if currently unwinding stack
         if (success && null != pse)
            throw pse;
      }
   }

   /**
    * Performs a search against the library associated with the supplied key
    * and counts the resulting docs.
    * <p>Available for use by other unit tests.
    *
    * @param eng Never <code>null</code>.
    * @param cTypeKey Never <code>null</code>.
    * @param query Never <code>null</code> or empty.
    *
    * @return How many docs match the supplied query string.
    *
    * @throws PSSearchException If any unexpected problems occur.
    */
   static int getDocCount(PSSearchEngine eng, PSKey cTypeKey, String query)
      throws PSSearchException
   {
      if (null == eng)
      {
         throw new IllegalArgumentException("eng cannot be null");
      }
      if (null == cTypeKey)
      {
         throw new IllegalArgumentException("ctype key cannot be null");
      }
      if (null == query || query.trim().length() == 0)
      {
         throw new IllegalArgumentException("query cannot be null or empty");
      }
      PSSearchQuery sq = null;
      List results = new ArrayList();
      try
      {
         sq = eng.getSearchQuery();
         Collection cTypeIds = new ArrayList();
         cTypeIds.add(cTypeKey);
         Map fieldQueries = new HashMap();
         results = sq.performSearch(cTypeIds, query, fieldQueries);
         Iterator dump = results.iterator();
         while (dump.hasNext())
         {
            PSSearchResult rs = (PSSearchResult) dump.next();
            System.out.println("      Locator = " + rs.getKey().getId()
                  + " : Rel = " + rs.getRelevancy());
         }

      }
      catch (PSSearchException se)
      {
         //if the library is empty, we get a 16003 code back
         assertEquals(IPSSearchErrors.SEARCH_ENGINE_NO_SEARCH_TERMS, se.getErrorCode(), "Unexpected error code querying docs");
      }
      finally
      {
          if (null != sq)
            eng.releaseSearchQuery(sq);
      }
      System.out.println("Found " + results.size() + " docs for query: "
            + query + "(ctype: " + cTypeKey.getPart(cTypeKey.getDefinition()[0])
            + ")");
      return results.size();
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

   /* (non-Javadoc)
    * @see IPSServerBasedJunitTest#oneTimeTearDown()
    */
   public void oneTimeTearDown()
   {
      // noop
   }

   /**
    * The name of a field in the rxs_generic_ce content type that will be used
    * for testing. The field should be the one that stores the main content of
    * the item.
    */
   private static final String BODY_FIELD_NAME = "body";
}
