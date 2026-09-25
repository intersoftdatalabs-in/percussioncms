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
import {
  canPreviewFromEditor,
  editorChosenTemplateFrameUrl,
  editorDefaultPreviewFrameUrl,
  editorDraftIsDirty,
  editorPreviewPathItem,
  numericPreviewTemplates,
  positivePreviewTemplateId,
  previewEditorItem,
} from "../../../main/ts/editor/editorPreview";

describe("editorPreviewPathItem", () => {
  it("classifies pages and assets for Explorer preview", () => {
    expect(editorPreviewPathItem("42", "page", "Home")).toEqual({
      id: "42",
      name: "Home",
      path: "/Sites/42",
      type: "page",
    });
    expect(editorPreviewPathItem("99", "asset")).toEqual({
      id: "99",
      name: "99",
      path: "/Assets/99",
      type: "asset",
    });
  });
});

describe("canPreviewFromEditor", () => {
  it("allows view and edit for page or asset, not promote", () => {
    expect(canPreviewFromEditor("edit", "page")).toBe(true);
    expect(canPreviewFromEditor("view", "asset")).toBe(true);
    expect(canPreviewFromEditor("promote", "page")).toBe(false);
    expect(canPreviewFromEditor("edit", "none")).toBe(false);
    expect(canPreviewFromEditor("view", "none")).toBe(false);
  });
});

describe("editorDraftIsDirty", () => {
  it("is false when draft matches saved values", () => {
    expect(
      editorDraftIsDirty(
        [{ name: "sys_title", value: "Home" }],
        { sys_title: "Home" },
        {},
      ),
    ).toBe(false);
  });

  it("is true when a field changed or a file is pending", () => {
    expect(
      editorDraftIsDirty(
        [{ name: "sys_title", value: "Home" }],
        { sys_title: "Updated" },
        {},
      ),
    ).toBe(true);
    expect(
      editorDraftIsDirty(
        [{ name: "sys_title", value: "Home" }],
        { sys_title: "Home" },
        { img: new File(["x"], "x.png") },
      ),
    ).toBe(true);
  });
});

describe("previewEditorItem", () => {
  it("probes page render then opens the assembled URL", async () => {
    const probeUrl = vi.fn().mockResolvedValue(undefined);
    const openWindow = vi.fn().mockReturnValue({});
    await previewEditorItem(
      "42",
      "page",
      { probeUrl, openWindow, servicesRoot: "/Rhythmyx/services" },
      "Home",
    );
    expect(probeUrl).toHaveBeenCalled();
    const probed = String(probeUrl.mock.calls[0]?.[0] ?? "");
    expect(probed).toContain("/pagemanagement/render/page/42");
    expect(openWindow).toHaveBeenCalled();
    const opened = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(opened).toContain("/pagemanagement/render/page/42");
  });

  it("does not open when page probe is forbidden", async () => {
    const probeUrl = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { Error: { message: "FORBIDDEN" } },
    });
    const openWindow = vi.fn();
    await expect(
      previewEditorItem("42", "page", {
        probeUrl,
        openWindow,
        servicesRoot: "/Rhythmyx/services",
      }),
    ).rejects.toMatchObject({ status: 403 });
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("does not treat unknown id as success", async () => {
    const probeUrl = vi.fn().mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: {},
    });
    const openWindow = vi.fn();
    await expect(
      previewEditorItem("0", "page", {
        probeUrl,
        openWindow,
        servicesRoot: "/Rhythmyx/services",
      }),
    ).rejects.toMatchObject({ status: 404 });
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("fetches asset view URL then opens the body", async () => {
    const fetchText = vi.fn().mockResolvedValue("/Rhythmyx/assembler/render?sys_contentid=99");
    const openWindow = vi.fn().mockReturnValue({});
    await previewEditorItem("99", "asset", {
      fetchText,
      openWindow,
      servicesRoot: "/Rhythmyx/services",
    });
    expect(fetchText).toHaveBeenCalled();
    const req = String(fetchText.mock.calls[0]?.[0] ?? "");
    expect(req).toContain("/assetmanagement/asset/assetViewUrl/99");
    expect(openWindow).toHaveBeenCalledWith(
      "/Rhythmyx/assembler/render?sys_contentid=99",
      expect.stringMatching(/percAssetPreview_/),
    );
  });

  it("throws without opening when kind or id is missing", async () => {
    const openWindow = vi.fn();
    await expect(previewEditorItem("", "page", { openWindow })).rejects.toThrow(
      /not available/i,
    );
    await expect(previewEditorItem("42", "none", { openWindow })).rejects.toThrow(
      /not available/i,
    );
    expect(openWindow).not.toHaveBeenCalled();
  });
});

describe("editor preview template panel (#4841)", () => {
  it("keeps only positive integer template ids", () => {
    expect(positivePreviewTemplateId("7")).toBe(7);
    expect(positivePreviewTemplateId("0")).toBeNull();
    expect(positivePreviewTemplateId("abc")).toBeNull();
    expect(positivePreviewTemplateId("")).toBeNull();
    expect(
      numericPreviewTemplates([
        { id: "7", name: "Home" },
        { id: "guid-not-int", name: "Named" },
        { id: "7", name: "Home again" },
        { id: "8", name: "Blog" },
      ]),
    ).toEqual([
      { id: "7", name: "Home" },
      { id: "8", name: "Blog" },
    ]);
  });

  it("uses page render for the current template and assembler URL for a choice", () => {
    expect(editorDefaultPreviewFrameUrl("42", "page")).toContain(
      "/pagemanagement/render/page/42",
    );
    expect(editorDefaultPreviewFrameUrl("99", "asset")).toBe("");
    expect(
      editorChosenTemplateFrameUrl(
        "/assembler/render?sys_contentid=42&sys_template=7&sys_revision=1",
      ),
    ).toContain("sys_template=7");
    expect(() => editorChosenTemplateFrameUrl("/services/not-preview")).toThrow(
      /assembler/i,
    );
  });
});
