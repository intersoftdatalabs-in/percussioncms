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
package com.percussion.delivery.feeds.data;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an item in a feed with title, description, publish date and link.
 */
public final class PSFeedItem {
    private final String title;
    private final String description;
    private final Instant publishDate;
    private final String link;

    private PSFeedItem(Builder builder) {
        this.title = Objects.requireNonNull(builder.title, "title must not be null");
        this.description = builder.description;
        this.publishDate = Objects.requireNonNull(builder.publishDate, "publishDate must not be null");
        this.link = Objects.requireNonNull(builder.link, "link must not be null");
    }

    public String getTitle() {
        return title;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Instant getPublishDate() {
        return publishDate;
    }

    public String getLink() {
        return link;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private String description;
        private Instant publishDate;
        private String link;

        private Builder() {}

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder publishDate(Instant publishDate) {
            this.publishDate = publishDate;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public PSFeedItem build() {
            return new PSFeedItem(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSFeedItem that = (PSFeedItem) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(description, that.description) &&
               Objects.equals(publishDate, that.publishDate) &&
               Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, publishDate, link);
    }

    @Override
    public String toString() {
        return "PSFeedItem{" +
               "title='" + title + '\'' +
               ", description='" + description + '\'' +
               ", publishDate=" + publishDate +
               ", link='" + link + '\'' +
               '}';
    }
}
