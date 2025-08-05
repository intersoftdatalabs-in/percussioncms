// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * ...existing code...
 */
package com.percussion.delivery.comments.service.rdbms;

import javax.persistence.*;

/**
 * Represents a tag associated with a comment.
 * @author miltonpividori
 */
@Entity
@Table(name = "PERC_COMMENT_TAGS")
public class PSCommentTag {

    @TableGenerator(
        name = "commentTagId",
        table = "PERC_ID_GEN",
        pkColumnName = "GEN_KEY",
        valueColumnName = "GEN_VALUE",
        pkColumnValue = "commentTagId",
        allocationSize = 1)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "commentTagId")
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "COMMENT_ID")
    private PSComment comment;

    @Basic
    private String name;

    public PSCommentTag() {
        // Default constructor
    }

    public PSCommentTag(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public PSComment getComment() {
        return comment;
    }

    public void setComment(PSComment comment) {
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

