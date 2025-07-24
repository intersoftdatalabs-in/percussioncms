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

import java.util.Date;

/**
 * @author erikserating
 *
 */
public class PSFeedItem {
    private String title;
    private String description;
    private Date publishDate;
    private String link;

    public PSFeedItem() {}

    public PSFeedItem(String title, String description, Date publishDate, String link) {
        this.title = title;
        this.description = description;
        this.publishDate = publishDate;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Date getPublishDate() {
        return publishDate;
    }
    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    @Override
    public String toString() {
        return String.format("PSFeedItem[title=%s, description=%s, publishDate=%s, link=%s]", title, description, publishDate, link);
    }
}
