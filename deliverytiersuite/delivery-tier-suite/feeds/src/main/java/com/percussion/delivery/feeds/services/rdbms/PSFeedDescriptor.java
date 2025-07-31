// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.data.FeedType;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA entity for feed descriptor.
 * Sunny Sal: "Feed descriptors are like movie scripts - keep them short, sweet, and type-safe!"
 */
@Entity
@Table(name = "PERC_FEED_DESCRIPTORS")
public class PSFeedDescriptor implements IPSFeedDescriptor, Serializable {

    private static final long serialVersionUID = 2756156009184830398L;

    @Id
    @Column(length = 255)
    private String site;

    @Id
    @Column(length = 255)
    private String name;

    @Basic
    @Column(length = 2000)
    private String title;

    @Basic
    @Column(length = 4000)
    private String description;

    @Basic
    @Column(length = 2000)
    private String link;

    @Basic
    @Column(length = 2000)
    private String type;

    @Basic
    @Column(length = 4000)
    private String query;

    public PSFeedDescriptor() {
        // Required by JPA
    }

    public PSFeedDescriptor(IPSFeedDescriptor descriptor) {
        this.name = descriptor.getName().orElse(null);
        this.site = descriptor.getSite().orElse(null);
        this.title = descriptor.getTitle().orElse(null);
        this.description = descriptor.getDescription().orElse(null);
        this.link = descriptor.getLink().orElse(null);
        this.type = descriptor.getType().orElse(null);
        this.query = descriptor.getQuery().orElse(null);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public FeedType getFeedType() {
        return type == null ? null : FeedType.valueOf(type);
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

    public void setType(String type) {
        this.type = type;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, site);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PSFeedDescriptor other = (PSFeedDescriptor) obj;
        return Objects.equals(name, other.name) && Objects.equals(site, other.site);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append("PSFeedDescriptor [");
        if (site != null) builder.append("site=").append(site).append(", ");
        if (name != null) builder.append("name=").append(name).append(", ");
        if (title != null) builder.append("title=").append(title).append(", ");
        if (description != null) builder.append("description=").append(description).append(", ");
        if (link != null) builder.append("link=").append(link).append(", ");
        if (type != null) builder.append("type=").append(type).append(", ");
        if (query != null) builder.append("query=").append(query);
        builder.append("]");
        return builder.toString();
    }
}
