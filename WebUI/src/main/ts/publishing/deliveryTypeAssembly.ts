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

import type { DeliveryTypeSummary } from "../api/publishing/designApi";

/**
 * A missing {@code unpublishingRequiresAssembly} is false. Only an explicit
 * true is on.
 */
export function deliveryTypeAssemblyOn(
  row: Pick<DeliveryTypeSummary, "unpublishingRequiresAssembly"> | null | undefined,
): boolean {
  return row?.unpublishingRequiresAssembly === true;
}

/** List text for the flag. Missing stays "No" until a successful save. */
export function deliveryTypeAssemblyLabel(
  row: Pick<DeliveryTypeSummary, "unpublishingRequiresAssembly"> | null | undefined,
): "Yes" | "No" {
  return deliveryTypeAssemblyOn(row) ? "Yes" : "No";
}

/**
 * Flag-only {@code updateDeliveryType} body. Publishing design already leaves
 * an omitted name, description, and bean stored, so those fields are not
 * resent. Explicit false is included so an omitted flag is not treated as
 * "leave the stored value". The id is the path parameter.
 */
export function buildDeliveryTypeAssemblyBody(
  unpublishingRequiresAssembly: boolean,
): DeliveryTypeSummary {
  return { unpublishingRequiresAssembly };
}

/**
 * List to show after an assembly-flag save succeeds. Prefer the refreshed
 * rows. If the refresh failed, keep every previous row and replace only the
 * flag so the name, description, and bean stay visible. Do not call this when
 * the update failed.
 */
export function deliveryTypesAfterSuccessfulAssembly(
  refreshed: DeliveryTypeSummary[] | null,
  deliveryTypeId: string,
  unpublishingRequiresAssembly: boolean,
  previous: DeliveryTypeSummary[],
): DeliveryTypeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.deliveryTypeId === deliveryTypeId
      ? { ...row, unpublishingRequiresAssembly }
      : row,
  );
}
