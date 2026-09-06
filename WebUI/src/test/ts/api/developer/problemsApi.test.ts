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

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import {
  INVALID_SESSION_FIXTURE,
  isDeveloperNavigateSection,
  isSafeProblemsFixture,
  listDesignProblems,
  problemsListUrl,
  unwrapDesignProblems,
} from "../../../../main/ts/api/developer/problemsApi";
import { PATHS } from "../../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("Problems path-safety helpers", () => {
  it("accepts REST fixture tokens", () => {
    expect(isSafeProblemsFixture("invalid-session")).toBe(true);
    expect(isSafeProblemsFixture("open_editor")).toBe(true);
  });

  it("rejects unsafe fixture tokens", () => {
    expect(isSafeProblemsFixture("")).toBe(false);
    expect(isSafeProblemsFixture("1bad")).toBe(false);
    expect(isSafeProblemsFixture("../etc")).toBe(false);
    expect(isSafeProblemsFixture("C:Windows")).toBe(false);
    expect(isSafeProblemsFixture("jdbc/RhythmyxData")).toBe(false);
  });

  it("accepts Developer navigate sections only", () => {
    expect(isDeveloperNavigateSection("content-types")).toBe(true);
    expect(isDeveloperNavigateSection("problems")).toBe(true);
    expect(isDeveloperNavigateSection("../admin")).toBe(false);
    expect(isDeveloperNavigateSection("home")).toBe(false);
  });
});

describe("unwrap DesignProblem payloads", () => {
  it("unwraps a bare array and Jackson wrap", () => {
    expect(
      unwrapDesignProblems([
        {
          id: "invalid-session",
          severity: "ERROR",
          message: "Open editor is missing a required name.",
          navigateSection: "content-types",
        },
      ]),
    ).toEqual([
      {
        id: "invalid-session",
        severity: "ERROR",
        code: undefined,
        message: "Open editor is missing a required name.",
        objectType: undefined,
        objectId: undefined,
        objectName: undefined,
        location: undefined,
        navigateSection: "content-types",
      },
    ]);
    expect(
      unwrapDesignProblems({
        DesignProblem: {
          id: "invalid-session",
          severity: "warning",
          message: "warn",
        },
      }),
    ).toEqual([
      {
        id: "invalid-session",
        severity: "WARNING",
        code: undefined,
        message: "warn",
        objectType: undefined,
        objectId: undefined,
        objectName: undefined,
        location: undefined,
        navigateSection: undefined,
      },
    ]);
    expect(unwrapDesignProblems("not-json")).toEqual([]);
    expect(unwrapDesignProblems(42)).toEqual([]);
  });

  it("skips unsafe ids and unknown navigate sections", () => {
    expect(
      unwrapDesignProblems([
        { id: "../etc", severity: "ERROR", message: "nope" },
        {
          id: "ok",
          severity: "ERROR",
          message: "bad nav",
          navigateSection: "../admin",
        },
      ]),
    ).toEqual([
      {
        id: "ok",
        severity: "ERROR",
        code: undefined,
        message: "bad nav",
        objectType: undefined,
        objectId: undefined,
        objectName: undefined,
        location: undefined,
        navigateSection: undefined,
      },
    ]);
  });
});

describe("problemsListUrl", () => {
  it("omits fixture when blank", () => {
    expect(problemsListUrl()).toBe(PATHS.PROBLEMS);
    expect(problemsListUrl("  ")).toBe(PATHS.PROBLEMS);
  });

  it("encodes a safe fixture", () => {
    expect(problemsListUrl(INVALID_SESSION_FIXTURE)).toBe(
      `${PATHS.PROBLEMS}?fixture=invalid-session`,
    );
  });

  it("rejects unsafe fixtures before GET", () => {
    expect(() => problemsListUrl("../etc")).toThrow(/Invalid Problems fixture/);
  });
});

describe("listDesignProblems REST calls", () => {
  it("listDesignProblems GETs PATHS.PROBLEMS", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue([
      { id: "invalid-session", severity: "ERROR", message: "missing name" },
    ]);
    const list = await listDesignProblems();
    expect(spy).toHaveBeenCalledWith(PATHS.PROBLEMS);
    expect(list[0].id).toBe("invalid-session");
  });

  it("listDesignProblems GETs encoded fixture URL", async () => {
    const spy = vi.spyOn(client, "get").mockResolvedValue([]);
    await listDesignProblems("invalid-session");
    expect(spy).toHaveBeenCalledWith(`${PATHS.PROBLEMS}?fixture=invalid-session`);
  });
});
