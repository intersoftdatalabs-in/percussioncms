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
  applyNotifyAfterReload,
  isNotifyChangeReady,
  parseNotifyChoice,
  storedNotify,
} from "../../../main/ts/developer/workflowStepRoleNotify";

describe("workflow step role notify", () => {
  const previous = [
    { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE", notify: true },
    { stepName: "Draft", roleName: "Editor", assignmentType: "READER", notify: false },
  ];

  it("treats a missing flag as off and enables confirm only when the choice differs", () => {
    expect(storedNotify(previous[0])).toBe(true);
    expect(storedNotify({})).toBe(false);
    expect(isNotifyChangeReady(true, false)).toBe(true);
    expect(isNotifyChangeReady(true, true)).toBe(false);
    expect(parseNotifyChoice("off")).toBe(false);
    expect(parseNotifyChoice("on")).toBe(true);
  });

  it("keeps the previous rows until a reload shows the requested flag", () => {
    const stale = applyNotifyAfterReload(previous, previous, "Draft", "Author", false);
    expect(stale.accepted).toBe(false);
    expect(stale.rows[0].notify).toBe(true);

    const reloaded = [
      { stepName: "Draft", roleName: "Author", assignmentType: "ASSIGNEE", notify: false },
      { stepName: "Draft", roleName: "Editor", assignmentType: "READER", notify: false },
    ];
    const applied = applyNotifyAfterReload(previous, reloaded, "Draft", "Author", false);
    expect(applied.accepted).toBe(true);
    expect(applied.rows).toBe(reloaded);
    expect(applied.rows[1].assignmentType).toBe("READER");
  });
});
