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
  DELIVERY_TYPE_BEAN_NAME_MAX_LENGTH,
  DELIVERY_TYPE_BEAN_NAME_REQUIRED,
  DELIVERY_TYPE_BEAN_NAME_TOO_LONG,
  buildDeliveryTypeBeanBody,
  deliveryTypesAfterSuccessfulBean,
  validateDeliveryTypeBeanName,
} from "@/publishing/deliveryTypeBean";

const source: DeliveryTypeSummary = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "Publish content to the filesystem",
  unpublishingRequiresAssembly: true,
};

describe("validateDeliveryTypeBeanName", () => {
  it("rejects blank text", () => {
    expect(validateDeliveryTypeBeanName("   ")).toEqual({
      ok: false,
      error: DELIVERY_TYPE_BEAN_NAME_REQUIRED,
    });
  });

  it("rejects an overlong bean name", () => {
    expect(
      validateDeliveryTypeBeanName("b".repeat(DELIVERY_TYPE_BEAN_NAME_MAX_LENGTH + 1)),
    ).toEqual({
      ok: false,
      error: DELIVERY_TYPE_BEAN_NAME_TOO_LONG,
    });
  });

  it("trims a bean name that fits", () => {
    expect(validateDeliveryTypeBeanName("  sys_ftpDeliveryHandler  ")).toEqual({
      ok: true,
      beanName: "sys_ftpDeliveryHandler",
    });
  });
});

describe("buildDeliveryTypeBeanBody", () => {
  it("sends only the bean name", () => {
    expect(buildDeliveryTypeBeanBody("sys_ftpDeliveryHandler")).toEqual({
      beanName: "sys_ftpDeliveryHandler",
    });
    expect(buildDeliveryTypeBeanBody("sys_ftpDeliveryHandler")).not.toHaveProperty("name");
    expect(buildDeliveryTypeBeanBody("sys_ftpDeliveryHandler")).not.toHaveProperty(
      "description",
    );
    expect(buildDeliveryTypeBeanBody("sys_ftpDeliveryHandler")).not.toHaveProperty(
      "unpublishingRequiresAssembly",
    );
    expect(buildDeliveryTypeBeanBody("sys_ftpDeliveryHandler")).not.toHaveProperty(
      "deliveryTypeId",
    );
  });
});

describe("deliveryTypesAfterSuccessfulBean", () => {
  const other: DeliveryTypeSummary = {
    deliveryTypeId: "2",
    name: "ftp",
    beanName: "sys_ftpDeliveryHandler",
    description: "ftp",
  };

  it("prefers the refreshed list", () => {
    const refreshed = [{ ...source, beanName: "sys_sftpDeliveryHandler" }, other];
    expect(
      deliveryTypesAfterSuccessfulBean(refreshed, "1", "sys_sftpDeliveryHandler", [
        source,
        other,
      ]),
    ).toBe(refreshed);
  });

  it("keeps name and description when the refresh failed", () => {
    expect(
      deliveryTypesAfterSuccessfulBean(null, "1", "sys_sftpDeliveryHandler", [source, other]),
    ).toEqual([
      { ...source, beanName: "sys_sftpDeliveryHandler" },
      other,
    ]);
  });
});
