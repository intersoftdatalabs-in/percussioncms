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
  CONTEXT_HAS_LOCATION_SCHEMES,
  contextsAfterSuccessfulDelete,
  mapContextDeleteError,
} from "@/publishing/contextDelete";

const publish: ContextSummary = { contextId: "3", name: "Publish" };
const keep: ContextSummary = { contextId: "4", name: "Keep" };

describe("mapContextDeleteError", () => {
  it("maps HTTP 400 body message", () => {
    expect(
      mapContextDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "contextId is required" },
      }),
    ).toBe("contextId is required");
  });

  it("maps HTTP 400 without a body to a visible status", () => {
    expect(
      mapContextDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/400|Bad Request/i);
  });

  it("maps HTTP 403 to forbidden chrome", () => {
    expect(
      mapContextDeleteError({
        status: 403,
        statusText: "Forbidden",
        body: { message: "Admin or Designer role required" },
      }),
    ).toMatch(/Admin or Designer|403|Forbidden/i);
  });

  it("maps HTTP 409 body message", () => {
    expect(
      mapContextDeleteError({
        status: 409,
        statusText: "Conflict",
        body: { message: CONTEXT_HAS_LOCATION_SCHEMES },
      }),
    ).toBe(CONTEXT_HAS_LOCATION_SCHEMES);
  });

  it("maps HTTP 409 without a body to schemes chrome", () => {
    expect(
      mapContextDeleteError({ status: 409, statusText: "Conflict", body: {} }),
    ).toMatch(/location schemes|409/i);
  });
});

describe("contextsAfterSuccessfulDelete", () => {
  it("drops the deleted id from a refreshed list", () => {
    expect(contextsAfterSuccessfulDelete([publish, keep], "3", [publish, keep])).toEqual([
      keep,
    ]);
  });

  it("drops the deleted id when refresh failed", () => {
    expect(contextsAfterSuccessfulDelete(null, "3", [publish, keep])).toEqual([keep]);
  });

  it("drops the deleted id when refresh still returns it", () => {
    expect(contextsAfterSuccessfulDelete([publish, keep], "3", [publish])).toEqual([keep]);
  });

  it("leaves the previous rows when the deleted id is blank", () => {
    expect(contextsAfterSuccessfulDelete(null, "  ", [publish])).toEqual([publish]);
  });
});
