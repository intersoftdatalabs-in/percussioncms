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
import {
  canChangeEditorWorkflow,
  canRunEditorTransition,
  confirmEditorAssignees,
  editorAssigneeStep,
  editorTransitionHttpFailure,
  normalizeAdhocAssignees,
  triggerRequiresAssignees,
  triggerRequiresComment,
  isAllowedTransitionTrigger,
  uniqueTransitionTriggers,
} from "../../../main/ts/editor/editorWorkflow";

describe("editorWorkflow (#4539)", () => {
  it("uniqueTransitionTriggers trims, drops blanks, and de-dupes", () => {
    expect(uniqueTransitionTriggers(["Submit", " Submit ", "", "Reject", "Submit"])).toEqual([
      "Submit",
      "Reject",
    ]);
    expect(uniqueTransitionTriggers(null)).toEqual([]);
  });

  it("isAllowedTransitionTrigger only matches loaded names", () => {
    expect(isAllowedTransitionTrigger("Submit", ["Submit", "Reject"])).toBe(true);
    expect(isAllowedTransitionTrigger("Approve", ["Submit"])).toBe(false);
    expect(isAllowedTransitionTrigger("  ", ["Submit"])).toBe(false);
  });

  it("triggerRequiresComment matches reject-style names case-insensitively", () => {
    expect(triggerRequiresComment("Reject")).toBe(true);
    expect(triggerRequiresComment("reject")).toBe(true);
    expect(triggerRequiresComment("Send Back")).toBe(true);
    expect(triggerRequiresComment("Submit")).toBe(false);
    expect(triggerRequiresComment("Approve")).toBe(false);
    expect(triggerRequiresComment("Custom", ["Custom"])).toBe(true);
  });

  it("canRunEditorTransition blocks view, unauthorized, and empty required comments", () => {
    expect(
      canRunEditorTransition({
        mode: "view",
        trigger: "Submit",
        allowed: ["Submit"],
        comment: "",
      }),
    ).toEqual({ ok: false, reason: "readonly" });
    expect(
      canRunEditorTransition({
        mode: "promote",
        trigger: "Submit",
        allowed: ["Submit"],
        comment: "",
      }),
    ).toEqual({ ok: false, reason: "readonly" });
    expect(
      canRunEditorTransition({
        mode: "edit",
        trigger: "Approve",
        allowed: ["Submit"],
        comment: "ok",
      }),
    ).toEqual({ ok: false, reason: "unauthorized" });
    expect(
      canRunEditorTransition({
        mode: "edit",
        trigger: "Reject",
        allowed: ["Reject"],
        comment: "   ",
      }),
    ).toEqual({ ok: false, reason: "comment" });
    expect(
      canRunEditorTransition({
        mode: "edit",
        trigger: "Submit",
        allowed: ["Submit"],
        comment: "",
      }),
    ).toEqual({ ok: true });
    expect(
      canRunEditorTransition({
        mode: "edit",
        trigger: "Reject",
        allowed: ["Reject"],
        comment: "needs work",
      }),
    ).toEqual({ ok: true });
  });

  it("normalizeAdhocAssignees trims and de-dupes (#5163)", () => {
    expect(normalizeAdhocAssignees(null)).toEqual([]);
    expect(normalizeAdhocAssignees([" alice ", "bob", "alice", "  ", "bob"])).toEqual([
      "alice",
      "bob",
    ]);
  });

  it("triggerRequiresAssignees matches the loaded list exactly (#5163)", () => {
    expect(triggerRequiresAssignees("Submit", ["Submit"])).toBe(true);
    expect(triggerRequiresAssignees("submit", ["Submit"])).toBe(false);
    expect(triggerRequiresAssignees("Reject", ["Submit"])).toBe(false);
    expect(triggerRequiresAssignees("Submit", null)).toBe(false);
  });

  it("editorAssigneeStep confirms only when assignees are required (#5163)", () => {
    expect(
      editorAssigneeStep({ requiresAssignees: false, assignees: [] }),
    ).toEqual({ action: "run", assignees: [] });
    expect(
      editorAssigneeStep({ requiresAssignees: false, assignees: [" ada "] }),
    ).toEqual({ action: "run", assignees: ["ada"] });
    expect(
      editorAssigneeStep({ requiresAssignees: true, assignees: ["ada"] }),
    ).toEqual({ action: "confirm" });
    expect(confirmEditorAssignees([])).toEqual({ ok: false, reason: "assignees" });
    expect(confirmEditorAssignees([" ada ", "ada"])).toEqual({
      ok: true,
      assignees: ["ada"],
    });
  });

  it("editorTransitionHttpFailure does not treat 400 403 or 409 as success (#5163)", () => {
    expect(editorTransitionHttpFailure(400)).toBe("badRequest");
    expect(editorTransitionHttpFailure(403)).toBe("forbidden");
    expect(editorTransitionHttpFailure(409)).toBe("conflict");
    expect(editorTransitionHttpFailure(500)).toBe("failed");
    expect(editorTransitionHttpFailure(undefined)).toBe("failed");
  });

  it("canChangeEditorWorkflow accepts only a different listed id", () => {
    expect(
      canChangeEditorWorkflow({
        selectedId: "7",
        currentId: "4",
        allowedIds: ["4", "7"],
      }),
    ).toEqual({ ok: true, workflowId: "7" });
    expect(
      canChangeEditorWorkflow({ selectedId: "", currentId: "4", allowedIds: ["7"] }),
    ).toEqual({ ok: false, reason: "blank" });
    expect(
      canChangeEditorWorkflow({ selectedId: "4", currentId: "4", allowedIds: ["4", "7"] }),
    ).toEqual({ ok: false, reason: "unchanged" });
    expect(
      canChangeEditorWorkflow({ selectedId: "9", currentId: "4", allowedIds: ["7"] }),
    ).toEqual({ ok: false, reason: "forbidden" });
  });
});
