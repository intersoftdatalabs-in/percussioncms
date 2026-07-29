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
package com.percussion.delivery.feeds;

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedItem;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Generates Atom/RSS feed XML for a feed descriptor and a list of feed items, using the configured
 * host name as the feed link host.
 *
 * @author erikserating
 */
public class PSFeedGenerator {

  /** Default constructor. */
  public PSFeedGenerator() {
    // default constructor
  }

  /**
   * Renders the supplied descriptor and items as feed XML using the given host.
   *
   * @param desc the feed descriptor, never <code>null</code>
   * @param host the host name used to build the feed's links, never <code>null</code>
   * @param items the feed items to include, never <code>null</code>
   * @return the feed XML as a string, never <code>null</code>
   * @throws FeedException if the feed XML cannot be produced
   */
  public String makeFeedContent(IPSFeedDescriptor desc, String host, List<PSFeedItem> items)
      throws FeedException {
    Objects.requireNonNull(desc, "desc cannot be null");
    Objects.requireNonNull(host, "host cannot be null");
    Objects.requireNonNull(items, "items cannot be null");

    SyndFeed feed = new SyndFeedImpl();
    feed.setFeedType(getFeedType(desc));
    feed.setTitle(desc.getTitle());
    feed.setDescription(desc.getDescription());
    feed.setLink(fixupHost(desc.getLink(), host));
    feed.setPublishedDate(new Date());

    // Process each item
    SyndEntry entry;
    SyndContent description;
    List<SyndEntry> entries = new ArrayList<>();
    for (PSFeedItem item : items) {
      entry = new SyndEntryImpl();
      entry.setTitle(item.getTitle());
      if (StringUtils.isNotBlank(item.getDescription())) {
        description = new SyndContentImpl();
        description.setType("text/html");
        description.setValue(item.getDescription());
        entry.setDescription(description);
      }
      entry.setLink(item.getLink());
      entry.setPublishedDate(item.getPublishDate());
      entries.add(entry);
    }
    feed.setEntries(entries);

    SyndFeedOutput output = new SyndFeedOutput();
    return output.outputString(feed);
  }

  /**
   * Replaces the host name in the link with the supplied host.
   *
   * @param link the original link, never <code>null</code>
   * @param host the replacement host, never <code>null</code>
   * @return the link with the host replaced, never <code>null</code>
   * @throws FeedException if the link cannot be parsed
   */
  private String fixupHost(String link, String host) throws FeedException {
    String curHost = getHost(link);
    return StringUtils.replace(link, curHost, host, 1);
  }

  /**
   * Extracts the host (and port, if present) from the supplied link.
   *
   * @param link the link to parse, never <code>null</code>
   * @return the host portion of the link (with port if present), never <code>null</code>
   * @throws FeedException if the link cannot be parsed
   */
  public static String getHost(String link) throws FeedException {
    try {
      URI uri = new URI(link);
      String host = uri.getHost();
      int port = uri.getPort();
      if (port != -1) host += ":" + port;
      return host;
    } catch (URISyntaxException e) {
      String error = "Failed to parse host from feed descriptor link: " + link;
      throw new FeedException(error);
    }
  }

  /**
   * Helper method to return the proper rome feed type string for the feed type set on the passed in
   * descriptor.
   *
   * @param desc assumed not <code>null</code>.
   * @return the feed type string, never <code>null</code> or empty.
   */
  private String getFeedType(IPSFeedDescriptor desc) {
    switch (desc.getType()) {
      case "ATOM":
        return "atom_1.0";
      case "RSS1":
        return "rss_1.0";
      default:
        return "rss_2.0";
    }
  }
}
