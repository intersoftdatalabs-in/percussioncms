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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.feeds.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.feeds.PSFeedGenerator;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for issue #6556: {@code generateFeed} must skip metadata index rows with a
 * missing/null title instead of 500ing the whole feed.
 *
 * <p>Covers {@link PSFeedService#toFeedItem} directly (same package) plus a pair-wise mapping that
 * mirrors the {@code generateFeed} loop: a title-less row beside a good row yields only the good
 * entry.
 */
@DisplayName("PSFeedService null-title row skip (issue #6556)")
class PSFeedServiceNullTitleSkipTest {

  private static JSONObject goodRow() {
    JSONObject props = new JSONObject();
    props.put(IPSFeedsRestService.PROP_TITLE, "Good entry");
    props.put(
        IPSFeedsRestService.PROP_DESCRIPTION, "<p>Hello <img src=\"/assets/a.png\"></p>");
    props.put(IPSFeedsRestService.PROP_PUBDATE, "2026-11-10T09:30:00");
    JSONObject row = new JSONObject();
    row.put("folder", "/site/");
    row.put("name", "page1");
    row.put("properties", props);
    return row;
  }

  private static JSONObject titlelessRow() {
    // Stale stringvalue-shape seeds carry an explicit JSON null title (issue #6556).
    JSONObject props = new JSONObject();
    props.put(IPSFeedsRestService.PROP_TITLE, JSONObject.NULL);
    props.put(IPSFeedsRestService.PROP_DESCRIPTION, "orphan description");
    JSONObject row = new JSONObject();
    row.put("folder", "/site/");
    row.put("name", "orphan");
    row.put("properties", props);
    return row;
  }

  @Test
  @DisplayName("explicit JSON-null title row is skipped")
  void testNullTitleRowSkipped() throws Exception {
    Optional<PSFeedItem> mapped = PSFeedService.toFeedItem(titlelessRow(), "https://", "example.com");
    assertTrue(mapped.isEmpty(), "Row with JSONObject.NULL title must be skipped");
  }

  @Test
  @DisplayName("missing title key row is skipped")
  void testMissingTitleRowSkipped() throws Exception {
    JSONObject row = goodRow();
    row.getJSONObject("properties").remove(IPSFeedsRestService.PROP_TITLE);
    Optional<PSFeedItem> mapped = PSFeedService.toFeedItem(row, "https://", "example.com");
    assertTrue(mapped.isEmpty(), "Row without a title key must be skipped");
  }

  @Test
  @DisplayName("blank title row is skipped")
  void testBlankTitleRowSkipped() throws Exception {
    JSONObject row = goodRow();
    row.getJSONObject("properties").put(IPSFeedsRestService.PROP_TITLE, "   ");
    Optional<PSFeedItem> mapped = PSFeedService.toFeedItem(row, "https://", "example.com");
    assertTrue(mapped.isEmpty(), "Row with a blank title must be skipped");
  }

  @Test
  @DisplayName("good row maps title, link, and rewritten description")
  void testGoodRowMapped() throws Exception {
    Optional<PSFeedItem> mapped = PSFeedService.toFeedItem(goodRow(), "https://", "example.com");
    assertTrue(mapped.isPresent(), "Good row must map to an item");
    PSFeedItem item = mapped.get();
    assertEquals("Good entry", item.getTitle());
    assertEquals("https://example.com/site/page1", item.getLink());
    assertTrue(
        item.getDescription().contains("https://example.com/assets/a.png"),
        "Relative description links must be rewritten, got: " + item.getDescription());
  }

  @Test
  @DisplayName("null description and publish date are tolerated")
  void testNullDescriptionAndPubDateTolerated() throws Exception {
    JSONObject row = goodRow();
    JSONObject props = row.getJSONObject("properties");
    props.put(IPSFeedsRestService.PROP_DESCRIPTION, JSONObject.NULL);
    props.put(IPSFeedsRestService.PROP_PUBDATE, JSONObject.NULL);
    Optional<PSFeedItem> mapped = PSFeedService.toFeedItem(row, "https://", "example.com");
    assertTrue(mapped.isPresent(), "Row with null description/pubdate must still map");
    assertNull(mapped.get().getDescription());
    assertNull(mapped.get().getPublishDate());
  }

  @Test
  @DisplayName("title-less row beside a good row yields only the good entry")
  void testPairYieldsOnlyGoodEntry() throws Exception {
    // Mirrors the generateFeed loop: per-row mapping with fail-single-row semantics.
    List<JSONObject> rows = List.of(titlelessRow(), goodRow());
    List<PSFeedItem> items = new ArrayList<>();
    for (JSONObject row : rows) {
      try {
        Optional<PSFeedItem> mapped = PSFeedService.toFeedItem(row, "https://", "example.com");
        mapped.ifPresent(items::add);
      } catch (Exception e) {
        // fail the single row, never the whole feed
      }
    }
    assertEquals(1, items.size(), "Only the good row must survive");
    assertEquals("Good entry", items.get(0).getTitle());

    String feed =
        new PSFeedGenerator().makeFeedContent(new TestDescriptor(), "example.com", items);
    assertTrue(feed.contains("Good entry"), "Feed must contain the good entry");
    assertTrue(!feed.contains("orphan"), "Feed must not contain the skipped row");
  }

  /** Minimal descriptor for feed rendering assertions. */
  static class TestDescriptor implements IPSFeedDescriptor {

    @Override
    public String getDescription() {
      return "Test feed description.";
    }

    @Override
    public String getType() {
      return "RSS2";
    }

    @Override
    public String getLink() {
      return "http://www.testme.com/feed";
    }

    @Override
    public String getName() {
      return "Test Feed";
    }

    @Override
    public String getQuery() {
      return null;
    }

    @Override
    public String getSite() {
      return "TestSite";
    }

    @Override
    public String getTitle() {
      return "Test Feed";
    }
  }
}
