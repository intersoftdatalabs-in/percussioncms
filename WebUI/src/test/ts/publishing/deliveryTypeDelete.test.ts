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
import type { DeliveryTypeSummary } from "@/api/publishing/designApi";
import {
  DELIVERY_TYPE_IN_USE,
  deliveryTypesAfterSuccessfulDelete,
  mapDeliveryTypeDeleteError,
} from "@/publishing/deliveryTypeDelete";

const unused: DeliveryTypeSummary = { deliveryTypeId: "8", name: "nightonly" };
const keep: DeliveryTypeSummary = { deliveryTypeId: "1", name: "filesystem" };

describe("mapDeliveryTypeDeleteError", () => {
  it("maps HTTP 400 body message", () => {
    expect(
      mapDeliveryTypeDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "deliveryTypeId is required" },
      }),
    ).toBe("deliveryTypeId is required");
  });

  it("maps HTTP 400 without a body to a visible status", () => {
    expect(
      mapDeliveryTypeDeleteError({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/400|Bad Request/i);
  });

  it("maps HTTP 403 to forbidden chrome", () => {
    expect(
      mapDeliveryTypeDeleteError({
        status: 403,
        statusText: "Forbidden",
        body: { message: "Admin or Designer role required" },
      }),
    ).toMatch(/Admin or Designer|403|Forbidden/i);
  });

  it("maps HTTP 409 body message", () => {
    expect(
      mapDeliveryTypeDeleteError({
        status: 409,
        statusText: "Conflict",
        body: { message: DELIVERY_TYPE_IN_USE },
      }),
    ).toBe(DELIVERY_TYPE_IN_USE);
  });

  it("maps HTTP 409 without a body to in-use chrome", () => {
    expect(
      mapDeliveryTypeDeleteError({ status: 409, statusText: "Conflict", body: {} }),
    ).toMatch(/in use|409/i);
  });

  it("does not read message off a plain ApiError object", () => {
    const err = {
      status: 409,
      statusText: "Conflict",
      body: { message: DELIVERY_TYPE_IN_USE },
    };
    expect(err instanceof Error).toBe(false);
    expect(mapDeliveryTypeDeleteError(err)).toBe(DELIVERY_TYPE_IN_USE);
  });
});

describe("deliveryTypesAfterSuccessfulDelete", () => {
  it("drops the deleted id from a refreshed list", () => {
    expect(deliveryTypesAfterSuccessfulDelete([unused, keep], "8", [unused, keep])).toEqual([
      keep,
    ]);
  });

  it("drops the deleted id when refresh failed", () => {
    expect(deliveryTypesAfterSuccessfulDelete(null, "8", [unused, keep])).toEqual([keep]);
  });

  it("drops the deleted id when refresh still returns it", () => {
    expect(deliveryTypesAfterSuccessfulDelete([unused, keep], "8", [keep])).toEqual([keep]);
  });

  it("leaves the previous rows when the deleted id is blank", () => {
    expect(deliveryTypesAfterSuccessfulDelete(null, "  ", [unused])).toEqual([unused]);
  });

  it("drops a numeric wire id without treating it as a string", () => {
    const numericUnused = {
      deliveryTypeId: 10001 as unknown as string,
      name: "DtDel",
    };
    const numericKeep = {
      deliveryTypeId: 1 as unknown as string,
      name: "filesystem",
    };
    expect(
      deliveryTypesAfterSuccessfulDelete(
        [numericUnused, numericKeep],
        10001,
        [numericUnused, numericKeep],
      ),
    ).toEqual([numericKeep]);
    expect(
      deliveryTypesAfterSuccessfulDelete(null, 10001, [numericUnused, numericKeep]),
    ).toEqual([numericKeep]);
  });
});
