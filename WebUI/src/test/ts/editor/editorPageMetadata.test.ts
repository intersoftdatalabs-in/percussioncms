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
import type { EditorFieldRow } from "../../../main/ts/editor/controlKinds";
import {
  appendSanitizedSummaryHtml,
  applyPageFilenameFilter,
  defaultPageMetadataOpenGroups,
  groupPageMetadataRows,
  isAutoSummaryOn,
  PAGE_METADATA_GROUP_DEFS,
  pageMetadataGroupsWithErrors,
  parsePageSiteImpact,
  siteImpactSiteFromPath,
  withPageFilenameField,
} from "../../../main/ts/editor/editorPageMetadata";

function row(name: string, value = ""): EditorFieldRow {
  return {
    name,
    value,
    label: name,
    readOnly: false,
    kind: "text",
    required: false,
  };
}

describe("page metadata dialog chrome parity (#5739)", () => {
  it("groups page fields in classic dialog order with the first group open", () => {
    const grouped = groupPageMetadataRows([
      row("page_calendar"),
      row("sys_title", "Home"),
      row("page_title"),
      row("displaytitle"),
    ]);
    expect(grouped.sections.map((section) => section.id)).toEqual(
      PAGE_METADATA_GROUP_DEFS.map((def) => def.id),
    );
    expect(grouped.sections[0].rows.map((entry) => entry.name)).toEqual([
      "sys_title",
    ]);
    expect(grouped.sections[1].rows.map((entry) => entry.name)).toEqual([
      "page_title",
    ]);
    expect(grouped.sections[3].rows.map((entry) => entry.name)).toEqual([
      "page_calendar",
    ]);
    expect(grouped.ungrouped.map((entry) => entry.name)).toEqual([
      "displaytitle",
    ]);
  });

  it("matches field names case-insensitively and keeps dialog field order", () => {
    const grouped = groupPageMetadataRows([
      row("SYS_TITLE", "Home"),
      row("Resource_Link_Title", "Home"),
    ]);
    expect(grouped.sections[0].rows.map((entry) => entry.name)).toEqual([
      "Resource_Link_Title",
      "SYS_TITLE",
    ]);
  });

  it("opens only the first group by default", () => {
    const open = defaultPageMetadataOpenGroups();
    expect(open["page-summary"]).toBe(true);
    expect(
      PAGE_METADATA_GROUP_DEFS.filter((def) => def.id !== "page-summary").every(
        (def) => open[def.id] === false,
      ),
    ).toBe(true);
  });

  it("expands groups holding field errors", () => {
    const grouped = groupPageMetadataRows([
      row("sys_title"),
      row("page_title"),
    ]);
    expect(
      pageMetadataGroupsWithErrors(grouped.sections, {
        page_title: "required",
      }),
    ).toEqual(["seo"]);
    expect(pageMetadataGroupsWithErrors(grouped.sections, {})).toEqual([]);
  });

  it("treats only yes as auto-summary on", () => {
    expect(isAutoSummaryOn("yes")).toBe(true);
    expect(isAutoSummaryOn("YES")).toBe(true);
    expect(isAutoSummaryOn("no")).toBe(false);
    expect(isAutoSummaryOn("")).toBe(false);
    expect(isAutoSummaryOn(null)).toBe(false);
    expect(isAutoSummaryOn(undefined)).toBe(false);
  });

  it("trims and strips trailing dots from the filename like WINDOWS_FILE_NAME", () => {
    expect(applyPageFilenameFilter("  Home...  ")).toBe("Home");
    expect(applyPageFilenameFilter("Home")).toBe("Home");
    expect(applyPageFilenameFilter("  ")).toBe("");
    expect(applyPageFilenameFilter(null)).toBe("");
  });

  it("filters only sys_title on save", () => {
    const next = withPageFilenameField([
      { name: "sys_title", value: "  Home... " },
      { name: "page_title", value: "  Home page... " },
    ]);
    expect(next).toEqual([
      { name: "sys_title", value: "Home" },
      { name: "page_title", value: "  Home page... " },
    ]);
  });

  it("sanitizes the auto-summary preview without script or event markup", () => {
    const host = document.createElement("div");
    appendSanitizedSummaryHtml(
      host,
      '<p>Hello <a href="javascript:alert(1)">link</a></p><script>alert(2)</script><img src="x" onerror="alert(3)">',
    );
    expect(host.querySelector("script")).toBeNull();
    expect(host.querySelector("[onerror]")).toBeNull();
    expect(host.querySelector("a")?.getAttribute("href")).toBeNull();
    expect(host.textContent).toContain("Hello");
    expect(host.textContent).toContain("link");
  });

  it("leaves the preview host empty for blank summaries", () => {
    const host = document.createElement("div");
    host.textContent = "stale";
    appendSanitizedSummaryHtml(host, "   ");
    expect(host.textContent).toBe("");
  });

  it("derives the impact site from the classic /Sites/ path shape", () => {
    expect(siteImpactSiteFromPath("/Sites/Demo/Home")).toBe("Demo");
    expect(siteImpactSiteFromPath("/Sites/Demo")).toBe("Demo");
    expect(siteImpactSiteFromPath("")).toBe("");
  });

  it("parses bare and legacy-wrapped site-impact payloads", () => {
    const bare = parsePageSiteImpact({
      pages: [
        {
          id: "7",
          name: "Home",
          path: "/Sites/Demo/Home",
          status: "Live",
          type: "Page",
        },
      ],
      templates: [{ template: { name: "Base" }, site: "Demo" }],
    });
    expect(bare.pages).toEqual([
      {
        id: "7",
        name: "Home",
        path: "/Sites/Demo/Home",
        site: "Demo",
        status: "Live",
        type: "Page",
      },
    ]);
    expect(bare.templates).toEqual([{ name: "Base", site: "Demo" }]);

    const wrapped = parsePageSiteImpact({
      data: { pages: [], templates: [] },
    });
    expect(wrapped).toEqual({ pages: [], templates: [] });
  });

  it("parses unknown site-impact shapes as empty instead of throwing", () => {
    expect(parsePageSiteImpact(null)).toEqual({ pages: [], templates: [] });
    expect(parsePageSiteImpact("not json")).toEqual({
      pages: [],
      templates: [],
    });
    expect(parsePageSiteImpact({ pages: [{}, null] })).toEqual({
      pages: [],
      templates: [],
    });
  });
});
