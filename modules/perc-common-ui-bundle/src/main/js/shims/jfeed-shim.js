/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

/**
 * JFeed Compatibility Shim
 *
 * Replaces the unmaintained jquery.jfeed library with a native DOMParser
 * implementation that exposes the same object shape expected by PercRssView.js.
 *
 * Supported formats:
 *   - RSS 2.0  (root element: <rss>, items: <item>)
 *   - Atom 1.0 (root element: <feed>, items: <entry>, links use href attribute)
 *
 * Usage (unchanged from jquery.jfeed):
 *   var feed = new JFeed(xmlString);
 *   feed.title       // channel/feed title
 *   feed.link        // channel/feed link
 *   feed.description // channel/feed description (RSS) or subtitle (Atom)
 *   feed.items       // array of feed item objects
 *
 * Each item in feed.items exposes:
 *   item.title
 *   item.link
 *   item.description
 *   item.updated
 *   item.id
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/API/DOMParser
 */

/**
 * Helper: return trimmed text content of the first matching element,
 * or an empty string if no element is found.
 *
 * @param {Element} parent  - DOM element to search within
 * @param {string}  tagName - local tag name to look up
 * @returns {string}
 */
function jfeedText(parent, tagName) {
  var el = parent.getElementsByTagName(tagName)[0];
  return el ? (el.textContent || el.innerText || "").trim() : "";
}

/**
 * Parse an RSS or Atom XML string into a feed object.
 *
 * @constructor
 * @param {string} xmlString - Raw XML text for the feed.
 */
function JFeed(xmlString) {
  var parser = new DOMParser();
  var doc = parser.parseFromString(xmlString, "text/xml");

  var root = doc.documentElement;
  var rootTag = root ? root.nodeName : "";

  this.title = "";
  this.link = "";
  this.description = "";
  this.items = [];

  if (rootTag === "rss" || rootTag === "rdf:RDF") {
    // ----- RSS 2.0 / RSS 1.0 -----
    var channel = root.getElementsByTagName("channel")[0];
    if (channel) {
      this.title = jfeedText(channel, "title");
      this.description = jfeedText(channel, "description");

      // <link> may contain CDATA or a text node; prefer the direct child
      var linkEl = channel.getElementsByTagName("link")[0];
      this.link = linkEl
        ? (linkEl.textContent || linkEl.innerText || "").trim()
        : "";
    }

    var items = root.getElementsByTagName("item");
    for (var i = 0; i < items.length; i++) {
      var item = items[i];
      this.items.push({
        title: jfeedText(item, "title"),
        link: jfeedText(item, "link"),
        description: jfeedText(item, "description"),
        updated: jfeedText(item, "pubDate") || jfeedText(item, "dc:date"),
        id: jfeedText(item, "guid"),
      });
    }
  } else if (rootTag === "feed") {
    // ----- Atom 1.0 -----
    this.title = jfeedText(root, "title");
    this.description = jfeedText(root, "subtitle");

    // Atom links use <link href="..."/> rather than text content
    var feedLinks = root.childNodes;
    for (var n = 0; n < feedLinks.length; n++) {
      var node = feedLinks[n];
      if (
        node.nodeName === "link" &&
        (!node.getAttribute("rel") || node.getAttribute("rel") === "alternate")
      ) {
        this.link = node.getAttribute("href") || "";
        break;
      }
    }

    var entries = root.getElementsByTagName("entry");
    for (var e = 0; e < entries.length; e++) {
      var entry = entries[e];

      // Atom item link is also an attribute
      var entryLink = "";
      var entryLinks = entry.getElementsByTagName("link");
      for (var l = 0; l < entryLinks.length; l++) {
        var rel = entryLinks[l].getAttribute("rel");
        if (!rel || rel === "alternate") {
          entryLink = entryLinks[l].getAttribute("href") || "";
          break;
        }
      }

      this.items.push({
        title: jfeedText(entry, "title"),
        link: entryLink,
        description: jfeedText(entry, "content") || jfeedText(entry, "summary"),
        updated: jfeedText(entry, "updated") || jfeedText(entry, "published"),
        id: jfeedText(entry, "id"),
      });
    }
  }
}
