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
import type { EditionSummary } from "@/api/publishing/designApi";
import {
  EDITION_PRIORITY_OUT_OF_RANGE,
  buildEditionPriorityBody,
  editionsAfterSuccessfulPriority,
  validateEditionPriority,
} from "@/publishing/editionPriority";

const source: EditionSummary = {
  editionId: "12",
  name: "NightEd",
  siteId: "1",
  comment: "keep-me",
  priority: 1,
};

const other: EditionSummary = {
  editionId: "13",
  name: "OtherEd",
  siteId: "1",
  comment: "other",
  priority: 2,
};

describe("validateEditionPriority", () => {
  it("accepts a trimmed whole number from 1 to 5", () => {
    expect(validateEditionPriority("  4 ")).toEqual({ ok: true, priority: 4 });
    expect(validateEditionPriority("1")).toEqual({ ok: true, priority: 1 });
    expect(validateEditionPriority("5")).toEqual({ ok: true, priority: 5 });
  });

  it.each(["", "   ", "0", "6", "9", "1.5", "abc", "05"])(
    "rejects %j before any save",
    (raw) => {
      expect(validateEditionPriority(raw)).toEqual({
        ok: false,
        error: EDITION_PRIORITY_OUT_OF_RANGE,
      });
    },
  );
});

describe("buildEditionPriorityBody", () => {
  it("sends the priority only", () => {
    expect(buildEditionPriorityBody(5)).toEqual({ priority: 5 });
    const body = buildEditionPriorityBody(2);
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("comment");
    expect(body).not.toHaveProperty("siteId");
    expect(body).not.toHaveProperty("editionId");
  });
});

describe("editionsAfterSuccessfulPriority", () => {
  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, priority: 5 }, other];
    expect(
      editionsAfterSuccessfulPriority(refreshed, "12", 5, [source, other]),
    ).toBe(refreshed);
  });

  it("patches only the priority when the reload failed", () => {
    expect(
      editionsAfterSuccessfulPriority(null, "12", 5, [source, other]),
    ).toEqual([{ ...source, priority: 5 }, other]);
  });
});
