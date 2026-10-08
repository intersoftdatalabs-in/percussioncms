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
import type { SitePropertyDto } from "@/api/publishing/designApi";
import { CONTEXT_VARIABLE_NAME_MAX_LENGTH } from "@/publishing/contextVariable";
import {
  CONTEXT_VARIABLE_NOT_LISTED,
  contextVariablesAfterSuccessfulDelete,
  mapContextVariableDeleteError,
  validateContextVariableDelete,
} from "@/publishing/contextVariableDelete";

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };
const target: SitePropertyDto = { name: "nightVar", contextId: "3", value: "before" };
const rows = [kept, target];

describe("validateContextVariableDelete", () => {
  it("rejects a blank name", () => {
    expect(validateContextVariableDelete("   ", rows)).toEqual({
      ok: false,
      error: "Context variable name is required",
    });
  });

  it("rejects an overlong name", () => {
    expect(
      validateContextVariableDelete("n".repeat(CONTEXT_VARIABLE_NAME_MAX_LENGTH + 1), rows),
    ).toEqual({
      ok: false,
      error: "Context variable name must be 50 characters or fewer",
    });
  });

  it("rejects a name that is not listed", () => {
    expect(validateContextVariableDelete("missing", rows)).toEqual({
      ok: false,
      error: CONTEXT_VARIABLE_NOT_LISTED,
    });
  });

  it("trims a listed name", () => {
    expect(validateContextVariableDelete("  nightVar  ", rows)).toEqual({
      ok: true,
      name: "nightVar",
    });
  });
});

describe("contextVariablesAfterSuccessfulDelete", () => {
  it("keeps the other variable when the refresh omits the deleted name", () => {
    expect(contextVariablesAfterSuccessfulDelete([kept], "nightVar", rows)).toEqual([kept]);
    expect(kept.value).toBe("old");
  });

  it("drops only the deleted name when refresh fails", () => {
    expect(contextVariablesAfterSuccessfulDelete(null, "nightVar", rows)).toEqual([kept]);
  });

  it("drops a stale refreshed row and leaves the other name", () => {
    expect(contextVariablesAfterSuccessfulDelete(rows, "  nightVar  ", rows)).toEqual([kept]);
  });
});

describe("mapContextVariableDeleteError", () => {
  it("uses the body message for 400, 403, and 409", () => {
    expect(
      mapContextVariableDeleteError({
        status: 400,
        body: { message: "Context variable name is required" },
      }),
    ).toBe("Context variable name is required");
    expect(
      mapContextVariableDeleteError({
        status: 403,
        body: { message: "Admin or Designer role required" },
      }),
    ).toBe("Admin or Designer role required");
    expect(
      mapContextVariableDeleteError({
        status: 409,
        body: { message: CONTEXT_VARIABLE_NOT_LISTED },
      }),
    ).toBe(CONTEXT_VARIABLE_NOT_LISTED);
  });

  it("falls back when a 409 body has no message", () => {
    expect(mapContextVariableDeleteError({ status: 409, body: {} })).toContain("not listed");
  });
});
