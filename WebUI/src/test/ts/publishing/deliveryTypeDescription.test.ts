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
  DELIVERY_TYPE_DESCRIPTION_MAX_LENGTH,
  buildDeliveryTypeDescriptionBody,
  deliveryTypesAfterSuccessfulDescription,
  validateDeliveryTypeDescription,
} from "@/publishing/deliveryTypeDescription";

const source: DeliveryTypeSummary = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "Publish content to the filesystem",
  unpublishingRequiresAssembly: true,
};

describe("validateDeliveryTypeDescription", () => {
  it("treats blank text as a clear", () => {
    expect(validateDeliveryTypeDescription("   ")).toEqual({
      ok: true,
      description: "",
    });
  });

  it("rejects a description longer than the column", () => {
    expect(
      validateDeliveryTypeDescription(
        "d".repeat(DELIVERY_TYPE_DESCRIPTION_MAX_LENGTH + 1),
      ),
    ).toEqual({
      ok: false,
      error: "Delivery type description must be 255 characters or fewer",
    });
  });

  it("trims a usable description", () => {
    expect(validateDeliveryTypeDescription("  Night notes  ")).toEqual({
      ok: true,
      description: "Night notes",
    });
  });
});

describe("buildDeliveryTypeDescriptionBody", () => {
  it("sends the description only, including an empty clear", () => {
    expect(buildDeliveryTypeDescriptionBody("Night notes")).toEqual({
      description: "Night notes",
    });
    expect(buildDeliveryTypeDescriptionBody("")).toEqual({ description: "" });
    expect(buildDeliveryTypeDescriptionBody("Night notes")).not.toHaveProperty("name");
    expect(buildDeliveryTypeDescriptionBody("Night notes")).not.toHaveProperty("beanName");
    expect(buildDeliveryTypeDescriptionBody("Night notes")).not.toHaveProperty(
      "unpublishingRequiresAssembly",
    );
    expect(buildDeliveryTypeDescriptionBody("Night notes")).not.toHaveProperty(
      "deliveryTypeId",
    );
  });
});

describe("deliveryTypesAfterSuccessfulDescription", () => {
  const other: DeliveryTypeSummary = {
    deliveryTypeId: "2",
    name: "ftp",
    beanName: "sys_ftpDeliveryHandler",
    description: "ftp notes",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, description: "Night notes" }, other];
    expect(
      deliveryTypesAfterSuccessfulDescription(refreshed, "1", "Night notes", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("patches only the description when the reload failed", () => {
    expect(
      deliveryTypesAfterSuccessfulDescription(null, "1", "Night notes", [source, other]),
    ).toEqual([{ ...source, description: "Night notes" }, other]);
  });

  it("clears only the description when the reload failed", () => {
    expect(
      deliveryTypesAfterSuccessfulDescription(null, "1", "", [source, other]),
    ).toEqual([{ ...source, description: "" }, other]);
  });
});
