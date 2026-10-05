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
  applyAdhocAfterReload,
  adhocRoleRows,
  isAdhocChangeReady,
  normalizeAdhocType,
  storedAdhoc,
} from "../../../main/ts/developer/workflowStepRoleAdhoc";

describe("workflow step role adhoc", () => {
  const previous = [
    {
      stepName: "Draft",
      roleName: "Author",
      assignmentType: "ASSIGNEE",
      notify: true,
      inbox: true,
      adhocType: "disabled",
    },
    {
      stepName: "Draft",
      roleName: "Editor",
      assignmentType: "READER",
      notify: false,
      inbox: false,
      adhocType: "enabled",
    },
    {
      stepName: "Draft",
      roleName: "Admin",
      assignmentType: "ADMIN",
      notify: true,
      inbox: true,
      adhocType: "disabled",
    },
  ];

  it("treats a missing type as disabled and enables confirm only when the choice differs", () => {
    expect(storedAdhoc(previous[0])).toBe("disabled");
    expect(storedAdhoc({})).toBe("disabled");
    expect(normalizeAdhocType("ENABLED")).toBe("enabled");
    expect(normalizeAdhocType("nope")).toBe("disabled");
    expect(isAdhocChangeReady("disabled", "enabled")).toBe(true);
    expect(isAdhocChangeReady("disabled", "DISABLED")).toBe(false);
  });

  it("offers only Reader and Assignee rows", () => {
    expect(adhocRoleRows(previous).map((row) => row.roleName)).toEqual(["Author", "Editor"]);
  });

  it("keeps the previous rows until a reload shows the requested type", () => {
    const stale = applyAdhocAfterReload(previous, previous, "Draft", "Author", "enabled");
    expect(stale.accepted).toBe(false);
    expect(stale.rows[0].adhocType).toBe("disabled");

    const reloaded = [
      {
        stepName: "Draft",
        roleName: "Author",
        assignmentType: "ASSIGNEE",
        notify: true,
        inbox: true,
        adhocType: "anonymous",
      },
      {
        stepName: "Draft",
        roleName: "Editor",
        assignmentType: "READER",
        notify: false,
        inbox: false,
        adhocType: "enabled",
      },
      {
        stepName: "Draft",
        roleName: "Admin",
        assignmentType: "ADMIN",
        notify: true,
        inbox: true,
        adhocType: "disabled",
      },
    ];
    const applied = applyAdhocAfterReload(previous, reloaded, "Draft", "Author", "anonymous");
    expect(applied.accepted).toBe(true);
    expect(applied.rows).toBe(reloaded);
    expect(applied.rows[0].notify).toBe(true);
    expect(applied.rows[0].inbox).toBe(true);
    expect(applied.rows[1].assignmentType).toBe("READER");
  });

  it("does not accept an adhoc change for an Admin role", () => {
    const reloaded = previous.map((row) =>
      row.roleName === "Admin" ? { ...row, adhocType: "enabled" } : row,
    );
    const applied = applyAdhocAfterReload(previous, reloaded, "Draft", "Admin", "enabled");
    expect(applied.accepted).toBe(false);
    expect(applied.rows).toBe(previous);
  });
});
