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
package com.percussion.delivery.comments.data;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * REST-friendly implementation of IPSComment with immutable fields.
 */
@XmlRootElement(name = "comment")
@XmlAccessorType(XmlAccessType.FIELD)
public final class PSRestComment implements IPSComment {
    private final String id;
    private final String parent;
    private final String text;
    private final String title;
    private final String site;
    private final String pagePath;
    private final String username;
    private final String url;
    private final String email;
    private final Date createdDate;
    private final Set<String> tags;
    private final APPROVAL_STATE approvalState;
    private final boolean moderated;
    private final boolean viewed;

    private PSRestComment(Builder builder) {
        this.id = builder.id;
        this.parent = builder.parent;
        this.text = Objects.requireNonNull(builder.text, "text must not be null");
        this.title = builder.title;
        this.site = Objects.requireNonNull(builder.site, "site must not be null");
        this.pagePath = Objects.requireNonNull(builder.pagePath, "pagePath must not be null");
        this.username = builder.username;
        this.url = builder.url;
        this.email = builder.email;
        this.createdDate = new Date(Objects.requireNonNull(builder.createdDate, "createdDate must not be null").getTime());
        this.tags = Collections.unmodifiableSet(new HashSet<>(
            Objects.requireNonNull(builder.tags, "tags must not be null")));
        this.approvalState = Objects.requireNonNull(builder.approvalState, "approvalState must not be null");
        this.moderated = builder.moderated;
        this.viewed = builder.viewed;
    }

    /**
     * Default constructor for JAXB.
     */
    protected PSRestComment() {
        this.id = "";
        this.parent = null;
        this.text = "";
        this.title = null;
        this.site = "";
        this.pagePath = "";
        this.username = null;
        this.url = null;
        this.email = null;
        this.createdDate = new Date();
        this.tags = Collections.emptySet();
        this.approvalState = APPROVAL_STATE.PENDING;
        this.moderated = false;
        this.viewed = false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<String> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @Override
    public String getSite() {
        return site;
    }

    @Override
    public String getPagePath() {
        return pagePath;
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @Override
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    @Override
    public Date getCreatedDate() {
        return new Date(createdDate.getTime());
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public APPROVAL_STATE getApprovalState() {
        return approvalState;
    }

    @Override
    public boolean isModerated() {
        return moderated;
    }

    public boolean isViewed() {
        return viewed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String parent;
        private String text;
        private String title;
        private String site;
        private String pagePath;
        private String username;
        private String url;
        private String email;
        private Date createdDate;
        private Set<String> tags = new HashSet<>();
        private APPROVAL_STATE approvalState = APPROVAL_STATE.PENDING;
        private boolean moderated;
        private boolean viewed;

        public Builder id(String id) { this.id = id; return this; }
        public Builder parent(String parent) { this.parent = parent; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder site(String site) { this.site = site; return this; }
        public Builder pagePath(String pagePath) { this.pagePath = pagePath; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder createdDate(Date createdDate) { this.createdDate = createdDate; return this; }
        public Builder tags(Set<String> tags) { this.tags = tags; return this; }
        public Builder approvalState(APPROVAL_STATE state) { this.approvalState = state; return this; }
        public Builder moderated(boolean moderated) { this.moderated = moderated; return this; }
        public Builder viewed(boolean viewed) { this.viewed = viewed; return this; }

        public PSRestComment build() {
            return new PSRestComment(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSRestComment)) return false;
        PSRestComment that = (PSRestComment) o;
        return moderated == that.moderated &&
               viewed == that.viewed &&
               Objects.equals(id, that.id) &&
               Objects.equals(parent, that.parent) &&
               Objects.equals(text, that.text) &&
               Objects.equals(title, that.title) &&
               Objects.equals(site, that.site) &&
               Objects.equals(pagePath, that.pagePath) &&
               Objects.equals(username, that.username) &&
               Objects.equals(url, that.url) &&
               Objects.equals(email, that.email) &&
               Objects.equals(createdDate, that.createdDate) &&
               Objects.equals(tags, that.tags) &&
               approvalState == that.approvalState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parent, text, title, site, pagePath, username, url,
                          email, createdDate, tags, approvalState, moderated, viewed);
    }

    @Override
    public String toString() {
        return String.format("PSRestComment{id='%s', title='%s', author='%s', state=%s}",
            id, title, username, approvalState);
    }
}
