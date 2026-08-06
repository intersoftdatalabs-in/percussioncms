/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentSort;
import com.percussion.delivery.comments.data.PSCommentSort.SORTBY;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.comments.data.PSPageSummary;
import com.percussion.delivery.comments.data.PSRestComment;
import com.percussion.delivery.comments.service.rdbms.PSComment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(locations = "classpath:/test-beans.xml")
@ExtendWith(SpringExtension.class)
public class PSCommentsServiceTest {

  // Logger instance for this test class
  private static final Logger log = LogManager.getLogger(PSCommentsServiceTest.class);

  private static String getMessageForLog(final Exception ex) {
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
    final PSComments comments = this.commentService.getComments(new PSCommentCriteria(), false);
    this.commentService.deleteComments(this.getCommentIds(comments));
  }

  private List<String> getCommentIds(final PSComments comments) {
    final List<String> commentIds = new ArrayList<>();
    final List<IPSComment> comm = comments.getComments();
    for (final IPSComment cmt : comm) {
      commentIds.add(cmt.getId());
    }
    return commentIds;
  }

  @Test
  public void testAddComment() throws Exception {
    /* This test uses a PSComment instance, which has the Hibernate mapping */
    final PSComment comment = new PSComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            this.add("some");
            this.add("tags");
            this.add("here");
          }
        });
    comment.setText("text here");
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    this.commentService.addComment(comment);
    final PSComments comms = this.commentService.getComments(new PSCommentCriteria(), false);
    this.checkNewCommentValues(comment, comms.getComments());
  }

  @Test
  public void testAddRestComment() throws Exception {
    /* This test tries to save a PSRestComment, which is not a Hibernate valid entity. But
     * the service must be able to save this type of entities, so this tests checks if the
     * SUT (Subject Under Test, the comment service) is able to do it. */
    final PSRestComment comment = new PSRestComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            this.add("some");
            this.add("tags");
            this.add("here");
          }
        });
    comment.setText("text here");
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    this.commentService.addComment(comment);

    final PSComments comms = this.commentService.getComments(new PSCommentCriteria(), false);
    this.checkNewCommentValues(comment, comms.getComments());
  }

  @Test
  public void testAddComment_CommentTextSizeTest() throws Exception {
    /* This test uses a PSComment instance, which has the Hibernate mapping */
    final PSComment comment = new PSComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            this.add("some");
            this.add("tags");
            this.add("here");
          }
        });
    comment.setText(StringUtils.repeat("c", 10000));
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    this.commentService.addComment(comment);

    final PSComments comms = this.commentService.getComments(new PSCommentCriteria(), false);
    this.checkNewCommentValues(comment, comms.getComments());
  }

  @Test
  public void testAddComment_IdPresent_ShouldCreateANewOne() throws Exception {
    /* Create a comment and save it. Create another one and set the same id than the
     * previous one, and save it. The comment service should create a new one. */
    final PSRestComment comment = new PSRestComment();
    comment.setEmail("email@domain.com");
    comment.setPagePath("this/is/the/pagePath.html");
    comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
    comment.setModerated(false);
    comment.setSite("site");
    comment.setTags(
        new HashSet<String>() {
          {
            this.add("some");
            this.add("tags");
            this.add("here");
          }
        });
    comment.setText("text here");
    comment.setTitle("title");
    comment.setUrl("http://url.com");
    comment.setUsername("user name");
    comment.setViewed(true);

    // Call addComents twice.
    this.commentService.addComment(comment);
    // commentService.addComment(comment);

    final PSComments comms = this.commentService.getComments(new PSCommentCriteria(), false);
    this.checkNewCommentValues(comment, comms.getComments());

    Assertions.assertEquals(1, comms.getComments().size(), "comment count");
  }

  private void checkNewCommentValues(
      final IPSComment expectedCommentValues, final List<IPSComment> comments) {
    Assertions.assertEquals(1, comments.size(), "comment count");
    Assertions.assertEquals(
        expectedCommentValues.getEmail(), comments.get(0).getEmail(), "comment mail");
    Assertions.assertEquals(
        expectedCommentValues.getPagePath(), comments.get(0).getPagePath(), "comment page path");
    Assertions.assertEquals(
        expectedCommentValues.getApprovalState(),
        comments.get(0).getApprovalState(),
        "comment approval state");
    Assertions.assertEquals(
        expectedCommentValues.isModerated(), comments.get(0).isModerated(), "comment moderated");
    Assertions.assertEquals(
        expectedCommentValues.getSite(), comments.get(0).getSite(), "comment site");
    Assertions.assertNotNull(comments.get(0).getCreatedDate(), "comment created date not null");

    Assertions.assertEquals(
        expectedCommentValues.getTags().size(),
        comments.get(0).getTags().size(),
        "comment tags count");
    Assertions.assertTrue(comments.get(0).getTags().contains("some"), "comment tags 1");
    Assertions.assertTrue(comments.get(0).getTags().contains("tags"), "comment tags 2");
    Assertions.assertTrue(comments.get(0).getTags().contains("here"), "comment tags 3");

    Assertions.assertEquals(
        expectedCommentValues.getText(), comments.get(0).getText(), "comment text");
    Assertions.assertEquals(
        expectedCommentValues.getTitle(), comments.get(0).getTitle(), "comment title");
    Assertions.assertEquals(
        expectedCommentValues.getUrl(), comments.get(0).getUrl(), "comment url");
    Assertions.assertEquals(
        expectedCommentValues.getUsername(), comments.get(0).getUsername(), "comment username");
    Assertions.assertEquals(
        expectedCommentValues.isViewed(), comments.get(0).isViewed(), "comment viewed");
  }

  @Test
  public void testGetComments_GetByPagepath_TestsCaseInsensitive() throws Exception {
    this.createSampleComments();

    final String expectedPagepath = "/site1/Folder/page1.html";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setPagepath(expectedPagepath.toUpperCase());

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments())
      Assertions.assertEquals(expectedPagepath, com.getPagePath(), "comment pagepath");
  }

  @Test
  public void testGetComments_GetBySite_TestsCaseInsensitive() throws Exception {
    this.createSampleComments();

    final String expectedSite = "the site";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setSite(expectedSite.toUpperCase());

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(15, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments())
      Assertions.assertEquals(expectedSite, com.getSite(), "comment site");
  }

  @Test
  public void testGetComments_GetByUsername_TestsCaseInsensitive() throws Exception {
    this.createSampleComments();

    final String expectedUsername = "john";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setUsername(expectedUsername.toUpperCase());

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments())
      Assertions.assertEquals(expectedUsername, com.getUsername(), "comment username");
  }

  @Test
  public void testGetComments_GetByTag_TestsCaseInsensitive() throws Exception {
    this.createSampleComments();

    final String expectedTag = "nosql";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setTag(expectedTag.toUpperCase());

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments())
      Assertions.assertTrue(com.getTags().contains(expectedTag), "comment tag");
  }

  @Test
  public void testGetComments_GetByApprovalState() throws Exception {
    this.createSampleComments();

    final IPSComment.APPROVAL_STATE expectedApprovalState = IPSComment.APPROVAL_STATE.REJECTED;

    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setState(expectedApprovalState);

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertNotNull(com.getApprovalState(), "comment approval state not null");
      Assertions.assertEquals(
          expectedApprovalState, com.getApprovalState(), "comment approval state value");
    }
  }

  @Test
  public void testGetComments_GetByViewed() throws Exception {
    //
    // viewed = true
    //
    this.createSampleComments();

    Boolean expectedViewedValue = true;

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setViewed(expectedViewedValue);

    PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertTrue(com.isViewed(), "comment viewed field");
    }

    //
    // viewed = false
    //
    expectedViewedValue = false;

    criteria = new PSCommentCriteria();
    criteria.setViewed(expectedViewedValue);

    comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(12, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertFalse(com.isViewed(), "comment viewed field");
    }

    //
    // viewed not set
    //
    expectedViewedValue = null;

    criteria = new PSCommentCriteria();
    criteria.setViewed(expectedViewedValue);

    comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(15, comments.getComments().size(), "comments count");
  }

  @Test
  public void testGetComments_GetByModerated() throws Exception {
    //
    // moderated = true
    //
    this.createSampleComments();

    Boolean expectedModeratedValue = true;

    PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setModerated(expectedModeratedValue);

    PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertTrue(com.isModerated(), "comment moderated field");
    }

    //
    // moderated = false
    //
    expectedModeratedValue = false;

    criteria = new PSCommentCriteria();
    criteria.setModerated(expectedModeratedValue);

    comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(11, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertFalse(com.isModerated(), "comment moderated field");
    }

    //
    // moderated not set
    //
    expectedModeratedValue = null;

    criteria = new PSCommentCriteria();
    criteria.setModerated(expectedModeratedValue);

    comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(15, comments.getComments().size(), "comments count");
  }

  @Test
  public void testGetComments_GetByLastCommentId() throws Exception {
    // Create some comments, and get the id of one of them
    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              this.add("general");
              this.add("agile");
              this.add("nosql");
              this.add("databases");
            }
          });
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
    }

    PSComment lastComment = new PSComment();
    lastComment.setPagePath("/site1/folder/page2.html");
    lastComment.setSite("the site");
    lastComment = (PSComment) this.commentService.addComment(lastComment);

    final String lastCommentId = lastComment.getId();

    // Create criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSite("the site");
    criteria.setLastCommentId(lastCommentId);
    final String pagePath = "/site1/folder/page2.html";
    criteria.setPagepath(pagePath.toUpperCase());

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      if (!com.getTags().contains("general")) {
        Assertions.assertTrue(lastCommentId.equals(com.getId()), "comment tag");
      }
    }
  }

  @Test
  public void testGetComments_GetByLastCommentIdAndDifferentSite() throws Exception {
    // Create some comments, and get the id of one of them
    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              this.add("general");
              this.add("agile");
              this.add("nosql");
              this.add("databases");
            }
          });
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
    }
    // Create a new comment for another site
    PSComment lastComment = new PSComment();
    lastComment.setPagePath("/site1/folder/subfolder/page.htm");
    lastComment.setSite("another site");
    lastComment = (PSComment) this.commentService.addComment(lastComment);

    final String lastCommentId = lastComment.getId();

    // Create criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSite("the site");
    criteria.setLastCommentId(lastCommentId);
    final String pagePath = "/site1/folder/page2.html";
    criteria.setPagepath(pagePath.toUpperCase());

    final PSComments comments = this.commentService.getComments(criteria, false);
    // The size of the list of comments should be 3
    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      if (!com.getTags().contains("general"))
        Assertions.assertTrue(lastCommentId.equals(com.getId()), "comment tag");
    }
  }

  @Test
  public void testGetComments_GetByLastCommentId_And_Tags() throws Exception {
    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              this.add("general");
              this.add("agile");
              this.add("nosql");
              this.add("databases");
            }
          });
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
    }

    PSComment lastComment = new PSComment();
    lastComment.setPagePath("/site1/folder/subfolder/page.htm");
    lastComment.setSite("another site");
    lastComment = (PSComment) this.commentService.addComment(lastComment);

    final String lastCommentId = lastComment.getId();

    // Create criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setTag("general");
    criteria.setLastCommentId(lastCommentId);

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertTrue(com.getTags().contains("general"), "comment tag");
    }
  }

  @Test
  public void testGetComments_GetByVariousProperties() throws Exception {
    this.createSampleComments();

    final IPSComment.APPROVAL_STATE expectedApprovalState = IPSComment.APPROVAL_STATE.APPROVED;
    final String expectedPagepath = "/site1/folder/subfolder/page.htm";
    final String expectedTag = "agile";
    final String expectedUsername = "the user";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setState(expectedApprovalState);
    criteria.setPagepath(expectedPagepath);
    criteria.setTag(expectedTag);
    criteria.setUsername(expectedUsername);

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(2, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertNotNull(com.getApprovalState(), "comment approval state not null");
      Assertions.assertEquals(
          expectedApprovalState, com.getApprovalState(), "comment approval state value");
      Assertions.assertEquals(expectedPagepath, com.getPagePath(), "comment pagepath");
      Assertions.assertEquals(expectedUsername, com.getUsername(), "comment username");
      Assertions.assertTrue(com.getTags().contains(expectedTag), "comment tag");
    }
  }

  @Test
  public void testGetComments_ModeratorFlagIsFalse_ShouldNotModifyReturnedComments()
      throws Exception {
    // Create some comments
    for (int i = 0; i < 7; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      comment.setViewed(false);
      this.commentService.addComment(comment);
    }

    final String expectedSite = "the site";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setSite(expectedSite);

    PSComments comments = this.commentService.getComments(criteria, false);

    // After calling getComments, and as isModerator flag is false, all returned
    // comments should not be modified.

    comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(7, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertEquals(expectedSite, com.getSite(), "comment site");

      // 'viewed' flag not modified.
      Assertions.assertFalse(com.isViewed(), "comment viewed flag");
    }
  }

  @Test
  public void testGetComments_ModeratorFlagIsTrue_ReturnedCommentsGetTheirViewedFlagToTrue()
      throws Exception {
    // Create some comments
    for (int i = 0; i < 7; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      comment.setViewed(false);
      // comment.setId(String.valueOf(i));
      this.commentService.addComment(comment);
    }

    final String expectedSite = "the site";

    final PSCommentCriteria criteria = new PSCommentCriteria();
    // Change case when querying
    criteria.setSite(expectedSite);

    PSComments comments = this.commentService.getComments(criteria, false);

    // After calling getComments, all returned comments have their 'viewed'
    // flag set to true

    comments = this.commentService.getComments(criteria, true);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(7, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertEquals(expectedSite, com.getSite(), "comment site");
      Assertions.assertTrue(com.isViewed(), "comment viewed flag");
    }
  }

  @Test
  public void testGetComments_DefaultSortByCreatedDateDescending() throws Exception {
    // Create two comments with different created date properties
    PSComment comment = new PSComment();
    comment.setPagePath("/site1/folder/page1.html");
    comment.setSite("the site");
    comment.setText("some thing");
    this.commentService.addComment(comment);

    Thread.sleep(1000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    comment.setText("some other thing");
    this.commentService.addComment(comment);

    Thread.sleep(1000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page3.html");
    comment.setSite("theSite");
    comment.setText("some other thing 3");
    this.commentService.addComment(comment);

    // Create an empty criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(3, comments.getComments().size(), "comments count");

    // Create a fake comment to make the comparison easier
    final Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, cal.getActualMaximum(Calendar.YEAR));

    comment = new PSComment();
    comment.setCreatedDate(cal.getTime());

    IPSComment previousComment = comment;

    for (final IPSComment com : comments.getComments()) {
      // Make sure the comments are ascending sorted
      Assertions.assertTrue(
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
    this.commentService.addComment(comment);

    Thread.sleep(2000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    this.commentService.addComment(comment);

    // Create the criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSort(new PSCommentSort(SORTBY.CREATEDDATE, true));

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(2, comments.getComments().size(), "comments count");

    // Create a fake comment to make the comparison easier
    final Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, cal.getActualMinimum(Calendar.YEAR));

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    comment.setCreatedDate(cal.getTime());

    IPSComment previousComment = comment;

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertTrue(
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
    this.commentService.addComment(comment);

    Thread.sleep(2000);

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    this.commentService.addComment(comment);

    // Create the criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSort(new PSCommentSort(SORTBY.CREATEDDATE, false));

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(2, comments.getComments().size(), "comments count");

    // Create a fake comment to make the comparison easier
    final Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, cal.getActualMaximum(Calendar.YEAR));

    comment = new PSComment();
    comment.setPagePath("/site1/folder/page2.html");
    comment.setSite("theSite");
    comment.setCreatedDate(cal.getTime());

    IPSComment previousComment = comment;

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertTrue(
          previousComment.getCreatedDate().compareTo(com.getCreatedDate()) > 0,
          "comment created order");
      previousComment = com;
    }
  }

  @Test
  public void testGetComments_MaxResults() throws Exception {
    this.createSampleComments();

    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setMaxResults(5);

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(5, comments.getComments().size(), "comments count");
  }

  @Test
  public void testGetComments_StartIndex() throws Exception {
    final String user1 = "john";
    final String user2 = "adam";

    // Create users
    for (int i = 0; i < 4; i++) {
      final PSComment comment = new PSComment();
      comment.setUsername(user1);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("theSite");
      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 6; i++) {
      final PSComment comment = new PSComment();
      comment.setUsername(user2);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("theSite");
      this.commentService.addComment(comment);
    }

    // Create criteria
    final PSCommentCriteria criteria = new PSCommentCriteria();
    criteria.setSort(new PSCommentSort(SORTBY.USERNAME, true));
    criteria.setStartIndex(6);

    final PSComments comments = this.commentService.getComments(criteria, false);

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.getComments().size(), "comments count");

    for (final IPSComment com : comments.getComments()) {
      Assertions.assertEquals(user1, com.getUsername(), "comment user");
    }
  }

  @Test
  public void testGetPagesWithComments_SiteNotSpecified() throws Exception {
    // null site
    try {
      this.commentService.getPagesWithComments(null, 10, 0);
      Assertions.assertTrue(false, "not exception thrown");
    } catch (final IllegalArgumentException ex) {
      PSCommentsServiceTest.log.error(ex.getMessage());
      PSCommentsServiceTest.log.debug(ex);
    }

    // empty site
    try {
      final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments("", 10, 0);
      Assertions.assertTrue(false, "not exception thrown");
    } catch (final IllegalArgumentException ex) {
      PSCommentsServiceTest.log.error(ex.getMessage());
      PSCommentsServiceTest.log.debug(ex);
    }
  }

  @Test
  public void testGetPagesWithComments_BasicQuery() throws Exception {
    // Create comments

    final String pagepath1 = "/Site1/folder/page1.html";
    final String site1 = "the site";
    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(pagepath1);
      comment.setSite(site1);
      this.commentService.addComment(comment);
    }

    final String pagepath2 = "/site1/folder/Page2.html";
    for (int i = 0; i < 2; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(pagepath2);
      comment.setSite(site1);

      if (i == 1) {
        this.commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.REJECTED);
      }
      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.APPROVED);
    }

    final String pagepath3 = "/site1/FOLDER/subfolder/page.htm";
    for (int i = 0; i < 5; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(pagepath3);
      comment.setUsername("the user");
      comment.setSite(site1);
      final Set<String> tags = new HashSet<>();
      tags.add("agile");
      tags.add("cars");
      comment.setTags(tags);

      if (i % 2 == 0) {
        this.commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.REJECTED);
      }
      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/page1.html");
      comment.setSite("another site");
      this.commentService.addComment(comment);
    }

    // Get page summaries
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(site1, 0, 0);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(3, pageSummaries.getSummaries().size(), "comments count");

    for (final PSPageSummary ps : pageSummaries.getSummaries()) {
      if (pagepath1.equals(ps.getPagePath())) {
        Assertions.assertEquals(3, ps.getCommentCount(), "pagepath 1 - comment count");
        Assertions.assertEquals(3, ps.getApprovedCount(), "pagepath 1 - approved comment count");
      } else if (pagepath2.equals(ps.getPagePath())) {
        Assertions.assertEquals(2, ps.getCommentCount(), "pagepath 2 - comment count");
        Assertions.assertEquals(1, ps.getApprovedCount(), "pagepath 2 - approved comment count");
      } else if (pagepath3.equals(ps.getPagePath())) {
        Assertions.assertEquals(5, (int) ps.getCommentCount(), "pagepath 3 - comment count");
        Assertions.assertEquals(
            2, (int) ps.getApprovedCount(), "pagepath 3 - approved comment count");
      } else {
        Assertions.assertTrue(false, "wrong pagepath");
      }
    }
  }

  @Test
  public void testGetPagesWithComments_PagepathDiffersOnlyByCase() throws Exception {
    // Create comments

    final String pagepath1 = "/site1/folder/page1.html";
    final String site1 = "the site";
    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(pagepath1);
      comment.setSite(site1);
      this.commentService.addComment(comment);
    }

    final String pagepath2 =
        "/site1/FOLDER/page1.html"; // Equals to pagepath1, only differs in case
    for (int i = 0; i < 2; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(pagepath2);
      comment.setSite(site1);

      if (i == 1) {
        this.commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.REJECTED);
      }
      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState(site1, IPSComment.APPROVAL_STATE.APPROVED);
    }

    // Get page summaries
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(site1, 0, 0);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(1, pageSummaries.getSummaries().size(), "comments count");

    for (final PSPageSummary ps : pageSummaries.getSummaries()) {
      if (pagepath1.equals(ps.getPagePath()) || pagepath2.equals(ps.getPagePath())) {
        Assertions.assertEquals(5, (int) ps.getCommentCount(), "pagepath 1 - comment count");
        Assertions.assertEquals(
            4, (int) ps.getApprovedCount(), "pagepath 1 - approved comment count");
      } else {
        Assertions.assertTrue(false, "wrong pagepath");
      }
    }
  }

  @Test
  public void testGetPagesWithComments_Paging_MaxReturn_And_StartIndex_EqualsToZero()
      throws Exception {
    // Create comments
    this.createSampleCommentsForPagingTests();

    // Get page summaries
    PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(this.SITE, 0, 0);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(7, pageSummaries.getSummaries().size(), "comments count");

    // Get page summaries
    pageSummaries = this.commentService.getPagesWithComments(this.SITE, -1, -1);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(7, pageSummaries.getSummaries().size(), "comments count");
  }

  @Test
  public void testGetPagesWithComments_Paging_WithNoComments() throws Exception {
    // Get page summaries
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(this.SITE, 3, 2);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(0, pageSummaries.getSummaries().size(), "comments count");
  }

  @Test
  public void testGetPagesWithComments_Paging_FirstPage_WithMaxResults() throws Exception {
    // Create comments
    this.createSampleCommentsForPagingTests();

    // Get page summaries
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(this.SITE, 3, 0);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(3, pageSummaries.getSummaries().size(), "comments count");

    final PSPageSummary ps1 = pageSummaries.getSummaries().get(0);
    final PSPageSummary ps2 = pageSummaries.getSummaries().get(1);
    final PSPageSummary ps3 = pageSummaries.getSummaries().get(2);
    Assertions.assertFalse(
        ps1.getPagePath().equals(ps2.getPagePath()),
        "summaries must point to different pagepaths - 1");
    Assertions.assertFalse(
        ps1.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 2");
    Assertions.assertFalse(
        ps2.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 3");

    // Check that the first page summary has the correct page path and counts
    PSPageSummary firstPage = pageSummaries.getSummaries().get(0);
    Assertions.assertEquals(this.COMMENT1_PAGEPATH, firstPage.getPagePath(), "first page path");
    Assertions.assertEquals(3, firstPage.getCommentCount(), "first page comment count");
    Assertions.assertEquals(3, firstPage.getApprovedCount(), "first page approved count");
  }

  @Test
  public void testGetPagesWithComments_Paging_SecondPage_WithMaxResults() throws Exception {
    // Create comments
    this.createSampleCommentsForPagingTests();

    // Get page summaries
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(this.SITE, 3, 1);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(3, pageSummaries.getSummaries().size(), "comments count");

    final PSPageSummary ps1 = pageSummaries.getSummaries().get(0);
    final PSPageSummary ps2 = pageSummaries.getSummaries().get(1);
    final PSPageSummary ps3 = pageSummaries.getSummaries().get(2);
    Assertions.assertFalse(
        ps1.getPagePath().equals(ps2.getPagePath()),
        "summaries must point to different pagepaths - 1");
    Assertions.assertFalse(
        ps1.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 2");
    Assertions.assertFalse(
        ps2.getPagePath().equals(ps3.getPagePath()),
        "summaries must point to different pagepaths - 3");

    // Check the page paths and counts for the second page of results
    // With startIndex=1 and maxResults=3, we should get indices 1, 2, 3 from the sorted list:
    // 1. /05_site1/folder/subfolder/page.htm (COMMENT5_PAGEPATH)
    // 2. /06_site10/folder/page100.html (COMMENT6_PAGEPATH)
    // 3. /07_site1/folder/page101.html (COMMENT7_PAGEPATH)

    PSPageSummary secondPage =
        pageSummaries.getSummaries().get(0); // First in result set (index 1 in original)
    PSPageSummary thirdPage =
        pageSummaries.getSummaries().get(1); // Second in result set (index 2 in original)
    PSPageSummary fourthPage =
        pageSummaries.getSummaries().get(2); // Third in result set (index 3 in original)

    Assertions.assertEquals(this.COMMENT5_PAGEPATH, secondPage.getPagePath(), "second page path");
    Assertions.assertEquals(5, secondPage.getCommentCount(), "second page comment count");
    Assertions.assertEquals(2, secondPage.getApprovedCount(), "second page approved count");

    Assertions.assertEquals(this.COMMENT6_PAGEPATH, thirdPage.getPagePath(), "third page path");
    Assertions.assertEquals(3, thirdPage.getCommentCount(), "third page comment count");
    Assertions.assertEquals(2, thirdPage.getApprovedCount(), "third page approved count");

    Assertions.assertEquals(this.COMMENT7_PAGEPATH, fourthPage.getPagePath(), "fourth page path");
    Assertions.assertEquals(2, fourthPage.getCommentCount(), "fourth page comment count");
    Assertions.assertEquals(1, fourthPage.getApprovedCount(), "fourth page approved count");
  }

  @Test
  public void testGetPagesWithComments_Paging_LastPage_CommentsReturned_LessThan_MaxResults()
      throws Exception {
    // Create comments
    this.createSampleCommentsForPagingTests();

    // Get page summaries - startIndex=6 to get only the last item (1 item instead of 3)
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(this.SITE, 3, 6);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(1, pageSummaries.getSummaries().size(), "comments count");

    for (final PSPageSummary ps : pageSummaries.getSummaries()) {
      if (ps.getPagePath().equals(this.COMMENT10_PAGEPATH)) {
        Assertions.assertEquals(4, (int) ps.getCommentCount(), "comment count - 1");
        Assertions.assertEquals(4, (int) ps.getApprovedCount(), "approved comment count - 1");
      } else {
        Assertions.assertTrue(false, "wrong pagepath: " + ps.getPagePath());
      }
    }
  }

  @Test
  public void testApproveComment() throws Exception {
    final List<String> commentsIdToApprove = new ArrayList<String>();

    this.commentService.setDefaultModerationState("theSite", IPSComment.APPROVAL_STATE.REJECTED);
    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page1.html" + i);
      comment.setSite("theSite");
      comment = (PSComment) this.commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToApprove.add(comment.getId());
    }
    this.commentService.setDefaultModerationState("theSite", IPSComment.APPROVAL_STATE.APPROVED);
    this.commentService.approveComments(commentsIdToApprove);

    final List<IPSComment> comments =
        this.commentService.getComments(new PSCommentCriteria(), false).getComments();

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.size(), "comment count");

    int approvedComments = 0;
    int rejectedComments = 0;

    for (final IPSComment com : comments) {
      if (commentsIdToApprove.contains(com.getId())) {
        Assertions.assertEquals(
            IPSComment.APPROVAL_STATE.APPROVED,
            com.getApprovalState(),
            "comment should be approved");
        approvedComments++;
      } else {
        Assertions.assertEquals(
            IPSComment.APPROVAL_STATE.REJECTED,
            com.getApprovalState(),
            "comment should be rejected");
        rejectedComments++;
      }
    }

    Assertions.assertEquals(2, approvedComments, "comments approved count");
    Assertions.assertEquals(2, rejectedComments, "comments rejected count");
  }

  @Test
  public void testApproveComment_CommentListIsNull() throws Exception {
    try {
      this.commentService.approveComments(null);
      Assertions.fail("null argument should throw an exception");
    } catch (final IllegalArgumentException ex) {
      PSCommentsServiceTest.log.error(ex.getMessage());
      PSCommentsServiceTest.log.debug(ex);
    }
  }

  @Test
  public void testApproveComment_CommentListIsEmpty() throws Exception {
    // If the list is empty, the method should quit silently.
    this.commentService.approveComments(new ArrayList<String>());
  }

  @Test
  public void testRejectComment() throws Exception {
    final List<String> commentsIdToReject = new ArrayList<String>();

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page1.html" + i);
      comment.setApprovalState(IPSComment.APPROVAL_STATE.APPROVED);
      comment.setSite("theSite");
      comment = (PSComment) this.commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToReject.add(comment.getId());
    }

    this.commentService.rejectComments(commentsIdToReject);
    final PSCommentCriteria cc = new PSCommentCriteria();
    final List<IPSComment> comments = this.commentService.getComments(cc, false).getComments();

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.size(), "comment count");

    int approvedComments = 0;
    int rejectedComments = 0;

    for (final IPSComment com : comments) {
      if (commentsIdToReject.contains(com.getId())) {
        Assertions.assertEquals(
            IPSComment.APPROVAL_STATE.REJECTED,
            com.getApprovalState(),
            "comment should be rejected");
        rejectedComments++;
      } else {
        Assertions.assertEquals(
            IPSComment.APPROVAL_STATE.APPROVED,
            com.getApprovalState(),
            "comment should be approved");
        approvedComments++;
      }
    }

    Assertions.assertEquals(2, approvedComments, "comments approved count");
    Assertions.assertEquals(2, rejectedComments, "comments rejected count");
  }

  @Test
  public void testRejectComment_CommentListIsNull() throws Exception {
    try {
      this.commentService.rejectComments(null);
      Assertions.fail("null argument should throw an exception");
    } catch (final IllegalArgumentException ex) {
      PSCommentsServiceTest.log.error(ex.getMessage());
      PSCommentsServiceTest.log.debug(ex);
    }
  }

  @Test
  public void testRejectComment_CommentListIsEmpty() throws Exception {
    // If the list is empty, the method should quit silently.
    this.commentService.rejectComments(new ArrayList<String>());
  }

  @Test
  public void testDefaultModerationState() throws Exception {
    final String SITE1 = "site1";
    final String SITE2 = "site2";
    final String SITE3 = "site3";
    this.commentService.setDefaultModerationState(SITE1, IPSComment.APPROVAL_STATE.REJECTED);
    this.commentService.setDefaultModerationState(SITE2, IPSComment.APPROVAL_STATE.APPROVED);
    this.commentService.setDefaultModerationState(SITE3, IPSComment.APPROVAL_STATE.REJECTED);

    final IPSComment.APPROVAL_STATE state1 = this.commentService.getDefaultModerationState(SITE1);
    final IPSComment.APPROVAL_STATE state2 = this.commentService.getDefaultModerationState(SITE2);
    final IPSComment.APPROVAL_STATE state3 = this.commentService.getDefaultModerationState(SITE3);

    Assertions.assertEquals(IPSComment.APPROVAL_STATE.REJECTED, state1);
    Assertions.assertEquals(IPSComment.APPROVAL_STATE.APPROVED, state2);
    Assertions.assertEquals(IPSComment.APPROVAL_STATE.REJECTED, state3);

    Assertions.assertEquals(
        IPSComment.APPROVAL_STATE.APPROVED,
        this.commentService.getDefaultModerationState("UNKNOWN"));
  }

  @Test
  public void testGetPagesWithComments_Performance() throws Exception {
    PSCommentsServiceTest.log.info("Adding comments");
    for (int i = 0; i < this.COMMENT_COUNT_FOR_PERFORMANCE_TESTS; i++)
      this.createSampleCommentsForPagingTests(Integer.toString(i));

    // Disable second-level Hibernate cache. Close current session to flush
    // first-level cache.
    // session.close();

    this.sessionFactory.getCache().evictAll();
    this.sessionFactory.getCache().evictQueryRegions();

    // Get page summaries for various pages
    for (int i = 0; i < 3; i++) {
      PSCommentsServiceTest.log.info("Getting pages with comments");
      final Calendar before = Calendar.getInstance();
      final PSPageSummaries pageSummariesPage0 =
          this.commentService.getPagesWithComments(this.SITE + "0", 3, i);
      final Calendar after = Calendar.getInstance();

      Assertions.assertEquals(3, pageSummariesPage0.getSummaries().size(), "page summaries count");

      PSCommentsServiceTest.log.info(
          "Page {} - Query took: {} milliseconds",
          i,
          (after.getTimeInMillis() - before.getTimeInMillis()));
    }
  }

  @Test
  public void testDeleteComments() throws Exception {
    final List<String> commentsIdToDelete = new ArrayList<String>();

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page" + i + ".html");
      comment.setSite("theSite");
      comment = (PSComment) this.commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToDelete.add(comment.getId());
    }

    List<IPSComment> comments =
        this.commentService.getComments(new PSCommentCriteria(), false).getComments();

    Assertions.assertNotNull(comments, "comments not null");
    Assertions.assertEquals(4, comments.size(), "comment count");

    // Delete comments
    this.commentService.deleteComments(commentsIdToDelete);

    comments = this.commentService.getComments(new PSCommentCriteria(), false).getComments();

    Assertions.assertEquals(2, comments.size(), "comments count");

    for (final IPSComment com : comments) {
      Assertions.assertTrue(
          !commentsIdToDelete.contains(com.getId()), "current comment is not the deleted one");
    }
  }

  @Test
  public void testDeleteComments_CommentsWithTags() throws Exception {
    final List<String> commentsIdToDelete = new ArrayList<String>();

    for (int i = 0; i < 4; i++) {
      PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page" + i + ".html");
      comment.setSite("theSite");
      comment.setTags(
          new HashSet<String>() {
            {
              this.add("agile");
              this.add("cars");
            }
          });

      comment = (PSComment) this.commentService.addComment(comment);

      if (i % 2 == 0) commentsIdToDelete.add(comment.getId());
    }

    final List<IPSComment> comments =
        this.commentService.getComments(new PSCommentCriteria(), false).getComments();
    int countTags = 0;
    for (final IPSComment comm : comments) {
      final Set tags = comm.getTags();
      countTags = countTags + tags.size();
    }

    // Make sure there are comment tags in database
    Assertions.assertEquals(8, countTags, "comment tags count before deleting");

    // reCreateSession();

    // Delete all added comments
    this.commentService.deleteComments(commentsIdToDelete);

    final List<IPSComment> comments2 =
        this.commentService.getComments(new PSCommentCriteria(), false).getComments();
    Assertions.assertEquals(2, comments2.size(), "comments count");

    countTags = 0;
    for (final IPSComment comm : comments2) {
      final Set tags = comm.getTags();
      countTags = countTags + tags.size();
    }
    // Correct count of tags
    Assertions.assertEquals(4, countTags, "comment tags count after deleting");
  }

  @Test
  public void testDeleteComments_EmptyIdList() throws Exception {
    this.commentService.deleteComments(new ArrayList<String>());
  }

  @Test
  public void testDeleteComments_NullIdList() throws Exception {
    try {
      this.commentService.deleteComments(null);
      Assertions.fail("It has to throw an exception");
    } catch (final IllegalArgumentException ex) {
      PSCommentsServiceTest.log.error(ex.getMessage());
      PSCommentsServiceTest.log.debug(ex);
    }
  }

  @Test
  public void testGetNewComments() throws Exception {
    // Create comments
    final String pagepath1 = "/Site1/folder/page1.html";
    final String site1 = "the site";
    // in pagepath1: newComment = 5
    for (int i = 0; i < 8; i++) {
      final PSComment comment = new PSComment();
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
      this.commentService.addComment(comment);
    }

    final String pagepath2 = "/Site1/folder/page2.html";
    // in pagepath2: newComment = 2
    for (int i = 0; i < 5; i++) {
      final PSComment comment = new PSComment();
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
      this.commentService.addComment(comment);
    }

    // Get page summaries
    final PSPageSummaries pageSummaries = this.commentService.getPagesWithComments(site1, 10, 0);

    Assertions.assertNotNull(pageSummaries, "comments not null");
    Assertions.assertEquals(2, pageSummaries.getSummaries().size(), "comments count");

    for (final PSPageSummary ps : pageSummaries.getSummaries()) {
      if (pagepath1.equals(ps.getPagePath())) {
        Assertions.assertEquals(5, (int) ps.getNewCommentCount(), "pagepath 1 - new comment");
        Assertions.assertEquals(8, ps.getCommentCount(), "pagepath 1 - new comment");
      } else if (pagepath2.equals(ps.getPagePath())) {
        Assertions.assertEquals(2, ps.getNewCommentCount(), "pagepath 2 - new comment");
        Assertions.assertEquals(5, ps.getCommentCount(), "pagepath 1 - new comment");
      } else {
        Assertions.assertTrue(false, "wrong pagepath");
      }
    }
  }

  private void createSampleCommentsForPagingTests() throws Exception {
    this.createSampleCommentsForPagingTests(StringUtils.EMPTY);
  }

  private void createSampleCommentsForPagingTests(final String pagepathSuffix) throws Exception {
    // Creates 18 comments, generates 10 page summaries.

    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT1_PAGEPATH + pagepathSuffix);
      comment.setSite(this.SITE + pagepathSuffix);
      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 5; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT5_PAGEPATH + pagepathSuffix);
      comment.setUsername("the user");
      comment.setSite(this.SITE + pagepathSuffix);
      comment.setTags(
          new HashSet<String>() {
            {
              this.add("agile");
              this.add("cars");
            }
          });

      if (i % 2 == 0) {
        this.commentService.setDefaultModerationState(
            "the site", IPSComment.APPROVAL_STATE.REJECTED);
      }
      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT6_PAGEPATH + pagepathSuffix);
      comment.setSite(this.SITE + pagepathSuffix);

      if (i == 1) {
        this.commentService.setDefaultModerationState(
            "the site", IPSComment.APPROVAL_STATE.REJECTED);
      }

      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 2; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT7_PAGEPATH + pagepathSuffix);
      comment.setSite(this.SITE + pagepathSuffix);
      final HashSet<String> tags = new HashSet<String>();
      tags.add("agile");
      tags.add("cars");
      comment.setTags(tags);

      if (i == 1) {
        this.commentService.setDefaultModerationState(
            "the site", IPSComment.APPROVAL_STATE.REJECTED);
      }
      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT8_PAGEPATH + pagepathSuffix);
      comment.setSite(this.SITE + pagepathSuffix);
      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 7; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT9_PAGEPATH + pagepathSuffix);
      comment.setSite(this.SITE + pagepathSuffix);
      final HashSet<String> tags = new HashSet<String>();
      tags.add("agile");
      tags.add("cars");
      comment.setTags(tags);

      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 4; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath(this.COMMENT10_PAGEPATH + pagepathSuffix);
      comment.setSite(this.SITE + pagepathSuffix);
      this.commentService.addComment(comment);
    }
  }

  private void createSampleComments() throws Exception {
    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath("/site1/Folder/page1.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 2; i++) {
      final PSComment comment2 = new PSComment();
      comment2.setPagePath("/site1/folder/page2.html");
      comment2.setSite("the site");
      this.commentService.addComment(comment2);
    }

    for (int i = 0; i < 4; i++) {
      final PSComment comment = new PSComment();
      comment.setUsername("john");
      comment.setModerated(true);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 1; i++) {
      final PSComment comment = new PSComment();
      comment.setUsername("adam");
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
    }

    for (int i = 0; i < 3; i++) {
      final PSComment comment = new PSComment();
      comment.setTags(
          new HashSet<String>() {
            {
              this.add("general");
              this.add("agile");
              this.add("nosql");
              this.add("databases");
            }
          });
      this.commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.REJECTED);
      comment.setViewed(true);
      comment.setPagePath("/site1/folder/page2.html");
      comment.setSite("the site");
      this.commentService.addComment(comment);
      this.commentService.setDefaultModerationState("the site", IPSComment.APPROVAL_STATE.APPROVED);
    }

    for (int i = 0; i < 2; i++) {
      final PSComment comment = new PSComment();
      comment.setPagePath("/site1/folder/subfolder/page.htm");
      comment.setUsername("the user");
      comment.setSite("the site");
      final HashSet<String> tags = new HashSet<String>();
      tags.add("agile");
      tags.add("cars");
      comment.setTags(tags);
      this.commentService.addComment(comment);
    }
  }
}
