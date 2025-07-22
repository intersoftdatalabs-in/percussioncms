// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds;

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedItem;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generates RSS and Atom feeds using the ROME tools library.
 */
public final class PSFeedGenerator {

    /**
     * Creates a feed in the specified format with the given items.
     *
     * @param descriptor The feed descriptor containing metadata
     * @param host The host name to use in feed URLs
     * @param items The items to include in the feed
     * @return The generated feed as a string
     * @throws FeedException if feed generation fails
     */
    public String makeFeedContent(IPSFeedDescriptor descriptor, String host, List<PSFeedItem> items)
            throws FeedException {
        Objects.requireNonNull(descriptor, "Feed descriptor must not be null");
        Objects.requireNonNull(host, "Host must not be null");
        Objects.requireNonNull(items, "Items list must not be null");

        var feed = new SyndFeedImpl();
        feed.setFeedType(getFeedType(descriptor));

        descriptor.getTitle().ifPresent(feed::setTitle);
        descriptor.getDescription().ifPresent(feed::setDescription);
        descriptor.getLink()
            .map(link -> fixupHost(link, host))
            .ifPresent(feed::setLink);

        feed.setPublishedDate(Date.from(Instant.now()));

        var entries = items.stream()
            .map(this::createEntry)
            .collect(Collectors.toList());
        feed.setEntries(entries);

        return new SyndFeedOutput().outputString(feed);
    }

    private SyndEntry createEntry(PSFeedItem item) {
        var entry = new SyndEntryImpl();
        entry.setTitle(item.getTitle());

        item.getDescription().ifPresent(desc -> {
            var description = new SyndContentImpl();
            description.setType("text/html");
            description.setValue(desc);
            entry.setDescription(description);
        });

        entry.setLink(item.getLink());
        entry.setPublishedDate(Date.from(item.getPublishDate()));
        return entry;
    }

    /**
     * Replaces the host name in the link with the supplied host.
     *
     * @param link Original link
     * @param host New host to use
     * @return Updated link with new host
     * @throws FeedException if URL processing fails
     */
    private String fixupHost(String link, String host) throws FeedException {
        try {
            var currentHost = getHost(link);
            return StringUtils.replace(link, currentHost, host, 1);
        } catch (FeedException e) {
            throw new FeedException("Failed to process feed URL", e);
        }
    }

    /**
     * Extracts the host component from a URL.
     *
     * @param link URL to process
     * @return Host component of the URL
     * @throws FeedException if URL parsing fails
     */
    private static String getHost(String link) throws FeedException {
        try {
            return Optional.ofNullable(link)
                .map(URI::create)
                .map(URI::getHost)
                .orElseThrow(() -> new FeedException("Invalid feed URL: missing host"));
        } catch (IllegalArgumentException e) {
            throw new FeedException("Invalid feed URL format", e);
        }
    }

    /**
     * Determines the appropriate feed type based on the descriptor.
     *
     * @param descriptor Feed descriptor
     * @return Feed type string for ROME
     */
    private String getFeedType(IPSFeedDescriptor descriptor) {
        return switch (descriptor.getType().toUpperCase()) {
            case "ATOM" -> "atom_1.0";
            case "RSS1" -> "rss_1.0";
            case "RSS2" -> "rss_2.0";
            default -> throw new IllegalArgumentException("Unsupported feed type: " + descriptor.getType());
        };
    }
}
