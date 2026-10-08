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
  buildDeliveryTypeAssemblyBody,
  deliveryTypeAssemblyLabel,
  deliveryTypeAssemblyOn,
  deliveryTypesAfterSuccessfulAssembly,
} from "@/publishing/deliveryTypeAssembly";

const source: DeliveryTypeSummary = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "Publish content to the filesystem",
  unpublishingRequiresAssembly: true,
};

describe("deliveryTypeAssemblyOn", () => {
  it("defaults a missing flag to false", () => {
    expect(deliveryTypeAssemblyOn(undefined)).toBe(false);
    expect(deliveryTypeAssemblyOn({})).toBe(false);
    expect(deliveryTypeAssemblyOn({ unpublishingRequiresAssembly: false })).toBe(false);
    expect(deliveryTypeAssemblyLabel({})).toBe("No");
  });

  it("treats only explicit true as on", () => {
    expect(deliveryTypeAssemblyOn(source)).toBe(true);
    expect(deliveryTypeAssemblyLabel(source)).toBe("Yes");
  });
});

describe("buildDeliveryTypeAssemblyBody", () => {
  it("sends only the flag, including explicit false", () => {
    expect(buildDeliveryTypeAssemblyBody(true)).toEqual({
      unpublishingRequiresAssembly: true,
    });
    expect(buildDeliveryTypeAssemblyBody(false)).toEqual({
      unpublishingRequiresAssembly: false,
    });
    for (const body of [
      buildDeliveryTypeAssemblyBody(true),
      buildDeliveryTypeAssemblyBody(false),
    ]) {
      expect(body).not.toHaveProperty("name");
      expect(body).not.toHaveProperty("description");
      expect(body).not.toHaveProperty("beanName");
      expect(body).not.toHaveProperty("deliveryTypeId");
    }
  });
});

describe("deliveryTypesAfterSuccessfulAssembly", () => {
  const other: DeliveryTypeSummary = {
    deliveryTypeId: "2",
    name: "ftp",
    beanName: "sys_ftpDeliveryHandler",
    description: "ftp",
    unpublishingRequiresAssembly: false,
  };

  it("prefers the refreshed list", () => {
    const refreshed = [{ ...source, unpublishingRequiresAssembly: false }, other];
    expect(
      deliveryTypesAfterSuccessfulAssembly(refreshed, "1", false, [source, other]),
    ).toBe(refreshed);
  });

  it("keeps name, description, and bean when the refresh failed", () => {
    expect(deliveryTypesAfterSuccessfulAssembly(null, "1", false, [source, other])).toEqual([
      { ...source, unpublishingRequiresAssembly: false },
      other,
    ]);
  });
});
