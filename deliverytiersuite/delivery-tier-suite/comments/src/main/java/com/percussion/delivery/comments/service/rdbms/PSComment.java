// REFACTORED: CP-JAVA11
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

import com.percussion.delivery.comments.data.IPSComment;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * RDBMS-backed entity representing a comment. Mapped to the {@code PERC_PAGE_COMMENTS} table and
 * related to its tags via {@link PSCommentTag}. Implements {@link IPSComment} so it can be used
 * directly by the delivery tier service layer.
 *
 * @author erikserating
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments1")
@Table(name = "PERC_PAGE_COMMENTS")
public class PSComment implements IPSComment, Serializable {
  private static final long serialVersionUID = 1L;

  /** Unique identifier for the comment. Assigned by the persistence layer. */
  @TableGenerator(
      name = "commentId",
      table = "PERC_ID_GEN",
      pkColumnName = "GEN_KEY",
      valueColumnName = "GEN_VALUE",
      pkColumnValue = "commentId",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "commentId")
  private long id;

  /** Current approval state of the comment, stored as the string form of {@link APPROVAL_STATE}. */
  @Basic
  private String approvalState =
      IPSComment.approvalStateToString(IPSComment.APPROVAL_STATE.APPROVED);

  /** Date and time the comment was created. */
  @Basic private Date createdDate;

  /** Relative path of the page being commented on, not including the site. */
  @Basic private String pagePath;

  /** Email address supplied by the comment author. May be {@code null}. */
  @Basic
  @Column(length = 4000)
  private String email;

  /** User name supplied by the comment author. May be {@code null}. */
  @Basic
  @Column(length = 4000)
  private String username;

  /** Body text of the comment. May be {@code null}. */
  @Lob
  @Column(length = Integer.MAX_VALUE)
  private String text;

  /** Title of the comment. May be {@code null}. */
  @Basic
  @Column(length = 4000)
  private String title;

  /** Numeric id of the parent comment, or {@code 0} for top-level comments. */
  @Basic private long parent;

  /** Set of {@link PSCommentTag} entities associated with this comment. */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      mappedBy = "comment",
      targetEntity = PSCommentTag.class)
  @Fetch(FetchMode.SUBSELECT)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Set<PSCommentTag> commentTags = new HashSet<>();

  /** Flag indicating the comment has been moderated by a user action. */
  @Basic private boolean moderated;

  /** Flag indicating the comment has been viewed by an admin. */
  @Basic private boolean viewed;

  /** Site this comment belongs to. */
  @Basic private String site;

  /** URL supplied by the comment author. May be {@code null}. */
  @Basic
  @Column(length = 2000)
  private String url;

  /** Comment created date as a string, used for legacy clients. Not persisted. */
  @Transient private String commentCreatedDate;

  /** Default no-arg constructor required by Hibernate. */
  public PSComment() {}

  /**
   * Creates a new comment with the same values as the given one, except for the id.
   *
   * <p>Tag materialization is inlined here (rather than calling {@link #setTags}) because that
   * method is overridable, and invoking it from a constructor would expose a partially-constructed
   * instance to subclass overrides. The parent back-reference on each {@link PSCommentTag} is set
   * via direct field access on the package-private {@code comment} field — a field write is not a
   * method call, so it does not trip javac's {@code this-escape} lint.
   *
   * @param comment A comment to create a copy from.
   */
  public PSComment(final IPSComment comment) {
    this.approvalState = comment.getApprovalState().toString();
    this.createdDate = comment.getCreatedDate();
    this.email = comment.getEmail();
    this.moderated = comment.isModerated();
    this.pagePath = comment.getPagePath();
    this.parent = comment.getParent() == null ? 0 : Long.valueOf(comment.getParent());
    this.site = comment.getSite();
    final Set<String> tags = comment.getTags();
    if (tags != null) {
      PSCommentTag commentTag;
      for (final String aTag : tags) {
        commentTag = new PSCommentTag(aTag);
        commentTag.comment = this;
        this.commentTags.add(commentTag);
      }
    }
    this.text = comment.getText();
    this.title = comment.getTitle();
    this.url = comment.getUrl();
    this.username = comment.getUsername();
    this.viewed = comment.isViewed();
    this.commentCreatedDate = comment.getCommentCreatedDate();
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getApprovalState()
   */
  @Override
  public APPROVAL_STATE getApprovalState() {
    return APPROVAL_STATE.valueOf(this.approvalState);
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getCreatedDate()
   */
  @Override
  public Date getCreatedDate() {
    return this.createdDate;
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getEmail()
   */
  @Override
  public String getEmail() {
    return this.email;
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getId()
   */
  @Override
  public String getId() {
    return String.valueOf(this.id);
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getPagePath()
   */
  @Override
  public String getPagePath() {
    return this.pagePath;
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getParent()
   */
  @Override
  public String getParent() {
    return String.valueOf(this.parent);
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getTags()
   */
  @Override
  public Set<String> getTags() {
    final Set<String> tagsAsString = new HashSet<>();

    for (final PSCommentTag tag : this.commentTags) tagsAsString.add(tag.getName());

    return tagsAsString;
  }

  /**
   * Gets the set of tag entities attached to this comment.
   *
   * @return the set of {@link PSCommentTag} entities, never {@code null}.
   */
  public Set<PSCommentTag> getCommentTags() {
    return this.commentTags;
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getText()
   */
  @Override
  public String getText() {
    return this.text;
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#getUsername()
   */
  @Override
  public String getUsername() {
    return this.username;
  }

  /**
   * @param createdDate the createdDate to set
   */
  @Override
  public void setCreatedDate(final Date createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * @param id the id to set
   */
  @Override
  public void setId(final String id) {
    this.id = id == null ? 0 : Long.valueOf(id);
  }

  /**
   * @param pagePath the pagePath to set
   */
  @Override
  public void setPagePath(final String pagePath) {
    this.pagePath = pagePath;
  }

  /**
   * @param email the email to set
   */
  @Override
  public void setEmail(final String email) {
    this.email = email;
  }

  /**
   * @param username the username to set
   */
  @Override
  public void setUsername(final String username) {
    this.username = username;
  }

  /**
   * @param text the text to set
   */
  @Override
  public void setText(final String text) {
    this.text = text;
  }

  /**
   * @param parent the parent to set
   */
  @Override
  public void setParent(final String parent) {
    this.parent = Long.valueOf(parent);
  }

  /**
   * Replaces the tags for this comment with a new set of tag strings. Each string is converted to a
   * {@link PSCommentTag} and linked back to this comment.
   *
   * @param tags the tag strings, may be {@code null} (in which case no change is made).
   */
  public void setTags(final Set<String> tags) {
    if (tags == null) return;
    PSCommentTag commentTag;

    for (final String aTag : tags) {
      commentTag = new PSCommentTag(aTag);
      commentTag.setComment(this);
      this.commentTags.add(commentTag);
    }
  }

  /**
   * Replaces the tag entities for this comment.
   *
   * @param commentTags the new set of {@link PSCommentTag} entities, must not be {@code null}.
   */
  public void setCommentTags(final Set<PSCommentTag> commentTags) {
    this.commentTags = commentTags;
  }

  /**
   * @param approvalState the approvalState to set
   */
  @Override
  public void setApprovalState(final APPROVAL_STATE approvalState) {
    this.approvalState = approvalState.toString();
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#isModerated()
   */
  @Override
  public boolean isModerated() {
    return this.moderated;
  }

  /* (non-Javadoc)
   * @see com.percussion.comments.data.IPSComment#isViewed()
   */
  @Override
  public boolean isViewed() {
    return this.viewed;
  }

  /**
   * @param moderated the moderated to set
   */
  @Override
  public void setModerated(final boolean moderated) {
    this.moderated = moderated;
  }

  /**
   * @param viewed the viewed to set
   */
  @Override
  public void setViewed(final boolean viewed) {
    this.viewed = viewed;
  }

  /**
   * @return the site
   */
  @Override
  public String getSite() {
    return this.site;
  }

  /**
   * @param site the site to set
   */
  @Override
  public void setSite(final String site) {
    this.site = site;
  }

  /**
   * @return the url
   */
  @Override
  public String getUrl() {
    return this.url;
  }

  /**
   * @param url the url to set
   */
  @Override
  public void setUrl(final String url) {
    this.url = url;
  }

  /**
   * @return the title
   */
  @Override
  public String getTitle() {
    return this.title;
  }

  /**
   * @param title the title to set
   */
  @Override
  public void setTitle(final String title) {
    this.title = title;
  }

  @Override
  public String getCommentCreatedDate() {
    return this.commentCreatedDate;
  }

  @Override
  public void setCommentCreatedDate(final String commentCreatedDate) {
    this.commentCreatedDate = commentCreatedDate;
  }
}
