// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * ...existing code...
 */
package com.percussion.delivery.comments.service.rdbms;

import com.percussion.delivery.comments.data.IPSComment;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a persisted comment entity.
 * @author erikserating
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments1")
@Table(name = "PERC_PAGE_COMMENTS")
public class PSComment implements IPSComment, Serializable {
    // ...existing code...

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL,
            orphanRemoval = true, mappedBy = "comment", targetEntity = PSCommentTag.class)
    @Fetch(FetchMode.SUBSELECT)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<PSCommentTag> commentTags = new HashSet<>();

    // ...existing code...

    public PSComment() {
        // Default constructor
    }

    /**
     * Creates a new comment with the same values as the given one,
     * except for the id.
     *
     * @param comment A comment to create a copy from.
     */
    public PSComment(IPSComment comment) {
        this.approvalState = comment.getApprovalState().toString();
        this.createdDate = comment.getCreatedDate();
        this.email = comment.getEmail();
        this.moderated = comment.isModerated();
        this.pagePath = comment.getPagePath();
        this.parent = comment.getParent() == null ? 0 : Long.valueOf(comment.getParent());
        this.site = comment.getSite();
        setTags(comment.getTags());
        this.text = comment.getText();
        this.title = comment.getTitle();
        this.url = comment.getUrl();
        this.username = comment.getUsername();
        this.viewed = comment.isViewed();
        this.commentCreatedDate = comment.getCommentCreatedDate();
    }

    // ...existing code...

    @Override
    public Set<String> getTags() {
        // Java 11 Streams for conversion
        return commentTags.stream()
                .map(PSCommentTag::getName)
                .collect(Collectors.toSet());
    }

    // ...existing code...

    @Override
    public void setTags(Set<String> tags) {
        if (tags == null) {
            return;
        }
        commentTags.clear();
        tags.forEach(tagName -> {
            var commentTag = new PSCommentTag(tagName);
            commentTag.setComment(this);
            commentTags.add(commentTag);
        });
    }

    // ...existing code...
}

