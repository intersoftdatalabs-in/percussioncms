/**
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
 * Tests for jfeed-shim.js
 *
 * Verifies that the native DOMParser-based JFeed constructor produces the same
 * object shape that PercRssView.js expects from the original jquery.jfeed
 * library.  Covers RSS 2.0, Atom 1.0, and graceful handling of bad input.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../../..");

// Load and evaluate the shim — JFeed and jfeedText land on the global scope
const shimCode = readFileSync(
  resolve(ROOT, "src/main/js/shims/jfeed-shim.js"),
  "utf8",
);
// In ESM strict mode, direct eval() scopes function declarations locally.
// Appending "; JFeed" causes eval to return the declared function, while
// jfeedText remains reachable via closure when JFeed is invoked.
// eslint-disable-next-line no-eval
const JFeed = eval(shimCode + "\n; JFeed");

// ---------------------------------------------------------------------------
// RSS 2.0 fixtures
// ---------------------------------------------------------------------------
const RSS_SINGLE_ITEM = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Perc Blog</title>
    <link>https://www.example.com/blog</link>
    <description>Latest posts from the Perc blog</description>
    <item>
      <title>First Post</title>
      <link>https://www.example.com/blog/first-post</link>
      <description>This is the first post.</description>
      <pubDate>Wed, 26 Feb 2026 10:00:00 +0000</pubDate>
      <guid>https://www.example.com/blog/first-post</guid>
    </item>
  </channel>
</rss>`;

const RSS_MULTIPLE_ITEMS = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Feed</title>
    <link>https://example.com</link>
    <description>A feed</description>
    <item>
      <title>Post A</title>
      <link>https://example.com/a</link>
      <description>Summary A</description>
      <pubDate>Mon, 01 Jan 2024 00:00:00 +0000</pubDate>
      <guid>guid-a</guid>
    </item>
    <item>
      <title>Post B</title>
      <link>https://example.com/b</link>
      <description>Summary B</description>
      <pubDate>Tue, 02 Jan 2024 00:00:00 +0000</pubDate>
      <guid>guid-b</guid>
    </item>
  </channel>
</rss>`;

const RSS_MISSING_FIELDS = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Minimal</title>
    <item>
      <title>Bare item</title>
    </item>
  </channel>
</rss>`;

// ---------------------------------------------------------------------------
// Atom 1.0 fixtures
// ---------------------------------------------------------------------------
const ATOM_SINGLE_ENTRY = `<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Perc Atom Feed</title>
  <subtitle>News from Percussion</subtitle>
  <link rel="alternate" href="https://www.example.com/atom"/>
  <entry>
    <title>Atom Entry One</title>
    <link rel="alternate" href="https://www.example.com/atom/1"/>
    <summary>Summary of entry one.</summary>
    <updated>2026-02-26T10:00:00Z</updated>
    <id>urn:uuid:atom-entry-1</id>
  </entry>
</feed>`;

const ATOM_CONTENT_ELEMENT = `<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Content Feed</title>
  <link href="https://example.com"/>
  <entry>
    <title>Entry with content</title>
    <link href="https://example.com/entry"/>
    <content>Full content of the entry.</content>
    <updated>2026-01-01T00:00:00Z</updated>
    <id>urn:uuid:content-entry</id>
  </entry>
</feed>`;

const ATOM_MULTIPLE_ENTRIES = `<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Multi Feed</title>
  <link href="https://example.com/multi"/>
  <entry>
    <title>Entry A</title>
    <link href="https://example.com/a"/>
    <summary>A</summary>
    <updated>2024-01-01T00:00:00Z</updated>
    <id>id-a</id>
  </entry>
  <entry>
    <title>Entry B</title>
    <link href="https://example.com/b"/>
    <summary>B</summary>
    <updated>2024-01-02T00:00:00Z</updated>
    <id>id-b</id>
  </entry>
</feed>`;

// ---------------------------------------------------------------------------
// RSS 2.0 tests
// ---------------------------------------------------------------------------
describe("JFeed — RSS 2.0", () => {
  describe("channel-level fields", () => {
    it("parses the feed title", () => {
      const feed = new JFeed(RSS_SINGLE_ITEM);
      expect(feed.title).toBe("Perc Blog");
    });

    it("parses the feed link", () => {
      const feed = new JFeed(RSS_SINGLE_ITEM);
      expect(feed.link).toBe("https://www.example.com/blog");
    });

    it("parses the feed description", () => {
      const feed = new JFeed(RSS_SINGLE_ITEM);
      expect(feed.description).toBe("Latest posts from the Perc blog");
    });
  });

  describe("single <item>", () => {
    it("produces exactly one item", () => {
      const feed = new JFeed(RSS_SINGLE_ITEM);
      expect(feed.items).toHaveLength(1);
    });

    it("parses item title", () => {
      const { items } = new JFeed(RSS_SINGLE_ITEM);
      expect(items[0].title).toBe("First Post");
    });

    it("parses item link", () => {
      const { items } = new JFeed(RSS_SINGLE_ITEM);
      expect(items[0].link).toBe("https://www.example.com/blog/first-post");
    });

    it("parses item description", () => {
      const { items } = new JFeed(RSS_SINGLE_ITEM);
      expect(items[0].description).toBe("This is the first post.");
    });

    it("maps pubDate to item.updated", () => {
      const { items } = new JFeed(RSS_SINGLE_ITEM);
      expect(items[0].updated).toBe("Wed, 26 Feb 2026 10:00:00 +0000");
    });

    it("maps guid to item.id", () => {
      const { items } = new JFeed(RSS_SINGLE_ITEM);
      expect(items[0].id).toBe("https://www.example.com/blog/first-post");
    });
  });

  describe("multiple <item> elements", () => {
    it("parses all items", () => {
      const feed = new JFeed(RSS_MULTIPLE_ITEMS);
      expect(feed.items).toHaveLength(2);
    });

    it("preserves item order", () => {
      const { items } = new JFeed(RSS_MULTIPLE_ITEMS);
      expect(items[0].title).toBe("Post A");
      expect(items[1].title).toBe("Post B");
    });

    it("parses each item's guid independently", () => {
      const { items } = new JFeed(RSS_MULTIPLE_ITEMS);
      expect(items[0].id).toBe("guid-a");
      expect(items[1].id).toBe("guid-b");
    });
  });

  describe("missing optional fields", () => {
    it("returns empty string for missing item link", () => {
      const { items } = new JFeed(RSS_MISSING_FIELDS);
      expect(items[0].link).toBe("");
    });

    it("returns empty string for missing item description", () => {
      const { items } = new JFeed(RSS_MISSING_FIELDS);
      expect(items[0].description).toBe("");
    });

    it("returns empty string for missing item pubDate", () => {
      const { items } = new JFeed(RSS_MISSING_FIELDS);
      expect(items[0].updated).toBe("");
    });

    it("returns empty string for missing item guid", () => {
      const { items } = new JFeed(RSS_MISSING_FIELDS);
      expect(items[0].id).toBe("");
    });

    it("returns empty string for missing channel link", () => {
      const feed = new JFeed(RSS_MISSING_FIELDS);
      expect(feed.link).toBe("");
    });

    it("returns empty string for missing channel description", () => {
      const feed = new JFeed(RSS_MISSING_FIELDS);
      expect(feed.description).toBe("");
    });
  });
});

// ---------------------------------------------------------------------------
// Atom 1.0 tests
// ---------------------------------------------------------------------------
describe("JFeed — Atom 1.0", () => {
  describe("feed-level fields", () => {
    it("parses the feed title", () => {
      const feed = new JFeed(ATOM_SINGLE_ENTRY);
      expect(feed.title).toBe("Perc Atom Feed");
    });

    it("maps <subtitle> to feed.description", () => {
      const feed = new JFeed(ATOM_SINGLE_ENTRY);
      expect(feed.description).toBe("News from Percussion");
    });

    it("parses the alternate link href as feed.link", () => {
      const feed = new JFeed(ATOM_SINGLE_ENTRY);
      expect(feed.link).toBe("https://www.example.com/atom");
    });
  });

  describe("single <entry>", () => {
    it("produces exactly one item", () => {
      const feed = new JFeed(ATOM_SINGLE_ENTRY);
      expect(feed.items).toHaveLength(1);
    });

    it("parses entry title", () => {
      const { items } = new JFeed(ATOM_SINGLE_ENTRY);
      expect(items[0].title).toBe("Atom Entry One");
    });

    it("parses entry link from href attribute", () => {
      const { items } = new JFeed(ATOM_SINGLE_ENTRY);
      expect(items[0].link).toBe("https://www.example.com/atom/1");
    });

    it("maps <summary> to item.description", () => {
      const { items } = new JFeed(ATOM_SINGLE_ENTRY);
      expect(items[0].description).toBe("Summary of entry one.");
    });

    it("maps <updated> to item.updated", () => {
      const { items } = new JFeed(ATOM_SINGLE_ENTRY);
      expect(items[0].updated).toBe("2026-02-26T10:00:00Z");
    });

    it("maps <id> to item.id", () => {
      const { items } = new JFeed(ATOM_SINGLE_ENTRY);
      expect(items[0].id).toBe("urn:uuid:atom-entry-1");
    });
  });

  describe("<content> element", () => {
    it("prefers <content> over <summary> for item.description", () => {
      const { items } = new JFeed(ATOM_CONTENT_ELEMENT);
      expect(items[0].description).toBe("Full content of the entry.");
    });
  });

  describe("multiple <entry> elements", () => {
    it("parses all entries", () => {
      const feed = new JFeed(ATOM_MULTIPLE_ENTRIES);
      expect(feed.items).toHaveLength(2);
    });

    it("preserves entry order", () => {
      const { items } = new JFeed(ATOM_MULTIPLE_ENTRIES);
      expect(items[0].title).toBe("Entry A");
      expect(items[1].title).toBe("Entry B");
    });
  });
});

// ---------------------------------------------------------------------------
// Edge cases
// ---------------------------------------------------------------------------
describe("JFeed — edge cases", () => {
  it("returns empty feed for an unrecognised root element", () => {
    const feed = new JFeed('<?xml version="1.0"?><unknown><foo/></unknown>');
    expect(feed.title).toBe("");
    expect(feed.description).toBe("");
    expect(feed.link).toBe("");
    expect(feed.items).toHaveLength(0);
  });

  it("returns empty feed for an empty string", () => {
    const feed = new JFeed("");
    expect(feed.items).toHaveLength(0);
  });

  it("always exposes items as an array", () => {
    const feed = new JFeed("");
    expect(Array.isArray(feed.items)).toBe(true);
  });
});
