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
  applyInboxAfterReload,
  inboxRoleRows,
  isInboxChangeReady,
  parseInboxChoice,
  storedInbox,
} from "../../../main/ts/developer/workflowStepRoleInbox";

describe("workflow step role inbox", () => {
  const previous = [
    { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE", notify: true, inbox: true },
    { stepName: "Draft", roleName: "Editor", assignmentType: "READER", notify: false, inbox: false },
    { stepName: "Draft", roleName: "Admin", assignmentType: "ADMIN", notify: true, inbox: true },
  ];

  it("treats a missing flag as off and enables confirm only when the choice differs", () => {
    expect(storedInbox(previous[0])).toBe(true);
    expect(storedInbox({})).toBe(false);
    expect(isInboxChangeReady(true, false)).toBe(true);
    expect(isInboxChangeReady(true, true)).toBe(false);
    expect(parseInboxChoice("off")).toBe(false);
    expect(parseInboxChoice("on")).toBe(true);
  });

  it("offers only Reader and Assignee rows", () => {
    expect(inboxRoleRows(previous).map((row) => row.roleName)).toEqual(["Author", "Editor"]);
  });

  it("keeps the previous rows until a reload shows the requested flag", () => {
    const stale = applyInboxAfterReload(previous, previous, "Draft", "Author", false);
    expect(stale.accepted).toBe(false);
    expect(stale.rows[0].inbox).toBe(true);

    const reloaded = [
      { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE", notify: true, inbox: false },
      { stepName: "Draft", roleName: "Editor", assignmentType: "READER", notify: false, inbox: false },
      { stepName: "Draft", roleName: "Admin", assignmentType: "ADMIN", notify: true, inbox: true },
    ];
    const applied = applyInboxAfterReload(previous, reloaded, "Draft", "Author", false);
    expect(applied.accepted).toBe(true);
    expect(applied.rows).toBe(reloaded);
    expect(applied.rows[0].notify).toBe(true);
    expect(applied.rows[1].assignmentType).toBe("READER");
  });

  it("does not accept an inbox change for an Admin role", () => {
    const reloaded = previous.map((row) =>
      row.roleName === "Admin" ? { ...row, inbox: false } : row,
    );
    const applied = applyInboxAfterReload(previous, reloaded, "Draft", "Admin", false);
    expect(applied.accepted).toBe(false);
    expect(applied.rows).toBe(previous);
  });
});
