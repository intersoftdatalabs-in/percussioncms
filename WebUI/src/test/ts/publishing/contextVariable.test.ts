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
import {
  CONTEXT_VARIABLE_NAME_MAX_LENGTH,
  CONTEXT_VARIABLE_VALUE_MAX_LENGTH,
  contextVariableNameListed,
  contextVariablesAfterSuccessfulCreate,
  validateContextVariable,
} from "@/publishing/contextVariable";
import { mapContextVariableSaveError } from "@/publishing/contextVariableSaveErrors";

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };

describe("validateContextVariable", () => {
  it("rejects a blank name and does not treat it as a value", () => {
    expect(validateContextVariable("   ", "night")).toEqual({
      ok: false,
      error: "Context variable name is required",
    });
  });

  it("rejects a blank value", () => {
    expect(validateContextVariable("night", "  ")).toEqual({
      ok: false,
      error: "Context variable value is required",
    });
  });

  it("rejects an overlong name or value", () => {
    expect(
      validateContextVariable("n".repeat(CONTEXT_VARIABLE_NAME_MAX_LENGTH + 1), "v"),
    ).toEqual({
      ok: false,
      error: "Context variable name must be 50 characters or fewer",
    });
    expect(
      validateContextVariable("n", "v".repeat(CONTEXT_VARIABLE_VALUE_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: "Context variable value must be 255 characters or fewer",
    });
  });

  it("trims a name and value that fit", () => {
    expect(validateContextVariable("  nightVar  ", "  night-value  ")).toEqual({
      ok: true,
      name: "nightVar",
      value: "night-value",
    });
  });
});

describe("contextVariablesAfterSuccessfulCreate", () => {
  it("keeps the other variable when the refresh is the server list", () => {
    const created = { name: "nightVar", contextId: "3", value: "night-value" };
    const refreshed = [kept, created];
    expect(contextVariablesAfterSuccessfulCreate(refreshed, created, [kept])).toEqual(
      refreshed,
    );
    expect(kept.value).toBe("old");
  });

  it("appends the new variable and leaves the other unchanged when refresh fails", () => {
    const created = { name: "nightVar", contextId: "3", value: "night-value" };
    const next = contextVariablesAfterSuccessfulCreate(null, created, [kept]);
    expect(next).toEqual([kept, created]);
    expect(next[0]).toBe(kept);
    expect(contextVariableNameListed([kept], "kept")).toBe(true);
    expect(contextVariableNameListed([kept], "nightVar")).toBe(false);
  });
});

describe("mapContextVariableSaveError", () => {
  it("uses the server text for 400, 403, and 409", () => {
    expect(
      mapContextVariableSaveError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "Context variable value is required" },
      }),
    ).toBe("Context variable value is required");
    expect(
      mapContextVariableSaveError({
        status: 403,
        statusText: "Forbidden",
        body: { message: "Admin or Designer role required" },
      }),
    ).toBe("Admin or Designer role required");
    expect(
      mapContextVariableSaveError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Context variable already exists" },
      }),
    ).toBe("Context variable already exists");
  });
});
