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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

/**
 * JPA entity representing a tag associated with a comment.
 * Uses Hibernate second-level cache for improved performance.
 */
@Entity
@Table(
    name = "PERC_COMMENT_TAGS",
    indexes = @Index(name = "idx_comment_tag_name", columnList = "name")
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSCommentTags")
public class PSCommentTag implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableGenerator(
        name = "commentTagId",
        table = "PERC_ID_GEN",
        pkColumnName = "GEN_KEY",
        valueColumnName = "GEN_VALUE",
        pkColumnValue = "commentTagId",
        allocationSize = 1
    )
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "commentTagId")
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "COMMENT_ID", nullable = false)
    private PSComment comment;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    // Required by JPA
    protected PSCommentTag() {}

    /**
     * Creates a new tag for the given comment.
     * @param comment the comment to tag, must not be null
     * @param tag the tag name, must not be blank
     * @return the new tag entity
     */
    public static PSCommentTag create(PSComment comment, String tag) {
        var commentTag = new PSCommentTag();
        commentTag.setComment(Objects.requireNonNull(comment, "comment must not be null"));
        commentTag.setName(Objects.requireNonNull(tag, "tag must not be blank"));
        return commentTag;
    }

    public Long getId() {
        return id;
    }

    public PSComment getComment() {
        return comment;
    }

    public void setComment(@NotNull PSComment comment) {
        this.comment = Objects.requireNonNull(comment, "comment must not be null");
    }

    public String getTag() {
        return name;
    }

    public void setName(@NotBlank String name) {
        this.name = Objects.requireNonNull(name, "name must not be blank");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSCommentTag)) return false;
        var that = (PSCommentTag) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("CommentTag{id=%d, tag='%s'}", id, name);
    }
}
