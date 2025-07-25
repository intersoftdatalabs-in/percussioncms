// REFACTORED: CP-JAVA11
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.comments.service.rdbms;

import com.percussion.delivery.comments.data.IPSComment;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA entity representing a comment in the system.
 * Uses Hibernate second-level cache for improved performance.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSComments1")
@Table(name = "PERC_PAGE_COMMENTS", indexes = {
    @Index(name = "idx_comment_site", columnList = "site"),
    @Index(name = "idx_comment_path", columnList = "pagePath")
})
public class PSComment implements IPSComment, Serializable {
    private static final long serialVersionUID = 1L;

    @TableGenerator(
        name = "commentId",
        table = "PERC_ID_GEN",
        pkColumnName = "GEN_KEY",
        valueColumnName = "GEN_VALUE",
        pkColumnValue = "commentId",
        allocationSize = 1
    )
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "commentId")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private APPROVAL_STATE approvalState = APPROVAL_STATE.PENDING;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdDate;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false)
    private String pagePath;

    @Size(max = 4000)
    private String email;

    @Size(max = 4000)
    private String username;

    @NotBlank
    @Lob
    @Column(length = Integer.MAX_VALUE, nullable = false)
    private String text;

    @Size(max = 4000)
    private String title;

    private Long parent;

    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        mappedBy = "comment"
    )
    @Fetch(FetchMode.SUBSELECT)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<PSCommentTag> commentTags = new HashSet<>();

    private boolean moderated;
    private boolean viewed;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String site;

    @Size(max = 2000)
    private String url;

    private String commentCreatedDate;

    // Required by JPA
    protected PSComment() {}

    /**
     * Creates a new comment with required fields.
     */
    public static PSComment create(String text, String site, String pagePath) {
        var comment = new PSComment();
        comment.text = Objects.requireNonNull(text, "text must not be null");
        comment.site = Objects.requireNonNull(site, "site must not be null");
        comment.pagePath = Objects.requireNonNull(pagePath, "pagePath must not be null");
        comment.createdDate = new Date();
        return comment;
    }

    @Override
    public String getId() {
        return Optional.ofNullable(id)
            .map(String::valueOf)
            .orElse(null);
    }

    @Override
    public Optional<String> getParent() {
        return Optional.ofNullable(parent)
            .map(String::valueOf);
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(@NotBlank String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getSite() {
        return site;
    }

    public void setSite(@NotBlank String site) {
        this.site = Objects.requireNonNull(site, "site must not be null");
    }

    @Override
    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(@NotBlank String pagePath) {
        this.pagePath = Objects.requireNonNull(pagePath, "pagePath must not be null");
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public Date getCreatedDate() {
        return new Date(createdDate.getTime());
    }

    @Override
    public Set<String> getTags() {
        return commentTags.stream()
            .map(PSCommentTag::getTag)
            .collect(Collectors.toUnmodifiableSet());
    }

    public void setTags(Set<String> tags) {
        this.commentTags.clear();
        Optional.ofNullable(tags)
            .orElse(Set.of())
            .stream()
            .map(tag -> PSCommentTag.create(this, tag))
            .forEach(this.commentTags::add);
    }

    @Override
    public APPROVAL_STATE getApprovalState() {
        return approvalState;
    }

    public void setApprovalState(@NotNull APPROVAL_STATE state) {
        this.approvalState = Objects.requireNonNull(state, "state must not be null");
    }

    @Override
    public boolean isModerated() {
        return moderated;
    }

    public void setModerated(boolean moderated) {
        this.moderated = moderated;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public String getCommentCreatedDate() {
        return commentCreatedDate;
    }

    public void setCommentCreatedDate(String commentCreatedDate) {
        this.commentCreatedDate = commentCreatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSComment)) return false;
        var that = (PSComment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("Comment{id=%d, title='%s', author='%s', state=%s}",
            id, title, username, approvalState);
    }
}
