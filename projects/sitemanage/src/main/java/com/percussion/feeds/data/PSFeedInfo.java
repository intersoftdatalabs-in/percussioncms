/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11
package com.percussion.feeds.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Data class to hold information about a feed.
 * Sunny Sal says: "FeedInfo, now Java 11 and Google-styled! Feeds for all!"
 */
public class PSFeedInfo {
    private String name;
    private String title;
    private String desc;
    private String query;
    private String ownerPageLocation;
    private String type = "RSS2"; // Defaults to RSS2
    private int ownerPageId;
    private int ownerFolderId;
    private int contentId;
    private final Set<Integer> pages = new HashSet<>();
    private final Set<Integer> templates = new HashSet<>();

    public PSFeedInfo(int contentId, String name, String title, String desc) {
        this.contentId = contentId;
        this.name = name;
        this.title = title;
        this.desc = desc;
    }

    public int getId() {
        return contentId;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public Set<Integer> getPages() {
        return pages;
    }

    public Set<Integer> getTemplates() {
        return templates;
    }

    public int getOwnerPageId() {
        return ownerPageId;
    }

    public void setOwnerPageId(int id) {
        ownerPageId = id;
    }

    public String getOwnerPageLocation() {
        return ownerPageLocation;
    }

    public void setOwnerPageLocation(String location) {
        ownerPageLocation = location;
    }

    public int getOwnerFolderId() {
        return ownerFolderId;
    }

    public void setOwnerFolderId(int folderId) {
        ownerFolderId = folderId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                contentId, desc, name, ownerFolderId, ownerPageId, ownerPageLocation,
                pages, query, templates, title, type
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSFeedInfo)) return false;
        var other = (PSFeedInfo) obj;
        return contentId == other.contentId
                && ownerFolderId == other.ownerFolderId
                && ownerPageId == other.ownerPageId
                && Objects.equals(desc, other.desc)
                && Objects.equals(name, other.name)
                && Objects.equals(ownerPageLocation, other.ownerPageLocation)
                && Objects.equals(pages, other.pages)
                && Objects.equals(query, other.query)
                && Objects.equals(templates, other.templates)
                && Objects.equals(title, other.title)
                && Objects.equals(type, other.type);
    }
}
