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

import { describe, expect, it, vi } from "vitest";
import type { ApiError } from "../../../main/ts/api/client";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";
import {
  classifyChangePageTemplateSelection,
  gateExplorerPageTemplateChange,
  loadChangePageTemplateCatalog,
  pageTemplateShownAfterSave,
  saveChangePageTemplate,
} from "../../../main/ts/contentExplorer/changePageTemplate";

function httpError(status: number): ApiError {
  return { status, statusText: String(status), body: null };
}

const page: PSPathItem = {
  id: "16777215-101-551",
  name: "Home",
  path: "/Sites/CI/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

const folder: PSPathItem = {
  id: "1",
  name: "CI",
  path: "/Sites/CI/",
  type: "folder",
  category: "folder",
  leaf: false,
};

const asset: PSPathItem = {
  id: "16777215-101-900",
  name: "Logo",
  path: "/Assets/uploads/logo.png",
  type: "percImage",
  category: "asset",
  leaf: true,
};

const choices = [
  { id: "101", name: "Article" },
  { id: "202", name: "Blog" },
];

function fields(templateId: string): ItemEditorFields {
  return {
    contentId: "551",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "",
    fields: [{ name: "templateid", value: templateId }],
  };
}

describe("change page template (#5200)", () => {
  it("blocks empty, folder, asset, and multi-select before any load", async () => {
    expect(
      classifyChangePageTemplateSelection({ item: null, selectedCount: 0 }).status,
    ).toBe("blocked");
    expect(
      classifyChangePageTemplateSelection({ item: folder, selectedCount: 1 }),
    ).toMatchObject({ status: "blocked", reason: "folder", name: "CI" });
    expect(
      classifyChangePageTemplateSelection({ item: asset, selectedCount: 1 }),
    ).toMatchObject({ status: "blocked", reason: "asset", name: "Logo" });
    expect(
      classifyChangePageTemplateSelection({ item: page, selectedCount: 2 }),
    ).toMatchObject({ reason: "multi" });

    const loadFields = vi.fn();
    const loaded = await loadChangePageTemplateCatalog({
      item: asset,
      selectedCount: 1,
      loadFields,
    });
    expect(loaded).toMatchObject({ status: "blocked", reason: "asset" });
    expect(loadFields).not.toHaveBeenCalled();
  });

  it("loads the stored template and the editor template choices", async () => {
    const loadFields = vi.fn(async () => fields("101"));
    const loadTemplates = vi.fn(async () => choices);
    const catalog = await loadChangePageTemplateCatalog({
      item: page,
      selectedCount: 1,
      loadFields,
      loadTemplates,
    });
    expect(catalog).toEqual({
      status: "ready",
      itemId: "16777215-101-551",
      currentId: "101",
      choices,
    });
    expect(loadTemplates).toHaveBeenCalledWith("/Sites/CI", "percPage");
  });

  it("refuses a page that has no template choices", async () => {
    const catalog = await loadChangePageTemplateCatalog({
      item: page,
      selectedCount: 1,
      loadFields: async () => fields(""),
      loadTemplates: async () => [],
    });
    expect(catalog).toEqual({ status: "none" });
  });

  it("keeps the catalog closed when fields return HTTP 403", async () => {
    const catalog = await loadChangePageTemplateCatalog({
      item: page,
      selectedCount: 1,
      loadFields: async () => {
        throw httpError(403);
      },
      loadTemplates: async () => choices,
    });
    expect(catalog).toEqual({ status: "http", http: 403 });
  });

  it("refuses the current template and an id that is not listed", () => {
    expect(
      gateExplorerPageTemplateChange({
        selectedId: "101",
        currentId: "101",
        allowedIds: ["101", "202"],
      }),
    ).toEqual({ ok: false, reason: "unchanged" });
    expect(
      gateExplorerPageTemplateChange({
        selectedId: "999",
        currentId: "101",
        allowedIds: ["101", "202"],
      }),
    ).toEqual({ ok: false, reason: "forbidden" });
    expect(
      gateExplorerPageTemplateChange({
        selectedId: "  ",
        currentId: "101",
        allowedIds: ["101", "202"],
      }),
    ).toEqual({ ok: false, reason: "blank" });
    expect(
      gateExplorerPageTemplateChange({
        selectedId: "202",
        currentId: "101",
        allowedIds: ["101", "202"],
      }),
    ).toEqual({ ok: true, templateId: "202" });
  });

  it("saves through the editor changeTemplate call", async () => {
    const change = vi.fn(async () => undefined);
    const saved = await saveChangePageTemplate({
      itemId: "16777215-101-551",
      selectedId: "202",
      currentId: "101",
      allowedIds: ["101", "202"],
      change,
    });
    expect(saved).toEqual({ status: "saved", templateId: "202" });
    expect(change).toHaveBeenCalledWith("16777215-101-551", "202");
  });

  it("does not call changeTemplate when the choice is unchanged", async () => {
    const change = vi.fn(async () => undefined);
    const saved = await saveChangePageTemplate({
      itemId: "551",
      selectedId: "101",
      currentId: "101",
      allowedIds: ["101", "202"],
      change,
    });
    expect(saved).toEqual({ status: "gate", reason: "unchanged" });
    expect(change).not.toHaveBeenCalled();
  });

  it("keeps the previous template on HTTP 400, 403, and 409", async () => {
    const previous = { templateId: "101", templateName: "Article" };
    for (const status of [400, 403, 409] as const) {
      const change = vi.fn(async () => {
        throw httpError(status);
      });
      const saved = await saveChangePageTemplate({
        itemId: "551",
        selectedId: "202",
        currentId: "101",
        allowedIds: ["101", "202"],
        change,
      });
      expect(saved).toEqual({ status: "http", http: status });
      expect(pageTemplateShownAfterSave(previous, saved, "Blog")).toEqual(previous);
    }
    const saved = { status: "saved" as const, templateId: "202" };
    expect(pageTemplateShownAfterSave(previous, saved, "Blog")).toEqual({
      templateId: "202",
      templateName: "Blog",
    });
    expect(pageTemplateShownAfterSave(undefined, saved, "Blog")?.templateName).toBe(
      "Blog",
    );
  });
});
