/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.data;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable implementation of IPSFeedDescriptor using builder pattern.
 */
public final class PSFeedDescriptor implements IPSFeedDescriptor {
    private final String name;
    private final String site;
    private final String description;
    private final String link;
    private final String title;
    private final String query;
    private final String type;

    private PSFeedDescriptor(Builder builder) {
        this.name = builder.name;
        this.site = builder.site;
        this.description = builder.description;
        this.link = builder.link;
        this.title = builder.title;
        this.query = builder.query;
        this.type = Objects.requireNonNull(builder.type, "Feed type must not be null");
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<String> getLink() {
        return Optional.ofNullable(link);
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public Optional<String> getQuery() {
        return Optional.ofNullable(query);
    }

    @Override
    public Optional<String> getSite() {
        return Optional.ofNullable(site);
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    @Override
    public Optional<String> getType() {
        return Optional.ofNullable(type);
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
        private String query;
        private String type;

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

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public PSFeedDescriptor build() {
            return new PSFeedDescriptor(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSFeedDescriptor that = (PSFeedDescriptor) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(site, that.site) &&
               Objects.equals(description, that.description) &&
               Objects.equals(link, that.link) &&
               Objects.equals(title, that.title) &&
               Objects.equals(query, that.query) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, site, description, link, title, query, type);
    }

    @Override
    public String toString() {
        return "PSFeedDescriptor{" +
               "name='" + name + '\'' +
               ", site='" + site + '\'' +
               ", description='" + description + '\'' +
               ", link='" + link + '\'' +
               ", title='" + title + '\'' +
               ", query='" + query + '\'' +
               ", type='" + type + '\'' +
               '}';
    }
}
