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
  collectStepTransitionNames,
  formatStepTransitionNames,
} from "../../../main/ts/developer/workflowStepTransitions";

describe("collectStepTransitionNames", () => {
  it("returns unique transitionPermission values from roles", () => {
    expect(
      collectStepTransitionNames({
        stepName: "Draft",
        stepRoles: [
          {
            roleName: "Author",
            roleTransitions: [
              { transitionPermission: "Submit" },
              { transitionPermission: "Submit" },
            ],
          },
          {
            roleName: "Editor",
            roleTransitions: [{ transitionName: "Reject" }],
          },
        ],
      }),
    ).toEqual(["Submit", "Reject"]);
  });

  it("returns empty when roles or transitions are missing", () => {
    expect(collectStepTransitionNames({})).toEqual([]);
    expect(collectStepTransitionNames({ stepRoles: [] })).toEqual([]);
    expect(
      collectStepTransitionNames({
        stepRoles: [{ roleName: "Author" }],
      }),
    ).toEqual([]);
  });

  it("formats as a comma-separated list", () => {
    expect(
      formatStepTransitionNames({
        stepRoles: [
          {
            roleTransitions: [
              { transitionPermission: "Submit" },
              { transitionPermission: "Approve" },
            ],
          },
        ],
      }),
    ).toBe("Submit, Approve");
  });
});
