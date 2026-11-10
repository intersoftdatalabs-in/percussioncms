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

import { describe, expect, it } from "vitest";
import {
  DETAIL_CELL_RENDERERS,
  DETAIL_COLUMN_IDS,
  lookupDisplayProperty,
  renderCategoryCell,
  renderDisplayFormatCell,
  renderModifiedCell,
  renderNameCell,
  renderPathCell,
  renderTitleCell,
  renderTypeCell,
  renderWorkflowCell,
  type DetailColumnId,
} from "../../../main/ts/contentExplorer/detailCellRenderers";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

const sample: PSPathItem = {
  id: "p-1",
  name: "Welcome",
  path: "/Sites/Foo/Welcome",
  type: "page",
  title: "Welcome Page",
  category: "page",
  lastModified: "2026-07-01",
  workflowId: "wf-default",
};

const EXPECTED: Record<DetailColumnId, string> = {
  name: "Welcome",
  type: "page",
  path: "/Sites/Foo/Welcome",
  title: "Welcome Page",
  category: "page",
  modified: "2026-07-01",
  workflow: "wf-default",
};

const PER_COLUMN: Record<DetailColumnId, (item: PSPathItem) => string> = {
  name: renderNameCell,
  type: renderTypeCell,
  path: renderPathCell,
  title: renderTitleCell,
  category: renderCategoryCell,
  modified: renderModifiedCell,
  workflow: renderWorkflowCell,
};

describe("detailCellRenderers (#5777)", () => {
  it("registers one renderer per column id", () => {
    expect(DETAIL_COLUMN_IDS).toEqual([
      "name",
      "type",
      "path",
      "title",
      "category",
      "modified",
      "workflow",
    ]);
    expect(Object.keys(DETAIL_CELL_RENDERERS).sort()).toEqual(
      [...DETAIL_COLUMN_IDS].sort(),
    );
  });

  it("each per-column renderer matches the dispatcher contract", () => {
    for (const column of DETAIL_COLUMN_IDS) {
      expect(PER_COLUMN[column](sample)).toBe(EXPECTED[column]);
      expect(DETAIL_CELL_RENDERERS[column](sample)).toBe(EXPECTED[column]);
      expect(renderDisplayFormatCell(column, sample)).toBe(EXPECTED[column]);
    }
  });

  it("dispatcher resolves unknown ids to empty string", () => {
    expect(
      renderDisplayFormatCell("bogus" as unknown as DetailColumnId, sample),
    ).toBe("");
  });

  it("tolerates null optional fields", () => {
    const minimal: PSPathItem = { id: "x", name: "X", path: "/x" };
    expect(renderTitleCell(minimal)).toBe("X");
    expect(renderDisplayFormatCell("title", minimal)).toBe("X");
    expect(renderModifiedCell(minimal)).toBe("");
    expect(renderDisplayFormatCell("modified", minimal)).toBe("");
  });

  it("prefers displayProperties with case-insensitive fallback", () => {
    const withProps: PSPathItem = {
      ...sample,
      displayProperties: { SYS_TITLE: "From Props" },
    };
    expect(lookupDisplayProperty(withProps, ["sys_title"])).toBe("From Props");
    expect(renderNameCell(withProps)).toBe("From Props");
    expect(lookupDisplayProperty(sample, ["missing"])).toBe("");
    expect(lookupDisplayProperty({ ...sample, displayProperties: undefined }, ["sys_title"])).toBe(
      "",
    );
  });

  it("site folder rows expose finder name, not GUID path (#3684)", () => {
    const site: PSPathItem = {
      id: "16777215-101-703",
      path: "/Sites/16777215-101-703/",
      folderPath: "//Sites/CorporateInvestments",
      name: "Corporate_Investments",
      type: "site",
      leaf: false,
      hasFolderChildren: true,
      displayProperties: { sys_title: "16777215-101-703" },
    };
    expect(renderNameCell(site)).toBe("Corporate_Investments");
    expect(renderDisplayFormatCell("name", site)).toBe("Corporate_Investments");
  });
});
