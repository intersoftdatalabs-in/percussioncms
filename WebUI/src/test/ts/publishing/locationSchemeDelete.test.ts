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
import type { LocationSchemeSummary } from "@/api/publishing/designApi";
import {
  mapLocationSchemeDeleteError,
  schemesAfterSuccessfulDelete,
} from "@/publishing/locationSchemeDelete";

const article: LocationSchemeSummary = {
  schemeId: "11",
  name: "Article",
};
const keep: LocationSchemeSummary = {
  schemeId: "12",
  name: "Keep",
};

describe("mapLocationSchemeDeleteError", () => {
  it("maps HTTP 400 body message", () => {
    expect(
      mapLocationSchemeDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "schemeId is required" },
      }),
    ).toBe("schemeId is required");
  });

  it("maps HTTP 400 without a body to a visible status", () => {
    expect(
      mapLocationSchemeDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/400|Bad Request/i);
  });

  it("maps HTTP 403 to forbidden chrome", () => {
    expect(
      mapLocationSchemeDeleteError({
        status: 403,
        statusText: "Forbidden",
        body: { message: "Admin or Designer role required" },
      }),
    ).toMatch(/Admin or Designer|403|Forbidden/i);
  });

  it("maps HTTP 409 body message", () => {
    expect(
      mapLocationSchemeDeleteError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Location scheme is in use" },
      }),
    ).toBe("Location scheme is in use");
  });

  it("maps HTTP 409 without a body to in-use chrome", () => {
    expect(
      mapLocationSchemeDeleteError({
        status: 409,
        statusText: "Conflict",
        body: {},
      }),
    ).toMatch(/Location scheme is in use|409/i);
  });
});

describe("schemesAfterSuccessfulDelete", () => {
  it("drops the deleted id from a refreshed list", () => {
    expect(schemesAfterSuccessfulDelete([article, keep], "11", [article, keep])).toEqual([
      keep,
    ]);
  });

  it("drops the deleted id when refresh failed", () => {
    expect(schemesAfterSuccessfulDelete(null, "11", [article, keep])).toEqual([keep]);
  });

  it("drops the deleted id when refresh still returns it", () => {
    expect(schemesAfterSuccessfulDelete([article, keep], "11", [article])).toEqual([keep]);
  });

  it("leaves the previous rows when the deleted id is blank", () => {
    expect(schemesAfterSuccessfulDelete(null, "  ", [article])).toEqual([article]);
  });
});
