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
import { validateContextVariableValue } from "@/publishing/contextVariableValue";
import {
  CONTEXT_VARIABLE_NOT_LISTED,
  buildContextVariableClearBody,
  contextVariablesAfterSuccessfulClear,
  mapContextVariableClearError,
  validateContextVariableClear,
} from "@/publishing/contextVariableClear";

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };
const target: SitePropertyDto = { name: "nightVar", contextId: "3", value: "before" };
const rows = [kept, target];

describe("validateContextVariableClear", () => {
  it("accepts a listed name and does not require a replacement value", () => {
    expect(validateContextVariableClear("  nightVar  ", rows)).toEqual({
      ok: true,
      name: "nightVar",
    });
  });

  it("rejects a blank name and a name that is not listed", () => {
    expect(validateContextVariableClear("   ", rows)).toEqual({
      ok: false,
      error: "Context variable name is required",
    });
    expect(validateContextVariableClear("missing", rows)).toEqual({
      ok: false,
      error: CONTEXT_VARIABLE_NOT_LISTED,
    });
  });

  it("rejects an overlong name", () => {
    expect(
      validateContextVariableClear("n".repeat(CONTEXT_VARIABLE_NAME_MAX_LENGTH + 1), rows),
    ).toEqual({
      ok: false,
      error: "Context variable name must be 50 characters or fewer",
    });
  });
});

describe("change-value form still refuses a blank value", () => {
  it("does not treat a blank change as a clear", () => {
    expect(validateContextVariableValue("nightVar", "   ", rows)).toEqual({
      ok: false,
      error: "Context variable value is required",
    });
  });
});

describe("buildContextVariableClearBody", () => {
  it("sends clearValue and not a value or the other variable", () => {
    expect(buildContextVariableClearBody("nightVar", "3")).toEqual({
      name: "nightVar",
      contextId: "3",
      clearValue: true,
    });
  });
});

describe("contextVariablesAfterSuccessfulClear", () => {
  it("keeps the name and the other variable when the refresh is empty", () => {
    const cleared = { name: "nightVar", contextId: "3", value: "" };
    const refreshed = [kept, cleared];
    expect(contextVariablesAfterSuccessfulClear(refreshed, cleared, rows)).toEqual(refreshed);
    expect(kept.value).toBe("old");
  });

  it("clears only that value when refresh fails", () => {
    const cleared = { name: "nightVar", contextId: "3", value: "" };
    expect(contextVariablesAfterSuccessfulClear(null, cleared, rows)).toEqual([
      kept,
      { name: "nightVar", contextId: "3", value: "" },
    ]);
  });

  it("replaces a stale refreshed value and leaves the other name", () => {
    const cleared = { name: "nightVar", contextId: "3", value: "" };
    expect(contextVariablesAfterSuccessfulClear(rows, cleared, rows)).toEqual([
      kept,
      { name: "nightVar", contextId: "3", value: "" },
    ]);
  });

  it("keeps the name when a refresh omits the cleared row", () => {
    const cleared = { name: "nightVar", contextId: "3", value: "" };
    expect(contextVariablesAfterSuccessfulClear([kept], cleared, rows)).toEqual([
      kept,
      { name: "nightVar", contextId: "3", value: "" },
    ]);
  });
});

describe("mapContextVariableClearError", () => {
  it("uses the body message for 400, 403, and 409", () => {
    expect(
      mapContextVariableClearError({
        status: 400,
        body: { message: "Context variable name is required" },
      }),
    ).toBe("Context variable name is required");
    expect(
      mapContextVariableClearError({
        status: 403,
        body: { message: "Admin or Designer role required" },
      }),
    ).toBe("Admin or Designer role required");
    expect(
      mapContextVariableClearError({
        status: 409,
        body: { message: CONTEXT_VARIABLE_NOT_LISTED },
      }),
    ).toBe(CONTEXT_VARIABLE_NOT_LISTED);
  });

  it("falls back when a 409 body has no message", () => {
    expect(mapContextVariableClearError({ status: 409, body: {} })).toContain("not listed");
  });
});
