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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { EditorFieldRow } from "../../../main/ts/editor/controlKinds";
import {
  defaultPageMetadataOpenGroups,
  groupPageMetadataRows,
} from "../../../main/ts/editor/editorPageMetadata";
import {
  EditorPageMetadataPanel,
  PageSummaryPreview,
} from "../../../main/ts/editor/EditorPageMetadataPanel";
import { EditorSiteImpactPanel } from "../../../main/ts/editor/EditorSiteImpactPanel";

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

describe("page metadata groups panel (#5739)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders the first group open and the rest collapsed with toggles", () => {
    const grouped = groupPageMetadataRows([
      row("sys_title", "Home"),
      row("page_title", "Home page"),
    ]);
    const renderRow = (entry: EditorFieldRow): React.ReactNode => (
      <div data-testid={`stub-row-${entry.name}`} />
    );
    render(
      <EditorPageMetadataPanel
        sections={grouped.sections}
        ungrouped={grouped.ungrouped}
        openGroups={defaultPageMetadataOpenGroups()}
        onToggleGroup={() => {}}
        renderRow={renderRow}
      />,
    );
    expect(screen.getByTestId("editor-metadata-group-page-summary")).toBeTruthy();
    expect(screen.getByTestId("stub-row-sys_title")).toBeTruthy();
    expect(screen.queryByTestId("stub-row-page_title")).toBeNull();
    const seoToggle = screen.getByTestId("editor-metadata-group-toggle-seo");
    expect(seoToggle.getAttribute("aria-expanded")).toBe("false");
    expect(
      screen
        .getByTestId("editor-metadata-group-toggle-page-summary")
        .getAttribute("aria-expanded"),
    ).toBe("true");
  });

  it("toggles a collapsed group open and shows the post-date note", () => {
    const grouped = groupPageMetadataRows([
      row("sys_contentpostdate", "2026-01-01"),
      row("page_title", "Home page"),
    ]);
    const onToggleGroup = vi.fn();
    const renderRow = (entry: EditorFieldRow): React.ReactNode => (
      <div data-testid={`stub-row-${entry.name}`} />
    );
    const open = defaultPageMetadataOpenGroups();
    const { rerender } = render(
      <EditorPageMetadataPanel
        sections={grouped.sections}
        ungrouped={grouped.ungrouped}
        openGroups={open}
        onToggleGroup={onToggleGroup}
        renderRow={renderRow}
      />,
    );
    fireEvent.click(screen.getByTestId("editor-metadata-group-toggle-seo"));
    expect(onToggleGroup).toHaveBeenCalledWith("seo");

    rerender(
      <EditorPageMetadataPanel
        sections={grouped.sections}
        ungrouped={grouped.ungrouped}
        openGroups={{ ...open, seo: true }}
        onToggleGroup={onToggleGroup}
        renderRow={renderRow}
      />,
    );
    expect(screen.getByTestId("stub-row-page_title")).toBeTruthy();
    expect(screen.getByTestId("editor-metadata-postdate-note")).toBeTruthy();
  });

  it("renders the auto-summary preview sanitized and read-only", () => {
    render(
      <PageSummaryPreview
        html={'<p>Preview<script>alert(1)</script></p><img src="x" onerror="alert(2)">'}
      />,
    );
    const preview = screen.getByTestId("editor-metadata-summary-preview");
    expect(preview.querySelector("script")).toBeNull();
    expect(preview.querySelector("[onerror]")).toBeNull();
    expect(preview.textContent).toContain("Preview");
    expect(preview.getAttribute("role")).toBe("region");
  });
});

describe("page site impact panel (#5739)", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders impacting pages and templates", async () => {
    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => ({
          pages: [
            {
              id: "7",
              name: "Home",
              path: "/Sites/Demo/Home",
              site: "Demo",
              status: "Live",
              type: "Page",
            },
          ],
          templates: [{ name: "Base", site: "Demo" }],
        })}
      />,
    );
    expect(
      await screen.findByTestId("editor-site-impact-pages-table"),
    ).toBeTruthy();
    expect(screen.getByTestId("editor-site-impact-page-row").textContent).toContain(
      "Home",
    );
    expect(
      await screen.findByTestId("editor-site-impact-templates-table"),
    ).toBeTruthy();
  });

  it("renders empty and error states", async () => {
    const { unmount } = render(
      <EditorSiteImpactPanel itemId="42" loadSiteImpact={async () => ({ pages: [], templates: [] })} />,
    );
    expect(await screen.findByTestId("editor-site-impact-empty")).toBeTruthy();
    expect(
      screen.getByTestId("editor-site-impact-templates-empty"),
    ).toBeTruthy();
    unmount();

    render(
      <EditorSiteImpactPanel
        itemId="42"
        loadSiteImpact={async () => {
          throw new Error("boom");
        }}
      />,
    );
    expect(await screen.findByTestId("editor-site-impact-error")).toBeTruthy();
  });
});
