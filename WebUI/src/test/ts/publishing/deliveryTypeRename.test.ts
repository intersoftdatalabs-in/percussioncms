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
  DELIVERY_TYPE_NAME_MAX_LENGTH,
  buildDeliveryTypeRenameBody,
  deliveryTypesAfterSuccessfulRename,
  validateDeliveryTypeRenameName,
} from "@/publishing/deliveryTypeRename";

const source: DeliveryTypeSummary = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "Publish content to the filesystem",
  unpublishingRequiresAssembly: true,
};

describe("validateDeliveryTypeRenameName", () => {
  it("rejects a blank name", () => {
    expect(validateDeliveryTypeRenameName("   ")).toEqual({
      ok: false,
      error: "Name is required",
    });
  });

  it("rejects a name longer than the column", () => {
    expect(
      validateDeliveryTypeRenameName("n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: "Delivery type name must be 50 characters or fewer",
    });
  });

  it("trims a usable name", () => {
    expect(validateDeliveryTypeRenameName("  Night  ")).toEqual({
      ok: true,
      name: "Night",
    });
  });
});

describe("buildDeliveryTypeRenameBody", () => {
  it("sends the name only", () => {
    expect(buildDeliveryTypeRenameBody("Renamed")).toEqual({ name: "Renamed" });
    expect(buildDeliveryTypeRenameBody("Renamed")).not.toHaveProperty("beanName");
    expect(buildDeliveryTypeRenameBody("Renamed")).not.toHaveProperty("description");
    expect(buildDeliveryTypeRenameBody("Renamed")).not.toHaveProperty(
      "unpublishingRequiresAssembly",
    );
    expect(buildDeliveryTypeRenameBody("Renamed")).not.toHaveProperty("deliveryTypeId");
  });
});

describe("deliveryTypesAfterSuccessfulRename", () => {
  const other: DeliveryTypeSummary = {
    deliveryTypeId: "2",
    name: "ftp",
    beanName: "sys_ftpDeliveryHandler",
    description: "ftp",
  };

  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [{ ...source, name: "Renamed" }, other];
    expect(
      deliveryTypesAfterSuccessfulRename(refreshed, "1", "Renamed", [source, other]),
    ).toBe(refreshed);
  });

  it("patches only the name when the reload failed", () => {
    expect(deliveryTypesAfterSuccessfulRename(null, "1", "Renamed", [source, other])).toEqual([
      { ...source, name: "Renamed" },
      other,
    ]);
  });
});
