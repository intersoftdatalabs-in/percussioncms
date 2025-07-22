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

import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Immutable class holding page and comment summary information.
 */
@XmlRootElement(name = "pageSummary")
@XmlAccessorType(XmlAccessType.FIELD)
public final class PSPageSummary {
    @XmlElement(required = true)
    private final String pagePath;
    private final long commentCount;
    private final long approvedCount;
    private final long newCommentCount;

    /**
     * Creates a new page summary.
     */
    private PSPageSummary(Builder builder) {
        this.pagePath = Objects.requireNonNull(builder.pagePath, "pagePath must not be null");
        this.commentCount = builder.commentCount;
        this.approvedCount = builder.approvedCount;
        this.newCommentCount = builder.newCommentCount;
    }

    /**
     * Default constructor for JAXB.
     */
    protected PSPageSummary() {
        this.pagePath = "";
        this.commentCount = 0;
        this.approvedCount = 0;
        this.newCommentCount = 0;
    }

    /**
     * @return the page path, never null
     */
    public String getPagePath() {
        return pagePath;
    }

    /**
     * @return total number of comments
     */
    public long getCommentCount() {
        return commentCount;
    }

    /**
     * @return number of approved comments
     */
    public long getApprovedCount() {
        return approvedCount;
    }

    /**
     * @return number of new comments
     */
    public long getNewCommentCount() {
        return newCommentCount;
    }

    /**
     * Creates a new builder for PSPageSummary.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PSPageSummary.
     */
    public static class Builder {
        private String pagePath;
        private long commentCount;
        private long approvedCount;
        private long newCommentCount;

        public Builder pagePath(String pagePath) {
            this.pagePath = pagePath;
            return this;
        }

        public Builder commentCount(long commentCount) {
            this.commentCount = commentCount;
            return this;
        }

        public Builder approvedCount(long approvedCount) {
            this.approvedCount = approvedCount;
            return this;
        }

        public Builder newCommentCount(long newCommentCount) {
            this.newCommentCount = newCommentCount;
            return this;
        }

        public PSPageSummary build() {
            return new PSPageSummary(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSPageSummary)) return false;
        PSPageSummary that = (PSPageSummary) o;
        return commentCount == that.commentCount &&
               approvedCount == that.approvedCount &&
               newCommentCount == that.newCommentCount &&
               pagePath.equals(that.pagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pagePath, commentCount, approvedCount, newCommentCount);
    }

    @Override
    public String toString() {
        return String.format("PSPageSummary{path='%s', total=%d, approved=%d, new=%d}",
            pagePath, commentCount, approvedCount, newCommentCount);
    }
}
