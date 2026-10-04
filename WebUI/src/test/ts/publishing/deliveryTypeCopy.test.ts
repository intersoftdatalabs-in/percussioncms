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
  DELIVERY_TYPE_NAME_REQUIRED,
  DELIVERY_TYPE_NAME_TOO_LONG,
  buildDeliveryTypeCopyBody,
  deliveryTypesAfterSuccessfulCopy,
  suggestedDeliveryTypeCopyName,
  validateDeliveryTypeCopyName,
} from "@/publishing/deliveryTypeCopy";

const source: DeliveryTypeSummary = {
  deliveryTypeId: "1",
  name: "filesystem",
  beanName: "sys_fileDeliveryHandler",
  description: "Publish content to the filesystem",
  unpublishingRequiresAssembly: true,
};

describe("validateDeliveryTypeCopyName", () => {
  it("rejects a blank name", () => {
    expect(validateDeliveryTypeCopyName("   ")).toEqual({
      ok: false,
      error: DELIVERY_TYPE_NAME_REQUIRED,
    });
  });

  it("rejects a name longer than the NAME column", () => {
    const raw = "n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH + 1);
    expect(validateDeliveryTypeCopyName(raw)).toEqual({
      ok: false,
      error: DELIVERY_TYPE_NAME_TOO_LONG,
    });
  });

  it("accepts a trimmed name at the column limit", () => {
    const raw = `  ${"n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH)}  `;
    expect(validateDeliveryTypeCopyName(raw)).toEqual({
      ok: true,
      name: "n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH),
    });
  });
});

describe("suggestedDeliveryTypeCopyName", () => {
  it("suggests the source name plus copy", () => {
    expect(suggestedDeliveryTypeCopyName("filesystem")).toBe("filesystem copy");
  });

  it("does not suggest a name that would be overlong", () => {
    expect(
      suggestedDeliveryTypeCopyName("n".repeat(DELIVERY_TYPE_NAME_MAX_LENGTH)),
    ).toBe("");
  });
});

describe("buildDeliveryTypeCopyBody", () => {
  it("keeps bean, description, and assembly flag and drops the source id", () => {
    const body = buildDeliveryTypeCopyBody(source, "filesystem copy");
    expect(body.deliveryTypeId).toBeUndefined();
    expect(body).toEqual({
      name: "filesystem copy",
      beanName: "sys_fileDeliveryHandler",
      description: "Publish content to the filesystem",
      unpublishingRequiresAssembly: true,
    });
  });

  it("omits a missing description and sends assembly false", () => {
    const body = buildDeliveryTypeCopyBody(
      { deliveryTypeId: "2", name: "ftp", beanName: " sys_ftpDeliveryHandler " },
      "ftp copy",
    );
    expect(body).toEqual({
      name: "ftp copy",
      beanName: "sys_ftpDeliveryHandler",
      unpublishingRequiresAssembly: false,
    });
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("deliveryTypeId");
  });
});

describe("deliveryTypesAfterSuccessfulCopy", () => {
  const created: DeliveryTypeSummary = {
    deliveryTypeId: "9",
    name: "filesystem copy",
    beanName: "sys_fileDeliveryHandler",
  };

  it("uses the refreshed list when the new row is already there", () => {
    const refreshed = [source, created];
    expect(deliveryTypesAfterSuccessfulCopy(refreshed, created, [source])).toEqual(
      refreshed,
    );
  });

  it("appends the created row and keeps the source when refresh fails", () => {
    expect(deliveryTypesAfterSuccessfulCopy(null, created, [source])).toEqual([
      source,
      created,
    ]);
  });

  it("restores a source row a partial refresh dropped", () => {
    expect(deliveryTypesAfterSuccessfulCopy([created], created, [source])).toEqual([
      created,
      source,
    ]);
  });
});
