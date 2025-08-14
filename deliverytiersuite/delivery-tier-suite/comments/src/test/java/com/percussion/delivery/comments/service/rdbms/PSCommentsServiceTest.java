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

package com.percussion.delivery.comments.service.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentSort;
import com.percussion.delivery.comments.data.PSCommentSort.SORTBY;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.comments.data.PSPageSummary;
import com.percussion.delivery.comments.data.PSRestComment;
import com.percussion.delivery.comments.services.IPSCommentsService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = PSCommentsServiceTestConfig.class)
public class PSCommentsServiceTest {

  // Logger instance for this test class
  private static final Logger log = LogManager.getLogger(PSCommentsServiceTest.class);

  private static String getMessageForLog(Exception ex) {
    return ex.getMessage();
  }

  private final String COMMENT1_PAGEPATH = "/01_site1/folder/page1.html";
  private final String COMMENT5_PAGEPATH = "/05_site1/folder/subfolder/page.htm";
  private final String COMMENT6_PAGEPATH = "/06_site10/folder/page100.html";
  private final String COMMENT7_PAGEPATH = "/07_site1/folder/page101.html";
  private final String COMMENT8_PAGEPATH = "/08_site1/folder/page102.html";
  private final String COMMENT9_PAGEPATH = "/09_site5/folder/page103.html";
  private final String COMMENT10_PAGEPATH = "/10_site5/folder/page103.html";
  private final String SITE = "the site";

  private final int COMMENT_COUNT_FOR_PERFORMANCE_TESTS = 100;

  @Autowired private IPSCommentsService commentService;

  @Autowired private SessionFactory sessionFactory;

  @BeforeEach
  public void setUp() throws Exception {
    PSComments comments = commentService.getComments(new PSCommentCriteria(), false);
    commentService.deleteComments(getCommnetIds(comments));
  }

  private List<String> getCommnetIds(PSComments comments) {
    List<String> commnetIds = new ArrayList<>();
    List<IPSComment> comm = comments.getComments();
    for (IPSComment cmt : comm) {
      commnetIds.add(cmt.getId());
    }
    return commnetIds;
  }

  @Test
  public void testAddComment() throws Exception {
    /* This test uses a PSComment instance, which has the Hibernate mapping */
    PSComment comment = new PSComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            add("some");
            add("tags");
            add("here");
          }
        });
    comment.setText("text here");
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    commentService.addComment(comment);
    PSComments comms = commentService.getComments(new PSCommentCriteria(), false);
    checkNewCommentValues(comment, comms.getComments());
  }

  @Test
  public void testAddRestComment() throws Exception {
    /* This test tries to save a PSRestComment, which is not a Hibernate valid entity. But
     * the service must be able to save this type of entities, so this tests checks if the
     * SUT (Subject Under Test, the comment service) is able to do it. */
    PSRestComment comment = new PSRestComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            add("some");
            add("tags");
            add("here");
          }
        });
    comment.setText("text here");
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    commentService.addComment(comment);

    PSComments comms = commentService.getComments(new PSCommentCriteria(), false);
    checkNewCommentValues(comment, comms.getComments());
  }

  @Test
  public void testAddComment_CommentTextSizeTest() throws Exception {
    /* This test uses a PSComment instance, which has the Hibernate mapping */
    PSComment comment = new PSComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            add("some");
            add("tags");
            add("here");
          }
        });
    comment.setText(StringUtils.repeat("c", 10000));
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    commentService.addComment(comment);

    PSComments comms = commentService.getComments(new PSCommentCriteria(), false);
    checkNewCommentValues(comment, comms.getComments());
  }

  @Test
  public void testAddComment_IdPresent_ShouldCreateANewOne() throws Exception {
    /* Create a comment and save it. Create another one and set the same id than the
     * previous one, and save it. The comment service should create a new one. */
    PSRestComment comment = new PSRestComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            add("some");
            add("tags");
            add("here");
          }
        });
    comment.setText("text here");
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    // Call addComents twice.
    commentService.addComment(comment);
    // commentService.addComment(comment);

    PSComments comms = commentService.getComments(new PSCommentCriteria(), false);
    checkNewCommentValues(comment, comms.getComments());

    assertEquals(1, comms.getComments().size(), "comment count");
  }

  private void checkNewCommentValues(IPSComment expectedCommentValues, List<IPSComment> comments) {
    assertEquals(1, comments.size(), "comment count");
    assertEquals("comment mail", expectedCommentValues.getEmail(), comments.get(0).getEmail());
    assertEquals(
        expectedCommentValues.getPagePath(), comments.get(0).getPagePath(), "comment page path");
    assertEquals(
        expectedCommentValues.getApprovalState(),
        comments.get(0).getApprovalState(),
        "comment approval state");
    assertEquals(
        expectedCommentValues.isModerated(), comments.get(0).isModerated(), "comment moderated");
    assertEquals(expectedCommentValues.getSite(), comments.get(0).getSite(), "comment site");
    assertNotNull(comments.get(0).getCreatedDate(), "comment created date not null");

    assertEquals(
        expectedCommentValues.getTags().size(),
        comments.get(0).getTags().size(),
        "comment tags count");
    assertTrue(comments.get(0).getTags().contains("some"), "comment tags 1");
    assertTrue(comments.get(0).getTags().contains("tags"), "comment tags 2");
    assertTrue(comments.get(0).getTags().contains("here"), "comment tags 3");

    assertEquals(expectedCommentValues.getText(), comments.get(0).getText(), "comment text");
    assertEquals(expectedCommentValues.getTitle(), comments.get(0).getTitle(), "comment title");
    assertEquals(expectedCommentValues.getUrl(), comments.get(0).getUrl(), "comment url");
    assertEquals(
        expectedCommentValues.getUsername(), comments.get(0).getUsername(), "comment username");
    assertEquals(expectedCommentValues.isViewed(), comments.get(0).isViewed(), "comment viewed");
  }

  @Test
  public void testGetComments_GetByPagepath_TestsCaseInsensitive() throws Exception {
    createSampleComments();

    String expectedPagepath = "/site1/Folder/page1.html";

    PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setPagepath(expectedPagepath.toUpperCase());

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(3, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments())
      assertEquals("comment pagepath", expectedPagepath, com.getPagePath());
  }

  @Test
  public void testGetComments_GetBySite_TestsCaseInsensitive() throws Exception {
    createSampleComments();

    String expectedSite = "the site";

    PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setSite(expectedSite.toUpperCase());

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(15, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments())
      assertEquals("comment site", expectedSite, com.getSite());
  }

  @Test
  public void testGetComments_GetByUsername_TestsCaseInsensitive() throws Exception {
    createSampleComments();

    String expectedUsername = "john";

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setUsername(expectedUsername.toUpperCase());

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments())
      assertEquals("comment username", expectedUsername, com.getUsername());
  }

  @Test
  public void testGetComments_GetByTag_TestsCaseInsensitive() throws Exception {
    createSampleComments();

    String expectedTag = "nosql";

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setTag(expectedTag.toUpperCase());

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(3, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments())
      assertTrue(com.getTags().contains(expectedTag), "comment tag");
  }

  @Test
  public void testGetComments_GetByApprovalState() throws Exception {
    createSampleComments();

    IPSComment.APPROVAL_STATE expectedApprovalState = IPSComment.APPROVAL_STATE.REJECTED;

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setState(expectedApprovalState);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(3, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertNotNull(com.getApprovalState(), "comment approval state not null");
      assertEquals(expectedApprovalState, com.getApprovalState(), "comment approval state value");
    }
  }

  @Test
  public void testGetComments_GetByViewed() throws Exception {
    //
    // viewed = true
    //
    createSampleComments();

    Boolean expectedViewedValue = true;

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setViewed(expectedViewedValue);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(3, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertTrue(com.isViewed(), "comment viewed field");
    }

    //
    // viewed = false
    //
    expectedViewedValue = false;

    criteria = new PSCommentCriteria();
    criteria.setViewed(expectedViewedValue);

    comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(12, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertFalse(com.isViewed(), "comment viewed field");
    }

    //
    // viewed not set
    //
    expectedViewedValue = null;

    criteria = new PSCommentCriteria();
    criteria.setViewed(expectedViewedValue);

    comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(15, comments.getComments().size(), "comments count");
  }

  @Test
  public void testGetComments_GetByModerated() throws Exception {
    //
    // moderated = true
    //
    createSampleComments();

    Boolean expectedModeratedValue = true;

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setModerated(expectedModeratedValue);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertTrue(com.isModerated(), "comment moderated field");
    }

    //
    // moderated = false
    //
    expectedModeratedValue = false;

    criteria = new PSCommentCriteria();
    criteria.setModerated(expectedModeratedValue);

    comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(11, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertFalse(com.isModerated(), "comment moderated field");
    }

    //
    // moderated not set
    //
    expectedModeratedValue = null;

    criteria = new PSCommentCriteria();
    criteria.setModerated(expectedModeratedValue);

    comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(15, comments.getComments().size(), "comments count");
  }

  @Test
  public void testGetComments_GetByLastCommentId() throws Exception {
    // Create some comments, and get the id of one of them
    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              add("general");
              add("agile");
              add("nosql");
              add("databases");
            }
          });
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      commentService.addComment(comment);
    }

    PSComment lastComment = new PSComment();
    lastComment.setPagePath("/site1/folder/page2.html");
    lastComment.setSite("the site");
    lastComment = (PSComment) commentService.addComment(lastComment);

    String lastCommentId = lastComment.getId();

    // Create criteria
    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSite("the site");
    criteria.setLastCommentId(lastCommentId);
    String pagePath = "/site1/folder/page2.html";
    criteria.setPagepath(pagePath.toUpperCase());

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      if (!com.getTags().contains("general"))
        assertTrue(lastCommentId.equals(com.getId()), "comment tag");
    }
  }

  @Test
  public void testGetComments_GetByLastCommentIdAndDifferentSite() throws Exception {
    // Create some comments, and get the id of one of them
    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              add("general");
              add("agile");
              add("nosql");
              add("databases");
            }
          });
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      commentService.addComment(comment);
    }
    // Create a new comment for another site
    PSComment lastComment = new PSComment();
    lastComment.setPagePath("/site1/folder/subfolder/page.htm");
    lastComment.setSite("another site");
    lastComment = (PSComment) commentService.addComment(lastComment);

    String lastCommentId = lastComment.getId();

    // Create criteria
    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSite("the site");
    criteria.setLastCommentId(lastCommentId);
    String pagePath = "/site1/folder/page2.html";
    criteria.setPagepath(pagePath.toUpperCase());

    PSComments comments = commentService.getComments(criteria, false);
    // The size of the list of comments should be 3
    assertNotNull(comments, "comments not null");
    assertEquals(3, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      if (!com.getTags().contains("general"))
        assertTrue(lastCommentId.equals(com.getId()), "comment tag");
    }
  }

  @Disabled("Not ready to run. It generates a query with a join sentence and fails.")
  @Test
  public void testGetComments_GetByLastCommentId_And_Tags() throws Exception {
    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              add("general");
              add("agile");
              add("nosql");
              add("databases");
            }
          });
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      commentService.addComment(comment);
    }

    PSComment lastComment = new PSComment();
    lastComment.setPagePath("/site1/folder/subfolder/page.htm");
    lastComment.setSite("another site");
    lastComment = (PSComment) commentService.addComment(lastComment);

    String lastCommentId = lastComment.getId();

    // Create criteria
    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setTag("general");
    criteria.setLastCommentId(lastCommentId);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      if (!com.getTags().contains("general")) {
        assertTrue(lastCommentId.equals(com.getId()), "comment tag");
      }
    }
  }

  @Test
  public void testGetComments_GetByVariousProperties() throws Exception {
    createSampleComments();

    IPSComment.APPROVAL_STATE expectedApprovalState = IPSComment.APPROVAL_STATE.APPROVED;
    String expectedPagepath = "/site1/folder/subfolder/page.htm";
    String expectedTag = "agile";
    String expectedUsername = "the user";

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setState(expectedApprovalState);
    criteria.setPagepath(expectedPagepath);
    criteria.setTag(expectedTag);
    criteria.setUsername(expectedUsername);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(2, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertNotNull(com.getApprovalState(), "comment approval state not null");
      assertEquals(expectedApprovalState, com.getApprovalState(), "comment approval state value");
      assertEquals("comment pagepath", expectedPagepath, com.getPagePath());
      assertEquals("comment username", expectedUsername, com.getUsername());
      assertTrue(com.getTags().contains(expectedTag), "comment tag");
    }
  }

  @Test
  public void testGetComments_ModeratorFlagIsFalse_ShouldNotModifyReturnedComments()
      throws Exception {
    // Create some comments
    for (int i = 0; i < 7; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      comment.setViewed(false);
      commentService.addComment(comment);
    }

    String expectedSite = "the site";

    PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setSite(expectedSite);

    PSComments comments = commentService.getComments(criteria, false);

    // After calling getComments, and as isModerator flag is false, all returned
    // comments should not be modified.

    comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(7, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertEquals("comment site", expectedSite, com.getSite());

      // 'viewed' flag not modified.
      assertFalse(com.isViewed(), "comment viewed flag");
    }
  }

  @Test
  public void testGetComments_ModeratorFlagIsTrue_ReturnedCommentsGetTheirViewedFlagToTrue()
      throws Exception {
    // Create some comments
    for (int i = 0; i < 7; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      comment.setViewed(false);
      // comment.setId(String.valueOf(i));
      commentService.addComment(comment);
    }

    String expectedSite = "the site";

    PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setSite(expectedSite);

    PSComments comments = commentService.getComments(criteria, false);

    // After calling getComments, all returned comments have their 'viewed'
    // flag set to true

    comments = commentService.getComments(criteria, true);

    assertNotNull(comments, "comments not null");
    assertEquals(7, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertEquals("comment site", expectedSite, com.getSite());
      assertTrue(com.isViewed(), "comment viewed flag");
    }
  }

  @Test
  public void testGetComments_DefaultSortByCreatedDateDescending() throws Exception {
    // Create two comments with different created date properties
    PSComment comment = new PSComment();
    comment.setPagePath("/site1/folder/page1.html");
    comment.setSite("the site");
    comment.setText("some thing");
    commentService.addComment(comment);

    Thread.sleep(1000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    comment.setText("some other thing");
    commentService.addComment(comment);

    Thread.sleep(1000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page3.html");
    comment.setSite("theSite");
    comment.setText("some other thing 3");
    commentService.addComment(comment);

    // Create an empty criteria
    PSCommentCriteria criteria = new PSCommentCriteria();

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(3, comments.getComments().size(), "comments count");

    // Create a fake comment to make the comparison easier
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, cal.getActualMaximum(Calendar.YEAR));

    comment = new PSComment();
    comment.setCreatedDate(cal.getTime());

    IPSComment previousComment = comment;

    for (IPSComment com : comments.getComments()) {
      // Make sure the comments are ascending sorted
      assertTrue(
          previousComment.getCreatedDate().compareTo(com.getCreatedDate()) > 0,
          "comment created order");
      previousComment = com;
    }
  }

  @Test
  public void testGetComments_SortByCreatedDate_Ascending() throws Exception {
    // Create two comments with different created date properties
    PSComment comment = new PSComment();
    comment.setPagePath("/site1/folder/page1.html");
    comment.setSite("theSite");
    commentService.addComment(comment);

    Thread.sleep(2000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    commentService.addComment(comment);

    // Create the criteria
    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSort(new PSCommentSort(SORTBY.CREATEDDATE, true));

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(2, comments.getComments().size(), "comments count");

    // Create a fake comment to make the comparison easier
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, cal.getActualMinimum(Calendar.YEAR));

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    comment.setCreatedDate(cal.getTime());

    IPSComment previousComment = comment;

    for (IPSComment com : comments.getComments()) {
      assertTrue(
          previousComment.getCreatedDate().compareTo(com.getCreatedDate()) < 0,
          "comment created order");
      previousComment = com;
    }
  }

  @Test
  public void testGetComments_SortByCreatedDate_Descending() throws Exception {
    // Create two comments with different created date properties
    PSComment comment = new PSComment();
    comment.setPagePath("/site1/folder/page1.html");
    comment.setSite("theSite");
    commentService.addComment(comment);

    Thread.sleep(2000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    commentService.addComment(comment);

    // Create the criteria
    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSort(new PSCommentSort(SORTBY.CREATEDDATE, false));

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(2, comments.getComments().size(), "comments count");

    // Create a fake comment to make the comparison easier
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, cal.getActualMaximum(Calendar.YEAR));

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    comment.setCreatedDate(cal.getTime());

    IPSComment previousComment = comment;

    for (IPSComment com : comments.getComments()) {
      assertTrue(
          previousComment.getCreatedDate().compareTo(com.getCreatedDate()) > 0,
          "comment created order");
      previousComment = com;
    }
  }

  @Test
  public void testGetComments_MaxResults() throws Exception {
    createSampleComments();

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setMaxResults(5);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(5, comments.getComments().size(), "comments count");
  }

  @Test
  public void testGetComments_StartIndex() throws Exception {
    String user1 = "john";
    String user2 = "adam";

    // Create users
    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setUsername(user1);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("theSite");
      commentService.addComment(comment);
    }

    for (int i = 0; i < 6; i++) {
      PSComment comment = new PSComment();
      comment.setUsername(user2);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("theSite");
      commentService.addComment(comment);
    }

    // Create criteria
    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSort(new PSCommentSort(SORTBY.USERNAME, true));
    criteria.setStartIndex(6);

    PSComments comments = commentService.getComments(criteria, false);

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.getComments().size(), "comments count");

    for (IPSComment com : comments.getComments()) {
      assertEquals(user1, com.getUsername(), "comment user");
    }
  }

  @Test
  public void testGetPagesWithComments_SiteNotSpecified() throws Exception {
    // null site
    try {
      commentService.getPagesWithComments(null, 10, 0);
      assertTrue(false, "not exception thrown");
    } catch (IllegalArgumentException ex) {
      log.error(ex.getMessage());
      log.debug(ex);
    }

    // empty site
    try {
      PSPageSummaries pageSummaries = commentService.getPagesWithComments("", 10, 0);
      assertTrue(false, "not exception thrown");
    } catch (IllegalArgumentException ex) {
      log.error(ex.getMessage());
      log.debug(ex);
    }
  }

  @Test
  public void testGetPagesWithComments_BasicQuery() throws Exception {
    // Create comments

    String pagepath1 = "/Site1/folder/page1.html";
    String site1 = "the site";
    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath1);
      comment.setSite(site1);
      commentService.addComment(comment);
    }

    String pagepath2 = "/site1/folder/Page2.html";
    for (int i = 0; i < 2; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath2);
      comment.setSite(site1);

      if (i == 1) {
        commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
      commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.APPROVED);
    }

    String pagepath3 = "/site1/FOLDER/subfolder/page.htm";
    for (int i = 0; i < 5; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath3);
      comment.setUsername("the user");
      comment.setSite(site1);
      Set<String> tags = new HashSet<>();
      tags.add("agile");
      tags.add("cars");
      comment.setTags(tags);

      if (i % 2 == 0) {
        commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
      commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/page1.html");
      comment.setSite("another site");
      commentService.addComment(comment);
    }

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(site1, 0, 0);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(3, pageSummaries.getSummaries().size(), "comments count");

    for (PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(pagepath1)) {
        assertEquals(3, ps.getCommentCount(), "pagepath 1 - comment count");
        assertEquals(3, ps.getApprovedCount(), "pagepath 1 - approved comment count");
      } else if (ps.getPagePath().equals(pagepath2)) {
        assertEquals(2, ps.getCommentCount(), "pagepath 2 - comment count");
        assertEquals(1, ps.getApprovedCount(), "pagepath 2 - approved comment count");
      } else if (ps.getPagePath().equals(pagepath3)) {
        assertEquals(5, (int) ps.getCommentCount(), "pagepath 3 - comment count");
        assertEquals(2, (int) ps.getApprovedCount(), "pagepath 3 - approved comment count");
      } else {
        assertTrue(false, "wrong pagepath");
      }
    }
  }

  @Test
  public void testGetPagesWithComments_PagepathDiffersOnlyByCase() throws Exception {
    // Create comments

    String pagepath1 = "/site1/folder/page1.html";
    String site1 = "the site";
    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath1);
      comment.setSite(site1);
      commentService.addComment(comment);
    }

    String pagepath2 = "/site1/FOLDER/page1.html"; // Equals to pagepath1, only differs in case
    for (int i = 0; i < 2; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath2);
      comment.setSite(site1);

      if (i == 1) {
        commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
      commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.APPROVED);
    }

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(site1, 0, 0);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(1, pageSummaries.getSummaries().size(), "comments count");

    for (PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(pagepath1) || ps.getPagePath().equals(pagepath2)) {
        assertEquals(5, (int) ps.getCommentCount(), "pagepath 1 - comment count");
        assertEquals(4, (int) ps.getApprovedCount(), "pagepath 1 - approved comment count");
      } else {
        assertTrue(false, "wrong pagepath");
      }
    }
  }

  @Test
  public void testGetPagesWithComments_Paging_MaxReturn_And_StartIndex_EqualsToZero()
      throws Exception {
    // Create comments
    createSampleCommentsForPagingTests();

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(SITE, 0, 0);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(10, pageSummaries.getSummaries().size(), "comments count");

    // Get page summaries
    pageSummaries = commentService.getPagesWithComments(SITE, -1, -1);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(10, pageSummaries.getSummaries().size(), "comments count");
  }

  @Test
  public void testGetPagesWithComments_Paging_WithNoComments() throws Exception {
    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(SITE, 3, 2);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(0, pageSummaries.getSummaries().size(), "comments count");
  }

  @Test
  public void testGetPagesWithComments_Paging_FirstPage_WithMaxResults() throws Exception {
    // Create comments
    createSampleCommentsForPagingTests();

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(SITE, 3, 0);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(3, pageSummaries.getSummaries().size(), "comments count");

    PSPageSummary ps1 = pageSummaries.getSummaries().get(0);
    PSPageSummary ps2 = pageSummaries.getSummaries().get(1);
    PSPageSummary ps3 = pageSummaries.getSummaries().get(2);
    assertFalse(
        ps1.getPagePath().equals(ps2.getPagePath()),
        "summaries must point to different pagepaths - 1");
    assertFalse(
        ps1.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 2");
    assertFalse(
        ps2.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 3");

    for (PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(COMMENT1_PAGEPATH)) {
        assertEquals(3, (int) ps.getCommentCount(), "comment count - 1");
        assertEquals(3, (int) ps.getApprovedCount(), "approved comment count - 1");
      } else {
        assertTrue(false, "wrong pagepath: " + ps.getPagePath());
      }
    }
  }

  @Test
  public void testGetPagesWithComments_Paging_SecondPage_WithMaxResults() throws Exception {
    // Create comments
    createSampleCommentsForPagingTests();

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(SITE, 3, 1);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(3, pageSummaries.getSummaries().size(), "comments count");

    PSPageSummary ps1 = pageSummaries.getSummaries().get(0);
    PSPageSummary ps2 = pageSummaries.getSummaries().get(1);
    PSPageSummary ps3 = pageSummaries.getSummaries().get(2);
    assertFalse(
        ps1.getPagePath().equals(ps2.getPagePath()),
        "summaries must point to different pagepaths - 1");
    assertFalse(
        ps1.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 2");
    assertFalse(
        ps2.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 3");

    for (PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(COMMENT5_PAGEPATH)) {
        assertEquals(5, (int) ps.getCommentCount(), "comment count - 2");
        assertEquals(2, (int) ps.getApprovedCount(), "approved comment count - 2");
      } else if (ps.getPagePath().equals(COMMENT6_PAGEPATH)) {
        assertEquals(3, (int) ps.getCommentCount(), "comment count - 3");
        assertEquals(2, (int) ps.getApprovedCount(), "approved comment count - 3");
      } else {
        assertTrue(false, "wrong pagepath: " + ps.getPagePath());
      }
    }
  }

  @Test
  public void testGetPagesWithComments_Paging_LastPage_CommentsReturned_LessThan_MaxResults()
      throws Exception {
    // Create comments
    createSampleCommentsForPagingTests();

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(SITE, 3, 3);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(1, pageSummaries.getSummaries().size(), "comments count");

    for (PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(COMMENT10_PAGEPATH)) {
        assertEquals(4, (int) ps.getCommentCount(), "comment count - 1");
        assertEquals(4, (int) ps.getApprovedCount(), "approved comment count - 1");
      } else {
        assertTrue(false, "wrong pagepath: " + ps.getPagePath());
      }
    }
  }

  @Test
  public void testApproveComment() throws Exception {
    List<String> commentsIdToApprove = new ArrayList<String>();

    commentService.setDefaultModerationState("theSite", IPSComment.APPROVAL_STATE.REJECTED);
    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page1.html" + i);
      comment.setSite("theSite");
      comment = (PSComment) commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToApprove.add(comment.getId());
    }
    commentService.setDefaultModerationState("theSite", IPSComment.APPROVAL_STATE.APPROVED);
    commentService.approveComments(commentsIdToApprove);

    List<IPSComment> comments =
        commentService.getComments(new PSCommentCriteria(), false).getComments();

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.size(), "comment count");

    int approvedComments = 0;
    int rejectedComments = 0;

    for (IPSComment com : comments) {
      if (commentsIdToApprove.contains(com.getId())) {
        assertEquals(
            IPSComment.APPROVAL_STATE.APPROVED,
            com.getApprovalState(),
            "comment should be approved");
        approvedComments++;
      } else {
        assertEquals(
            IPSComment.APPROVAL_STATE.REJECTED,
            com.getApprovalState(),
            "comment should be rejected");
        rejectedComments++;
      }
    }

    assertEquals(2, approvedComments, "comments approved count");
    assertEquals(2, rejectedComments, "comments rejected count");
  }

  @Test
  public void testApproveComment_CommentListIsNull() throws Exception {
    try {
      commentService.approveComments(null);
      fail("null argument should throw an exception");
    } catch (IllegalArgumentException ex) {
      log.error(ex.getMessage());
      log.debug(ex);
    }
  }

  @Test
  public void testApproveComment_CommentListIsEmpty() throws Exception {
    // If the list is empty, the method should quit silently.
    commentService.approveComments(new ArrayList<String>());
  }

  @Test
  public void testRejectComment() throws Exception {
    List<String> commentsIdToReject = new ArrayList<String>();

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page1.html" + i);
      comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
      comment.setSite("theSite");
      comment = (PSComment) commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToReject.add(comment.getId());
    }

    commentService.rejectComments(commentsIdToReject);
    PSCommentCriteria cc = new PSCommentCriteria();
    List<IPSComment> comments = commentService.getComments(cc, false).getComments();

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.size(), "comment count");

    int approvedComments = 0;
    int rejectedComments = 0;

    for (IPSComment com : comments) {
      if (commentsIdToReject.contains(com.getId())) {
        assertEquals(
            IPSComment.APPROVAL_STATE.REJECTED,
            com.getApprovalState(),
            "comment should be rejected");
        rejectedComments++;
      } else {
        assertEquals(
            IPSComment.APPROVAL_STATE.APPROVED,
            com.getApprovalState(),
            "comment should be approved");
        approvedComments++;
      }
    }

    assertEquals(2, approvedComments, "comments approved count");
    assertEquals(2, rejectedComments, "comments rejected count");
  }

  @Test
  public void testRejectComment_CommentListIsNull() throws Exception {
    try {
      commentService.rejectComments(null);
      fail("null argument should throw an exception");
    } catch (IllegalArgumentException ex) {
      log.error(ex.getMessage());
      log.debug(ex);
    }
  }

  @Test
  public void testRejectComment_CommentListIsEmpty() throws Exception {
    // If the list is empty, the method should quit silently.
    commentService.rejectComments(new ArrayList<String>());
  }

  @Test
  public void testDefaultModerationState() throws Exception {
    final String SITE1 = "site1";
    final String SITE2 = "site2";
    final String SITE3 = "site3";
    commentService.setDefaultModerationState(SITE1, IPSComment.APPROVAL_STATE.REJECTED);
    commentService.setDefaultModerationState(SITE2, IPSComment.APPROVAL_STATE.APPROVED);
    commentService.setDefaultModerationState(SITE3, IPSComment.APPROVAL_STATE.REJECTED);

    IPSComment.APPROVAL_STATE state1 = commentService.getDefaultModerationState(SITE1);
    IPSComment.APPROVAL_STATE state2 = commentService.getDefaultModerationState(SITE2);
    IPSComment.APPROVAL_STATE state3 = commentService.getDefaultModerationState(SITE3);

    assertEquals(IPSComment.APPROVAL_STATE.REJECTED, state1);
    assertEquals(IPSComment.APPROVAL_STATE.APPROVED, state2);
    assertEquals(IPSComment.APPROVAL_STATE.REJECTED, state3);

    assertEquals(
        IPSComment.APPROVAL_STATE.APPROVED, commentService.getDefaultModerationState("UNKNOWN"));
  }

  @Test
  @Disabled("Enable just for performance testing purposes")
  public void testGetPagesWithComments_Performance() throws Exception {
    log.info("Adding comments");
    for (int i = 0; i < COMMENT_COUNT_FOR_PERFORMANCE_TESTS; i++)
      createSampleCommentsForPagingTests(Integer.toString(i));

    // Disable second-level Hibernate cache. Close current session to flush
    // first-level cache.
    // session.close();

    sessionFactory.getCache().evictAll();
    sessionFactory.getCache().evictQueryRegions();

    // Get page summaries for various pages
    for (int i = 0; i < 3; i++) {
      log.info("Getting pages with comments");
      Calendar before = Calendar.getInstance();
      PSPageSummaries pageSummariesPage0 = commentService.getPagesWithComments(SITE + "0", 3, i);
      Calendar after = Calendar.getInstance();

      assertEquals(3, pageSummariesPage0.getSummaries().size(), "page summaries count");

      log.info(
          "Page {} - Query took: {} milliseconds",
          i,
          (after.getTimeInMillis() - before.getTimeInMillis()));
    }
  }

  @Test
  public void testDeleteComments() throws Exception {
    List<String> commentsIdToDelete = new ArrayList<String>();

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page" + i + ".html");
      comment.setSite("theSite");
      comment = (PSComment) commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToDelete.add(comment.getId());
    }

    List<IPSComment> comments =
        commentService.getComments(new PSCommentCriteria(), false).getComments();

    assertNotNull(comments, "comments not null");
    assertEquals(4, comments.size(), "comment count");

    // Delete comments
    commentService.deleteComments(commentsIdToDelete);

    comments = commentService.getComments(new PSCommentCriteria(), false).getComments();

    assertEquals(2, comments.size(), "comments count");

    for (IPSComment com : comments) {
      assertTrue(
          !commentsIdToDelete.contains(com.getId()), "current comment is not the deleted one");
    }
  }

  @Test
  public void testDeleteComments_CommentsWithTags() throws Exception {
    List<String> commentsIdToDelete = new ArrayList<String>();

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page" + i + ".html");
      comment.setSite("theSite");
      comment.setTags(
          new HashSet<String>() {
            {
              add("agile");
              add("cars");
            }
          });

      comment = (PSComment) commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToDelete.add(comment.getId());
    }

    List<IPSComment> comments =
        commentService.getComments(new PSCommentCriteria(), false).getComments();
    int countTags = 0;
    for (IPSComment comm : comments) {
      Set tags = comm.getTags();
      countTags = countTags + tags.size();
    }

    // Make sure there are comment tags in database
    assertEquals(8, countTags, "comment tags count before deleting");

    // reCreateSession();

    // Delete all added comments
    commentService.deleteComments(commentsIdToDelete);

    List<IPSComment> comments2 =
        commentService.getComments(new PSCommentCriteria(), false).getComments();
    assertEquals(2, comments2.size(), "comments count");

    countTags = 0;
    for (IPSComment comm : comments2) {
      Set tags = comm.getTags();
      countTags = countTags + tags.size();
    }
    // Correct count of tags
    assertEquals(4, countTags, "comment tags count after deleting");
  }

  @Test
  public void testDeleteComments_EmptyIdList() throws Exception {
    commentService.deleteComments(new ArrayList<String>());
  }

  @Test
  public void testDeleteComments_NullIdList() throws Exception {
    try {
      commentService.deleteComments(null);
      fail("It has to throw an exception");
    } catch (IllegalArgumentException ex) {
      log.error(ex.getMessage());
      log.debug(ex);
    }
  }

  @Test
  public void testGetNewComments() throws Exception {
    // Create comments
    String pagepath1 = "/Site1/folder/page1.html";
    String site1 = "the site";
    // in pagepath1: newComment = 5
    for (int i = 0; i < 8; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath1);
      comment.setSite(site1);
      comment.setViewed(true);
      comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
      if (i > 2) {
        comment.setViewed(false);
      }
      if (i > 4) {
        comment.setApprovalState(IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
    }

    String pagepath2 = "/Site1/folder/page2.html";
    // in pagepath2: newComment = 2
    for (int i = 0; i < 5; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(pagepath2);
      comment.setSite(site1);
      comment.setViewed(true);
      comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
      if (i > 2) {
        comment.setViewed(false);
      }
      if (i > 3) {
        comment.setApprovalState(IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
    }

    // Get page summaries
    PSPageSummaries pageSummaries = commentService.getPagesWithComments(site1, 10, 0);

    assertNotNull(pageSummaries, "comments not null");
    assertEquals(2, pageSummaries.getSummaries().size(), "comments count");

    for (PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(pagepath1)) {
        assertEquals(5, (int) ps.getNewCommentCount(), "pagepath 1 - new comment");
        assertEquals(8, ps.getCommentCount(), "pagepath 1 - new comment");
      } else if (ps.getPagePath().equals(pagepath2)) {
        assertEquals(2, ps.getNewCommentCount(), "pagepath 2 - new comment");
        assertEquals(5, ps.getCommentCount(), "pagepath 1 - new comment");
      } else {
        assertTrue(false, "wrong pagepath");
      }
    }
  }

  private void createSampleCommentsForPagingTests() throws Exception {
    createSampleCommentsForPagingTests(StringUtils.EMPTY);
  }

  private void createSampleCommentsForPagingTests(String pagepathSuffix) throws Exception {
    // Creates 18 comments, generates 10 page summaries.

    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT1_PAGEPATH + pagepathSuffix);
      comment.setSite(SITE + pagepathSuffix);
      commentService.addComment(comment);
    }

    for (int i = 0; i < 5; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT5_PAGEPATH + pagepathSuffix);
      comment.setUsername("the user");
      comment.setSite(SITE + pagepathSuffix);
      comment.setTags(
          new HashSet<String>() {
            {
              add("agile");
              add("cars");
            }
          });

      if (i % 2 == 0) {
        commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
      commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT6_PAGEPATH + pagepathSuffix);
      comment.setSite(SITE + pagepathSuffix);

      if (i == 1) {
        commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.REJECTED);
      }

      commentService.addComment(comment);
      commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 2; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT7_PAGEPATH + pagepathSuffix);
      comment.setSite(SITE + pagepathSuffix);
      comment.setTags(
          new HashSet<String>() {
            {
              add("agile");
              add("cars");
            }
          });

      if (i == 1) {
        commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.REJECTED);
      }
      commentService.addComment(comment);
      commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT8_PAGEPATH);
      comment.setSite(SITE);
      commentService.addComment(comment);
    }

    for (int i = 0; i < 7; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT9_PAGEPATH + pagepathSuffix);
      comment.setSite(SITE + pagepathSuffix);
      comment.setTags(
          new HashSet<String>() {
            {
              add("agile");
              add("cars");
            }
          });

      commentService.addComment(comment);
    }

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath(COMMENT10_PAGEPATH + pagepathSuffix);
      comment.setSite(SITE + pagepathSuffix);
      commentService.addComment(comment);
    }
  }

  private void createSampleComments() throws Exception {
    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page1.html");
      comment.setSite("the site");
      commentService.addComment(comment);
    }

    for (int i = 0; i < 2; i++) {
      PSComment comment2 = new PSComment();
      comment2.setPagePath("/site1/folder/page2.html");
      comment2.setSite("the site");
      commentService.addComment(comment2);
    }

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setUsername("john");
      comment.setModerated(true);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      commentService.addComment(comment);
    }

    for (int i = 0; i < 1; i++) {
      PSComment comment = new PSComment();
      comment.setUsername("adam");
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      commentService.addComment(comment);
    }

    for (int i = 0; i < 3; i++) {
      PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              add("general");
              add("agile");
              add("nosql");
              add("databases");
            }
          });
      commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.REJECTED);
      comment.setViewed(true);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      commentService.addComment(comment);
      commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 2; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/subfolder/page.htm");
      comment.setUsername("the user");
      comment.setSite("the site");
      comment.setTags(
          new HashSet<String>() {
            {
              add("agile");
              add("cars");
            }
          });
      commentService.addComment(comment);
    }
  }
}
