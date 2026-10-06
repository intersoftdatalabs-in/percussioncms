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
import type { ContextSummary } from "@/api/publishing/designApi";
import {
  CONTEXT_DESCRIPTION_MAX_LENGTH,
  buildContextDescriptionBody,
  contextsAfterSuccessfulDescription,
  validateContextDescription,
} from "@/publishing/contextDescription";

const source: ContextSummary = {
  contextId: "3",
  name: "Publish",
  description: "Public site",
  defaultSchemeId: "11",
};

describe("validateContextDescription", () => {
  it("treats blank text as a clear", () => {
    expect(validateContextDescription("   ")).toEqual({
      ok: true,
      description: "",
    });
  });

  it("rejects a description longer than the column", () => {
    expect(
      validateContextDescription("d".repeat(CONTEXT_DESCRIPTION_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: "Publishing context description must be 255 characters or fewer",
    });
  });

  it("trims a usable description", () => {
    expect(validateContextDescription("  Night notes  ")).toEqual({
      ok: true,
      description: "Night notes",
    });
  });
});

describe("buildContextDescriptionBody", () => {
  it("sends the description only, including an empty clear", () => {
    expect(buildContextDescriptionBody("Night notes")).toEqual({
      description: "Night notes",
    });
    expect(buildContextDescriptionBody("")).toEqual({ description: "" });
    expect(buildContextDescriptionBody("Night notes")).not.toHaveProperty("name");
    expect(buildContextDescriptionBody("Night notes")).not.toHaveProperty(
      "defaultSchemeId",
    );
    expect(buildContextDescriptionBody("Night notes")).not.toHaveProperty("contextId");
  });
});

describe("contextsAfterSuccessfulDescription", () => {
  const other: ContextSummary = {
    contextId: "4",
    name: "Preview",
    description: "preview",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, description: "Night notes" }, other];
    expect(
      contextsAfterSuccessfulDescription(refreshed, "3", "Night notes", [source, other]),
    ).toBe(refreshed);
  });

  it("patches only the description when the reload failed", () => {
    expect(
      contextsAfterSuccessfulDescription(null, "3", "Night notes", [source, other]),
    ).toEqual([{ ...source, description: "Night notes" }, other]);
  });

  it("clears only the description when the reload failed", () => {
    expect(contextsAfterSuccessfulDescription(null, "3", "", [source, other])).toEqual([
      { ...source, description: "" },
      other,
    ]);
  });
});
