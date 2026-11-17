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

import { describe, expect, it } from "vitest";
import {
  describeSiteHistoryCount,
  siteHistoryExportFilename,
  slugifySiteName,
  SITE_HISTORY_FALLBACK_SLUG,
} from "@/publishing/siteHistory";

describe("slugifySiteName", () => {
  it("lowercases and dashes a plain site name", () => {
    expect(slugifySiteName("FastForward")).toBe("fastforward");
  });

  it("collapses spaces and punctuation to single dashes", () => {
    expect(slugifySiteName("My Site (East)!")).toBe("my-site-east");
  });

  it("keeps digits, dashes, and underscores", () => {
    expect(slugifySiteName("Site_42-prod")).toBe("site_42-prod");
  });

  it("falls back for blank or punctuation-only names", () => {
    expect(slugifySiteName("")).toBe(SITE_HISTORY_FALLBACK_SLUG);
    expect(slugifySiteName("   ")).toBe(SITE_HISTORY_FALLBACK_SLUG);
    expect(slugifySiteName("!!!")).toBe(SITE_HISTORY_FALLBACK_SLUG);
    expect(slugifySiteName(null)).toBe(SITE_HISTORY_FALLBACK_SLUG);
    expect(slugifySiteName(undefined)).toBe(SITE_HISTORY_FALLBACK_SLUG);
  });
});

describe("siteHistoryExportFilename", () => {
  it("names the CSV per site slug", () => {
    expect(siteHistoryExportFilename("FastForward")).toBe(
      "publish-history-fastforward.csv",
    );
  });

  it("falls back when the site name is blank", () => {
    expect(siteHistoryExportFilename("")).toBe(
      `publish-history-${SITE_HISTORY_FALLBACK_SLUG}.csv`,
    );
  });
});

describe("describeSiteHistoryCount", () => {
  it("reports visible of total rows", () => {
    expect(describeSiteHistoryCount(1, 3)).toBe("Showing 1 of 3 history rows");
    expect(describeSiteHistoryCount(0, 0)).toBe("Showing 0 of 0 history rows");
  });
});
