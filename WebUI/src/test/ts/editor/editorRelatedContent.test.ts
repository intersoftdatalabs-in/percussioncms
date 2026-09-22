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
import { RelationshipSummaryAuthError } from "../../../main/ts/api/contentExplorer/relationshipsApi";
import {
  flattenRelatedContent,
  insertSlotChoices,
  relatedContentErrorReason,
  relatedInsertErrorReason,
} from "../../../main/ts/editor/editorRelatedContent";

describe("flattenRelatedContent", () => {
  it("returns empty when canvas and local have no items", () => {
    expect(
      flattenRelatedContent({ ownerId: 1, templateId: null, slots: [] }, {
        count: 0,
        links: [],
      }),
    ).toEqual([]);
  });

  it("lists slot dependents and inline local links", () => {
    const rows = flattenRelatedContent(
      {
        ownerId: 42,
        templateId: 7,
        slots: [
          {
            slotId: 9,
            name: "percSystem",
            label: "Content",
            items: [
              {
                relationshipId: 100,
                ownerId: 42,
                dependentId: 55,
                slotId: 9,
                templateId: 3,
                sortRank: 0,
              },
            ],
          },
        ],
      },
      { count: 1, links: [{ type: "local", targetId: "88" }] },
    );
    expect(rows.map((r) => r.itemId)).toEqual(["55", "88"]);
    expect(rows[0].kind).toBe("slot");
    expect(rows[1].kind).toBe("inline");
  });
});

describe("relatedContentErrorReason", () => {
  it("maps 403 and RelationshipSummaryAuthError to forbidden", () => {
    expect(relatedContentErrorReason({ status: 403, statusText: "F", body: {} })).toBe(
      "forbidden",
    );
    expect(
      relatedContentErrorReason(
        new RelationshipSummaryAuthError("no", 403, "Forbidden"),
      ),
    ).toBe("forbidden");
  });

  it("maps insert 400, 403, and 404 and leaves other statuses failed", () => {
    expect(relatedInsertErrorReason({ status: 400, statusText: "B", body: {} })).toBe(
      "bad_request",
    );
    expect(relatedInsertErrorReason({ status: 403, statusText: "F", body: {} })).toBe(
      "forbidden",
    );
    expect(relatedInsertErrorReason({ status: 404, statusText: "N", body: {} })).toBe(
      "not_found",
    );
    expect(relatedInsertErrorReason(new Error("boom"))).toBe("failed");
  });

  it("offers canvas slots and a template id for insert", () => {
    expect(
      insertSlotChoices({
        ownerId: 42,
        templateId: 7,
        slots: [
          { slotId: 9, name: "percSystem", label: "Content", items: [] },
          { slotId: 0, name: "skip", label: "Skip", items: [] },
        ],
      }),
    ).toEqual([{ slotId: 9, templateId: 7, label: "Content" }]);
  });

  it("uses an existing item template and skips slots with no template", () => {
    expect(
      insertSlotChoices({
        ownerId: 1,
        templateId: null,
        slots: [
          {
            slotId: 2,
            name: "bare",
            label: "Bare",
            items: [],
          },
          {
            slotId: 3,
            name: "filled",
            label: "Filled",
            items: [
              {
                relationshipId: 1,
                ownerId: 1,
                dependentId: 8,
                slotId: 3,
                templateId: 4,
                sortRank: 0,
              },
            ],
          },
        ],
      }),
    ).toEqual([{ slotId: 3, templateId: 4, label: "Filled" }]);
  });

  it("maps other errors to failed", () => {
    expect(relatedContentErrorReason(new Error("boom"))).toBe("failed");
    expect(relatedContentErrorReason({ status: 500, statusText: "x", body: {} })).toBe(
      "failed",
    );
  });
});
