/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software,
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import {
  APPROVE_TRIGGER_NAMES,
  formatApproveRejectBody,
  resolveApproveRejectTriggers,
} from "../../../main/ts/editor/editorApproveReject";

describe("editorApproveReject quick-bar trigger resolution (#6483)", () => {
  it("resolves an allowlisted Approve and Reject by exact name", () => {
    expect(
      resolveApproveRejectTriggers(["Submit", "Approve", "Reject"]),
    ).toEqual({ approve: "Approve", reject: "Reject" });
  });

  it("hides the bar when neither style is allowlisted", () => {
    expect(resolveApproveRejectTriggers(["Submit"])).toEqual({
      approve: null,
      reject: null,
    });
  });

  it("treats a missing or empty allowlist as no bar", () => {
    expect(resolveApproveRejectTriggers(null)).toEqual({
      approve: null,
      reject: null,
    });
    expect(resolveApproveRejectTriggers([])).toEqual({
      approve: null,
      reject: null,
    });
  });

  it("matches case- and whitespace-insensitively and returns the listed name", () => {
    expect(resolveApproveRejectTriggers(["  approve  "])).toEqual({
      approve: "approve",
      reject: null,
    });
    expect(resolveApproveRejectTriggers(["SendBack"])).toEqual({
      approve: null,
      reject: "SendBack",
    });
    expect(resolveApproveRejectTriggers(["send back"])).toEqual({
      approve: null,
      reject: "send back",
    });
  });

  it("covers the built-in reject-style names", () => {
    for (const name of [
      "Reject",
      "Return",
      "Disapprove",
      "Decline",
      "SendBack",
      "Send Back",
    ]) {
      expect(resolveApproveRejectTriggers([name]).reject).toBe(name);
    }
  });

  it("ignores blank entries and picks the first match", () => {
    expect(
      resolveApproveRejectTriggers(["", "  ", "Reject", "Return"]),
    ).toEqual({ approve: null, reject: "Reject" });
  });

  it("lets overrides replace the built-in bases", () => {
    expect(
      resolveApproveRejectTriggers(["Promote"], {
        approveNames: ["Promote"],
        rejectNames: ["Demote"],
      }),
    ).toEqual({ approve: "Promote", reject: null });
    expect(
      resolveApproveRejectTriggers(["Approve"], {
        approveNames: ["Promote"],
      }).approve,
    ).toBeNull();
  });

  it("exposes the Approve base name", () => {
    expect(APPROVE_TRIGGER_NAMES).toEqual(["Approve"]);
  });

  it("formats the dialog body with the resolved trigger", () => {
    expect(formatApproveRejectBody("Runs {0} now.", "Reject")).toBe(
      "Runs Reject now.",
    );
  });
});
