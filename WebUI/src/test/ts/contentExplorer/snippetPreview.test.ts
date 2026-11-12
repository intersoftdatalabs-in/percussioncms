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
  canPreviewSnippetRow,
  openSnippetPreview,
  snippetPreviewWindowName,
} from "../../../main/ts/contentExplorer/snippetPreview";

const LOCATION = {
  previewUrl:
    "/assembler/render?sys_contentid=55&sys_template=4&sys_context=0&sys_itemfilter=preview",
  contentId: 55,
  templateId: 4,
  revision: 1,
};

describe("snippetPreview", () => {
  it("gates on positive content and template ids", () => {
    expect(canPreviewSnippetRow(55, 4)).toBe(true);
    expect(canPreviewSnippetRow(55, 0)).toBe(false);
    expect(canPreviewSnippetRow(55, undefined)).toBe(false);
    expect(canPreviewSnippetRow(0, 4)).toBe(false);
    expect(canPreviewSnippetRow("55", "4")).toBe(true);
    expect(canPreviewSnippetRow(NaN, 4)).toBe(false);
    expect(canPreviewSnippetRow(1.5, 4)).toBe(false);
  });

  it("names one popup window per snippet", () => {
    expect(snippetPreviewWindowName(55)).toBe("percSnippetPreview_55");
  });

  it("opens the assembler render from preview-location", async () => {
    const fetchPreview = vi.fn().mockResolvedValue(LOCATION);
    const openWindow = vi.fn().mockReturnValue(null);
    await openSnippetPreview(
      { contentId: 55, templateId: 4 },
      { fetchPreview, openWindow },
    );
    expect(fetchPreview).toHaveBeenCalledWith(55, 4);
    expect(openWindow).toHaveBeenCalledWith(
      expect.stringContaining("/assembler/render"),
      "percSnippetPreview_55",
    );
  });

  it("throws without a positive content id or template id", async () => {
    const fetchPreview = vi.fn();
    const openWindow = vi.fn();
    await expect(
      openSnippetPreview(
        { contentId: 55, templateId: 0 },
        { fetchPreview, openWindow },
      ),
    ).rejects.toThrow();
    expect(fetchPreview).not.toHaveBeenCalled();
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("rejects a non-assembler preview location instead of opening it", async () => {
    const fetchPreview = vi
      .fn()
      .mockResolvedValue({ ...LOCATION, previewUrl: "/Rhythmyx/login" });
    const openWindow = vi.fn();
    await expect(
      openSnippetPreview(
        { contentId: 55, templateId: 4 },
        { fetchPreview, openWindow },
      ),
    ).rejects.toThrow(/assembler/);
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("propagates a failed preview-location lookup", async () => {
    const fetchPreview = vi.fn().mockRejectedValue(new Error("nope"));
    const openWindow = vi.fn();
    await expect(
      openSnippetPreview(
        { contentId: 55, templateId: 4 },
        { fetchPreview, openWindow },
      ),
    ).rejects.toThrow("nope");
    expect(openWindow).not.toHaveBeenCalled();
  });
});
