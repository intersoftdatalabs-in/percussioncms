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

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  buildRestoreRevisionId,
  fetchItemRevisions,
  restoreItemRevision,
  unwrapRevisionsSummary,
} from "../../../../main/ts/api/contentExplorer/itemRevisionsApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("itemRevisionsApi", () => {
  it("buildRestoreRevisionId replaces the GUID revision segment", () => {
    expect(buildRestoreRevisionId("1-101-708", 3)).toBe("3-101-708");
    expect(buildRestoreRevisionId("42", 3)).toBe("3-101-42");
  });

  it("unwraps a single Revision object as a one-element list", () => {
    const summary = unwrapRevisionsSummary({
      RevisionsSummary: {
        restorable: true,
        revisions: {
          revId: 1,
          lastModifiedDate: "d",
          lastModifier: "u",
          status: "Draft",
        },
        comments: {
          comment: "c",
          commenter: "u",
          commentType: "Approve",
          commentDate: "d",
        },
      },
    });
    expect(summary.revisions).toHaveLength(1);
    expect(summary.revisions[0]?.revId).toBe(1);
    expect(summary.comments).toHaveLength(1);
    expect(summary.comments[0]?.comment).toBe("c");
  });

  it("treats missing revisions and comments as empty lists", () => {
    const summary = unwrapRevisionsSummary({ restorable: false });
    expect(summary.revisions).toEqual([]);
    expect(summary.comments).toEqual([]);
  });

  it("unwraps RevisionsSummary envelope", () => {
    const summary = unwrapRevisionsSummary({
      RevisionsSummary: {
        restorable: true,
        revisions: [
          {
            revId: 2,
            lastModifiedDate: "d",
            lastModifier: "u",
            status: "Live",
          },
        ],
        comments: [
          {
            comment: "c",
            commenter: "u",
            commentType: "Approve",
            commentDate: "d",
          },
        ],
      },
    });
    expect(summary.restorable).toBe(true);
    expect(summary.revisions).toHaveLength(1);
    expect(summary.comments[0]?.comment).toBe("c");
  });

  it("fetchItemRevisions GETs the revisions path", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          restorable: false,
          revisions: [],
          comments: [],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const summary = await fetchItemRevisions("1-101-42");
    expect(summary.restorable).toBe(false);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "itemmanagement/item/revisions/1-101-42",
    );
  });

  it("restoreItemRevision GETs the revision-encoded GUID", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await restoreItemRevision("1-101-42", 2);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "itemmanagement/item/restoreRevision/2-101-42",
    );
  });
});
