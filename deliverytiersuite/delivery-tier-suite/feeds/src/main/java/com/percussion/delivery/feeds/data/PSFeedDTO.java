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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

/**
 * Data Transfer Object for feed information.
 * Immutable class following the builder pattern.
 */
public final class PSFeedDTO {
    private final String name;
    private final String site;
    private final String description;
    private final String link;
    private final String title;
    private final FeedType type;
    private final String feedUrl;

    private PSFeedDTO(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        this.site = Objects.requireNonNull(builder.site, "site must not be null");
        this.description = builder.description;
        this.link = builder.link;
        this.title = builder.title;
        this.type = Objects.requireNonNull(builder.type, "type must not be null");
        this.feedUrl = Objects.requireNonNull(builder.feedUrl, "feedUrl must not be null");
    }

    public PSFeedDTO(IPSFeedDescriptor descriptor, String baseUrl) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");

        this.name = descriptor.getName()
            .orElseThrow(() -> new IllegalArgumentException("Feed name must not be null"));
        this.site = descriptor.getSite()
            .orElseThrow(() -> new IllegalArgumentException("Feed site must not be null"));
        this.description = descriptor.getDescription().orElse(null);
        this.link = descriptor.getLink().orElse(null);
        this.title = descriptor.getTitle().orElse(null);
        this.type = descriptor.getType()
            .flatMap(FeedType::fromName)
            .orElseThrow(() -> new IllegalArgumentException("Invalid feed type"));
        this.feedUrl = buildFeedUrl(baseUrl, this.site, this.name);
    }

    private static String buildFeedUrl(String baseUrl, String site, String name) {
        return baseUrl.endsWith("/")
            ? baseUrl + "feeds/" + site + "/" + name
            : baseUrl + "/feeds/" + site + "/" + name;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("site")
    public String getSite() {
        return site;
    }

    @JsonProperty("description")
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @JsonProperty("link")
    public Optional<String> getLink() {
        return Optional.ofNullable(link);
    }

    @JsonProperty("title")
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @JsonProperty("type")
    public FeedType getType() {
        return type;
    }

    @JsonProperty("feedUrl")
    public String getFeedUrl() {
        return feedUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String site;
        private String description;
        private String link;
        private String title;
        private FeedType type;
        private String feedUrl;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder site(String site) {
            this.site = site;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(FeedType type) {
            this.type = type;
            return this;
        }

        public Builder feedUrl(String feedUrl) {
            this.feedUrl = feedUrl;
            return this;
        }

        public PSFeedDTO build() {
            return new PSFeedDTO(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSFeedDTO)) return false;
        PSFeedDTO other = (PSFeedDTO) o;
        return Objects.equals(name, other.name) &&
               Objects.equals(site, other.site) &&
               Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, site, type);
    }
}
