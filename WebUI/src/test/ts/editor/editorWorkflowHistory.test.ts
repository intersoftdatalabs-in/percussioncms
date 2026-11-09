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
import type { ItemRevisionsSummary } from "../../../main/ts/api/contentExplorer/itemRevisionsApi";
import {
  canViewWorkflowHistoryFromEditor,
  summarizeWorkflowHistoryRow,
  workflowHistoryEntries,
} from "../../../main/ts/editor/editorWorkflowHistory";

function summaryWith(
  comments: ItemRevisionsSummary["comments"],
): ItemRevisionsSummary {
  return { restorable: false, revisions: [], comments };
}

describe("editorWorkflowHistory (#5829)", () => {
  it("is available in edit and view, but not in promote", () => {
    expect(canViewWorkflowHistoryFromEditor("edit")).toBe(true);
    expect(canViewWorkflowHistoryFromEditor("view")).toBe(true);
    expect(canViewWorkflowHistoryFromEditor("promote")).toBe(false);
  });

  it("returns no entries for a missing summary", () => {
    expect(workflowHistoryEntries(null)).toEqual([]);
    expect(workflowHistoryEntries(undefined)).toEqual([]);
  });

  it("keeps recorded rows and drops fully blank rows", () => {
    const entries = workflowHistoryEntries(
      summaryWith([
        {
          comment: "Looks good",
          commenter: "admin",
          commentType: "Approve",
          commentDate: "2026-04-12",
        },
        { comment: "", commenter: "", commentType: "", commentDate: "" },
      ]),
    );
    expect(entries).toHaveLength(1);
    expect(entries[0].commenter).toBe("admin");
  });

  it("keeps a row with empty text when date, user, or type is present", () => {
    const entries = workflowHistoryEntries(
      summaryWith([
        { comment: "", commenter: "admin", commentType: "", commentDate: "" },
      ]),
    );
    expect(entries).toHaveLength(1);
  });

  it("summarizes a row with date, user, type, and comment", () => {
    expect(
      summarizeWorkflowHistoryRow({
        comment: "Looks good",
        commenter: "admin",
        commentType: "Approve",
        commentDate: "2026-04-12",
      }),
    ).toBe("2026-04-12 — admin — Approve — Looks good");
  });

  it("summarizes a sparse row from whatever fields are present", () => {
    expect(
      summarizeWorkflowHistoryRow({
        comment: "",
        commenter: "admin",
        commentType: "",
        commentDate: "",
      }),
    ).toBe("admin");
    expect(
      summarizeWorkflowHistoryRow(
        { comment: "", commenter: "", commentType: "", commentDate: "" },
        "none",
      ),
    ).toBe("none");
  });
});
