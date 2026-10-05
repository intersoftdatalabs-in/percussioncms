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
  buildEditionCommentBody,
  editionsAfterSuccessfulComment,
  normalizeEditionComment,
} from "@/publishing/editionComment";

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

describe("normalizeEditionComment", () => {
  it("trims text and treats blank as clear", () => {
    expect(normalizeEditionComment("  night note  ")).toBe("night note");
    expect(normalizeEditionComment("")).toBe("");
    expect(normalizeEditionComment("   ")).toBe("");
    expect(normalizeEditionComment("\n\t")).toBe("");
  });
});

describe("buildEditionCommentBody", () => {
  it("sends the comment only, including an empty clear", () => {
    expect(buildEditionCommentBody("night note")).toEqual({
      comment: "night note",
    });
    const cleared = buildEditionCommentBody("");
    expect(cleared).toEqual({ comment: "" });
    expect(cleared).not.toHaveProperty("name");
    expect(cleared).not.toHaveProperty("priority");
    expect(cleared).not.toHaveProperty("siteId");
    expect(cleared).not.toHaveProperty("editionId");
  });
});

describe("editionsAfterSuccessfulComment", () => {
  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, comment: "night note" }, other];
    expect(
      editionsAfterSuccessfulComment(refreshed, "12", "night note", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the comment when the reload failed", () => {
    expect(
      editionsAfterSuccessfulComment(null, "12", "", [source, other]),
    ).toEqual([{ ...source, comment: "" }, other]);
  });
});
