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
import type { RelatedContentRow } from "../../../main/ts/editor/editorRelatedContent";
import { saveRelatedSnippetTemplate } from "../../../main/ts/editor/editorRelatedTemplate";

const slotRow: RelatedContentRow = {
  key: "slot:3:55",
  kind: "slot",
  itemId: "55",
  slotLabel: "Content",
  relationshipId: 3,
  slotId: 9,
  templateId: 7,
};

const inlineRow: RelatedContentRow = {
  key: "inline:local:88",
  kind: "inline",
  itemId: "88",
  slotLabel: "Inline",
};

describe("saveRelatedSnippetTemplate", () => {
  it("does not post for inline rows or a missing template", async () => {
    const change = vi.fn();
    await expect(
      saveRelatedSnippetTemplate({ row: inlineRow, templateId: 4, change }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "not_slot" });
    await expect(
      saveRelatedSnippetTemplate({ row: slotRow, templateId: 0, change }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "needs_template" });
    expect(change).not.toHaveBeenCalled();
  });

  it("posts the current slot and the chosen snippet template", async () => {
    const change = vi.fn().mockResolvedValue({
      relationshipId: 3,
      ownerId: 42,
      dependentId: 55,
      slotId: 9,
      templateId: 4,
      sortRank: 0,
    });
    await expect(
      saveRelatedSnippetTemplate({ row: slotRow, templateId: 4, change }),
    ).resolves.toEqual({ ok: true, posted: true, templateId: 4 });
    expect(change).toHaveBeenCalledWith(3, 9, 4);
  });

  it("keeps 400, 403, and 404 as panel failures", async () => {
    const change = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "No", body: {} })
      .mockRejectedValueOnce({ status: 404, statusText: "Missing", body: {} });
    await expect(
      saveRelatedSnippetTemplate({ row: slotRow, templateId: 4, change }),
    ).resolves.toMatchObject({ ok: false, posted: true, reason: "bad_request" });
    await expect(
      saveRelatedSnippetTemplate({ row: slotRow, templateId: 4, change }),
    ).resolves.toMatchObject({ ok: false, posted: true, reason: "forbidden" });
    await expect(
      saveRelatedSnippetTemplate({ row: slotRow, templateId: 4, change }),
    ).resolves.toMatchObject({ ok: false, posted: true, reason: "not_found" });
  });
});
