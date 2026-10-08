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
import { CONTEXT_VARIABLE_VALUE_MAX_LENGTH } from "@/publishing/contextVariable";
import {
  CONTEXT_VARIABLE_NOT_LISTED,
  buildContextVariableValueBody,
  contextVariablesAfterSuccessfulValueChange,
  mapContextVariableValueSaveError,
  validateContextVariableValue,
} from "@/publishing/contextVariableValue";

const kept: SitePropertyDto = { name: "kept", contextId: "3", value: "old" };
const target: SitePropertyDto = { name: "nightVar", contextId: "3", value: "before" };
const rows = [kept, target];

describe("validateContextVariableValue", () => {
  it("rejects a blank value and does not clear the stored one", () => {
    expect(validateContextVariableValue("nightVar", "   ", rows)).toEqual({
      ok: false,
      error: "Context variable value is required",
    });
  });

  it("rejects an overlong value", () => {
    expect(
      validateContextVariableValue(
        "nightVar",
        "v".repeat(CONTEXT_VARIABLE_VALUE_MAX_LENGTH + 1),
        rows,
      ),
    ).toEqual({
      ok: false,
      error: "Context variable value must be 255 characters or fewer",
    });
  });

  it("rejects a name that is not listed", () => {
    expect(validateContextVariableValue("missing", "next", rows)).toEqual({
      ok: false,
      error: CONTEXT_VARIABLE_NOT_LISTED,
    });
  });

  it("trims a listed name and a new value", () => {
    expect(validateContextVariableValue("  nightVar  ", "  next-value  ", rows)).toEqual({
      ok: true,
      name: "nightVar",
      value: "next-value",
    });
  });
});

describe("buildContextVariableValueBody", () => {
  it("sends the same name with updateValue and not the other variable", () => {
    expect(buildContextVariableValueBody("nightVar", "3", "next-value")).toEqual({
      name: "nightVar",
      contextId: "3",
      value: "next-value",
      updateValue: true,
    });
  });
});

describe("contextVariablesAfterSuccessfulValueChange", () => {
  it("keeps the other variable when the refresh is the server list", () => {
    const updated = { name: "nightVar", contextId: "3", value: "next-value" };
    const refreshed = [kept, updated];
    expect(contextVariablesAfterSuccessfulValueChange(refreshed, updated, rows)).toEqual(
      refreshed,
    );
    expect(kept.value).toBe("old");
  });

  it("replaces only that value when refresh fails", () => {
    const updated = { name: "nightVar", contextId: "3", value: "next-value" };
    expect(contextVariablesAfterSuccessfulValueChange(null, updated, rows)).toEqual([
      kept,
      { name: "nightVar", contextId: "3", value: "next-value" },
    ]);
  });

  it("replaces a stale refreshed value and leaves the other name", () => {
    const updated = { name: "nightVar", contextId: "3", value: "next-value" };
    expect(contextVariablesAfterSuccessfulValueChange(rows, updated, rows)).toEqual([
      kept,
      { name: "nightVar", contextId: "3", value: "next-value" },
    ]);
  });
});

describe("mapContextVariableValueSaveError", () => {
  it("uses the body message for 400, 403, and 409", () => {
    expect(
      mapContextVariableValueSaveError({
        status: 400,
        body: { message: "Context variable value is required" },
      }),
    ).toBe("Context variable value is required");
    expect(
      mapContextVariableValueSaveError({
        status: 403,
        body: { message: "Admin or Designer role required" },
      }),
    ).toBe("Admin or Designer role required");
    expect(
      mapContextVariableValueSaveError({
        status: 409,
        body: { message: CONTEXT_VARIABLE_NOT_LISTED },
      }),
    ).toBe(CONTEXT_VARIABLE_NOT_LISTED);
  });

  it("falls back when a 409 body has no message", () => {
    expect(mapContextVariableValueSaveError({ status: 409, body: {} })).toContain(
      "not listed",
    );
  });
});
