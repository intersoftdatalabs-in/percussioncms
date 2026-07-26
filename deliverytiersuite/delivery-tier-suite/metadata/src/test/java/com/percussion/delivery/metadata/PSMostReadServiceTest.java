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

package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.metadata.data.PSVisitQuery;
import com.percussion.delivery.metadata.rdbms.impl.PSDbBlogPostVisit;
import com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry;
import com.percussion.security.error.PSExceptionUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@Transactional(isolation = Isolation.READ_UNCOMMITTED, propagation = Propagation.NESTED)
@ContextConfiguration(locations = {"classpath:test-beans.xml"})
@TestMethodOrder(MethodName.class)
public class PSMostReadServiceTest {

  private static final Logger log = LogManager.getLogger(PSMostReadServiceTest.class);

  @Autowired private IPSBlogPostVisitService blogPostService;

  @Autowired public IPSMetadataIndexerService indexer;

  /** Used to set the number of items to create */
  private static final int ENTRY_COUNT = 5;

  /**
   * Used to set the max entries when testing large numbers of hits 1005 - entry_count(5) = 1000
   * posts
   */
  private static final int MAX_COUNT = 1005;

  /** hack name for site */
  private static final String SITE_NAME = "www.holy-moly.com";

  /** hack name for page */
  private static final String PAGE_NAME = "/page";

  /** the site name and page name */
  private static final String PAGE_FULL = "/" + SITE_NAME + PAGE_NAME;

  @BeforeEach
  public void before() {

    try {

      indexer.deleteAllMetadataEntries();
      addEntries();
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * Checks Basic query with 1 hit on each page
   *
   * @throws Exception
   */
  @Test
  public void testA() throws Exception {
    PSVisitQuery query = new PSVisitQuery();
    query.setLimit("3");
    query.setSectionPath("");
    query.setSortOrder("desc");
    query.setTimePeriod("WEEK");
    List<String> topPages = blogPostService.getTopVisitedBlogPosts(query);

    assertEquals(
        List.of(PAGE_FULL + "4.html", PAGE_FULL + "3.html", PAGE_FULL + "2.html"),
        topPages,
        "equally visited pages should use descending page path as the tie-breaker");
  }

  /**
   * Mixes up the page order manually and then ensures query works correctly with different query
   * params
   *
   * @throws Exception
   */
  @Test
  public void testB() throws Exception {
    PSVisitQuery query = new PSVisitQuery();
    query.setLimit("5");
    query.setSectionPath(PAGE_FULL);
    query.setSortOrder("desc");
    query.setTimePeriod("ALLTIME");

    List<String> topPages = blogPostService.getTopVisitedBlogPosts(query);

    mixUpBlogPostHits(topPages);

    topPages = blogPostService.getTopVisitedBlogPosts(query);

    assertEquals(5, topPages.size(), "list should contain 5 items");

    String testName = PAGE_FULL + "0.html";
    assertEquals(testName, topPages.get(4), "page0.html should have the most hits");

    testName = PAGE_FULL + "2.html";
    assertEquals(testName, topPages.get(2), "page2.html should have 2nd most hits");

    testName = PAGE_FULL + "1.html";
    assertEquals(testName, topPages.get(3), "page1.html should have 3rd most hits");
  }

  /**
   * checks a few more params
   *
   * @return Exception
   */
  @Test
  public void testC() throws Exception {
    PSVisitQuery query = new PSVisitQuery();
    query.setLimit("5");
    // should return nothing with this section path selected
    query.setSectionPath("/test");
    query.setSortOrder("asc");
    query.setTimePeriod("ALLTIME");

    List<String> pagePaths = blogPostService.getTopVisitedBlogPosts(query);

    assertEquals(0, pagePaths.size(), "Page paths size should be 0");
  }

  @Test
  public void testD() throws Exception {
    PSVisitQuery query = new PSVisitQuery();
    query.setLimit("1");
    query.setSectionPath("");
    query.setSortOrder("asc");
    query.setTimePeriod("ALLTIME");

    List<String> pagePaths = blogPostService.getTopVisitedBlogPosts(query);

    assertEquals(1, pagePaths.size(), "list size should be 1");

    String testName = PAGE_FULL + "4.html";
    assertEquals(testName, pagePaths.get(0), "least hit page should be page4.html");

    query.setSortOrder("desc");
    pagePaths = blogPostService.getTopVisitedBlogPosts(query);

    testName = PAGE_FULL + "0.html";
    assertEquals(testName, pagePaths.get(0), "page0.html should still have most hits");
  }

  @Test
  public void testE() throws Exception {
    // tests many items in DB
    addManyEntries();

    PSVisitQuery query = new PSVisitQuery();
    // 25 is the current max allowed limit for the query in the UI
    query.setLimit("25");
    query.setSectionPath("");
    query.setSortOrder("asc");
    query.setTimePeriod("ALLTIME");

    List<String> topPosts = blogPostService.getTopVisitedBlogPosts(query);

    assertEquals(25, topPosts.size(), "List size is 25");

    // ensure 155 still got set regardless of UI max limit
    query.setLimit("155");

    topPosts = blogPostService.getTopVisitedBlogPosts(query);
    assertEquals(155, topPosts.size(), "List size is 155");
  }

  @Test
  public void testF() throws Exception {
    // test the delete functionality for items 6 - 156
    // IMPORTANT: delete functionality hasn't been completed yet
    // check the delete methods to see code that is a template but not
    // completed.  leaving as unsupportedOperationException for now
    // as this test should work once those methods are implemented
    PSVisitQuery query = new PSVisitQuery();

    // 25 is the current max allowed limit for the query in the UI
    query.setLimit("25");
    query.setSectionPath("");
    query.setSortOrder("desc");
    query.setTimePeriod("DAY");

    List<String> pagePaths = new ArrayList<String>();
    for (int i = ENTRY_COUNT + 1; i < MAX_COUNT + 1; i++) {
      pagePaths.add(PAGE_FULL + i + ".html");
    }

    try {
      blogPostService.delete(pagePaths);
    } catch (Exception e) {
      assertTrue(e instanceof UnsupportedOperationException, "delete method should be unsupported");
    }
  }

  @Test
  public void testG() {
    // test miscellaneous code for coverage
    PSDbBlogPostVisit bpv = null;

    try {
      bpv = new PSDbBlogPostVisit(null, new Date(), BigInteger.ONE);
    } catch (IllegalArgumentException illegalArgumentException) {
      assertEquals("pagepath cannot be null or empty", illegalArgumentException.getMessage());
    }

    try {
      bpv = new PSDbBlogPostVisit("test", null, BigInteger.ONE);
    } catch (IllegalArgumentException illegalArgumentException) {
      assertEquals("hitDate cannot be null", illegalArgumentException.getMessage());
    }

    try {
      bpv = new PSDbBlogPostVisit("test", new Date(), null);
    } catch (IllegalArgumentException illegalArgumentException) {
      assertEquals("hitCount cannot be null", illegalArgumentException.getMessage());
    }

    bpv = new PSDbBlogPostVisit("test", new Date(), BigInteger.ONE);
    assertEquals("test", bpv.getPagepath(), "should equal test");
    assertTrue(bpv.getHitDate().getTime() <= System.currentTimeMillis());
  }

  /**
   * adds mock blog post visit classes to blog_post_visits table as well as perc_page_metadata table
   */
  private void addEntries() throws Exception {
    Collection<IPSMetadataEntry> ents = new ArrayList<IPSMetadataEntry>();
    PSDbMetadataEntry e = null;

    String fullPath = null;
    String pageName = null;

    for (int i = 0; i < ENTRY_COUNT; i++) {
      pageName = PAGE_NAME + i + ".html";
      // appending the slash as that is used for pagepath
      fullPath = "/" + SITE_NAME + pageName;
      blogPostService.trackBlogPost(fullPath);
      e = createMDEntry(pageName, "/", fullPath, "page", SITE_NAME);
      ents.add(e);
    }

    indexer.save(ents);

    // sleep for 2 seconds, 1 second longer than PSBlogPostVisitService
    // thread executor scheduler
    Thread.sleep(2000);
  }

  /**
   * Adds duplicates to inMemoryMap in blogpostvisitservice to get better coverage and adds
   * different hit rates to pages
   *
   * @param pagePaths the list of pagePaths in DB
   */
  private void mixUpBlogPostHits(List<String> pagePaths) {
    // page1.html tracks -- 3rd most hits
    for (int i = 0; i < 3; i++) {
      blogPostService.trackBlogPost(pagePaths.get(3));
    }
    // page2.html tracks -- 2nd most hits
    for (int i = 0; i < 5; i++) {
      blogPostService.trackBlogPost(pagePaths.get(2));
    }
    // page0.html tracks should now have the most
    for (int i = 0; i < 10; i++) {
      blogPostService.trackBlogPost(pagePaths.get(4));
    }

    // track page3.html once
    blogPostService.trackBlogPost(pagePaths.get(1));
  }

  /** helper method to help test to set up over 50 entries in db to test session.flush and clear */
  private void addManyEntries() throws Exception {
    Collection<IPSMetadataEntry> ents = new ArrayList<IPSMetadataEntry>();
    PSDbMetadataEntry e = null;

    String fullPath = null;
    String pageName = null;

    for (int i = ENTRY_COUNT + 1; i < MAX_COUNT + 1; i++) {
      pageName = PAGE_NAME + i + ".html";
      // appending the slash as that is used for pagepath
      fullPath = "/" + SITE_NAME + pageName;
      blogPostService.trackBlogPost(fullPath);
      e = createMDEntry(pageName, "/", fullPath, "page", SITE_NAME);
      ents.add(e);
    }

    indexer.save(ents);

    // sleep for 2 seconds, 1 second longer than PSBlogPostVisitService
    // thread executor scheduler

    Thread.sleep(2000);
  }

  private PSDbMetadataEntry createMDEntry(
      String name, String folder, String pagepath, String type, String testsite) {
    PSDbMetadataEntry entry = new PSDbMetadataEntry(name, folder, pagepath, type, testsite);
    return entry;
  }
}
