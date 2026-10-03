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
import {
  mapEditionContentListDisassociateError,
  mapEditionDeleteError,
  mapEditionSaveError,
} from "@/publishing/editionSaveErrors";

describe("mapEditionSaveError", () => {
  it("maps HTTP 403 to forbidden chrome", () => {
    expect(mapEditionSaveError({ status: 403, statusText: "Forbidden", body: {} })).toMatch(
      /Forbidden|403/i,
    );
  });

  it("maps HTTP 409 body message", () => {
    expect(
      mapEditionSaveError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Edition name already exists" },
      }),
    ).toBe("Edition name already exists");
  });

  it("maps HTTP 400 body message for an invalid name", () => {
    expect(
      mapEditionSaveError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "Edition name must be 100 characters or fewer" },
      }),
    ).toBe("Edition name must be 100 characters or fewer");
  });

  it("maps HTTP 400 without a body to a visible status", () => {
    expect(
      mapEditionSaveError({ status: 400, statusText: "Bad Request", body: {} }),
    ).toMatch(/400|Bad Request/i);
  });
});

describe("mapEditionDeleteError", () => {
  it("maps HTTP 409 to the edition-in-use message", () => {
    expect(
      mapEditionDeleteError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Edition is in use" },
      }),
    ).toBe("Edition is in use");
  });

  it("maps HTTP 409 without a body to edition in use", () => {
    expect(
      mapEditionDeleteError({ status: 409, statusText: "Conflict", body: {} }),
    ).toMatch(/Edition is in use|409/i);
  });

  it("maps HTTP 403 to forbidden chrome", () => {
    expect(
      mapEditionDeleteError({
        status: 403,
        statusText: "Forbidden",
        body: { message: "Admin or Designer role required to save a publish edition" },
      }),
    ).toMatch(/Admin or Designer|403|Forbidden/i);
  });

  it("maps HTTP 400 body message", () => {
    expect(
      mapEditionDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "editionId is required" },
      }),
    ).toBe("editionId is required");
  });
});

describe("mapEditionContentListDisassociateError", () => {
  it("maps HTTP 400 body message", () => {
    expect(
      mapEditionContentListDisassociateError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "contentListId is required" },
      }),
    ).toBe("contentListId is required");
  });

  it("maps HTTP 403 to forbidden chrome", () => {
    expect(
      mapEditionContentListDisassociateError({
        status: 403,
        statusText: "Forbidden",
        body: {
          message: "Admin or Designer role required to save a publish edition",
        },
      }),
    ).toMatch(/Admin or Designer|403|Forbidden/i);
  });

  it("maps HTTP 409 to edition in use", () => {
    expect(
      mapEditionContentListDisassociateError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Edition is in use" },
      }),
    ).toBe("Edition is in use");
  });

  it("maps HTTP 409 without a body to edition in use", () => {
    expect(
      mapEditionContentListDisassociateError({
        status: 409,
        statusText: "Conflict",
        body: {},
      }),
    ).toMatch(/Edition is in use|409/i);
  });

  it("does not treat a plain API error as an Error message", () => {
    expect(
      mapEditionContentListDisassociateError({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).not.toBe("[object Object]");
  });
});
