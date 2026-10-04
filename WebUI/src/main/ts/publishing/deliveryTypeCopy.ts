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
 * PSX_DELIVERY_TYPE.NAME is VARCHAR(50). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_DELIVERY_TYPE_NAME_LENGTH}.
 */
export const DELIVERY_TYPE_NAME_MAX_LENGTH = 50;

export const DELIVERY_TYPE_NAME_REQUIRED = "Name is required";

export const DELIVERY_TYPE_NAME_TOO_LONG =
  "Delivery type name must be 50 characters or fewer";

export type DeliveryTypeCopyNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong copy name before any create request. */
export function validateDeliveryTypeCopyName(
  raw: string,
): DeliveryTypeCopyNameResult {
  const name = raw.trim();
  if (!name) {
    return { ok: false, error: DELIVERY_TYPE_NAME_REQUIRED };
  }
  if (name.length > DELIVERY_TYPE_NAME_MAX_LENGTH) {
    return { ok: false, error: DELIVERY_TYPE_NAME_TOO_LONG };
  }
  return { ok: true, name };
}

/**
 * Suggested name for the copy form. Empty when the suggestion would exceed
 * {@link DELIVERY_TYPE_NAME_MAX_LENGTH}.
 */
export function suggestedDeliveryTypeCopyName(
  sourceName: string | undefined,
): string {
  const base = (sourceName ?? "").trim();
  if (!base) {
    return "";
  }
  const suggestion = `${base} copy`;
  if (suggestion.length > DELIVERY_TYPE_NAME_MAX_LENGTH) {
    return "";
  }
  return suggestion;
}

/**
 * Body for the existing create-delivery-type API. Omits deliveryTypeId so
 * the server allocates a new row. Keeps the source bean name, description,
 * and unpublishing-requires-assembly flag. The visible name is the only
 * field the operator supplies.
 */
export function buildDeliveryTypeCopyBody(
  source: DeliveryTypeSummary,
  newName: string,
): DeliveryTypeSummary {
  const body: DeliveryTypeSummary = {
    name: newName,
    beanName: (source.beanName ?? "").trim(),
    unpublishingRequiresAssembly: source.unpublishingRequiresAssembly === true,
  };
  if (source.description != null) {
    body.description = source.description;
  }
  return body;
}

/**
 * List to show after create succeeds. Prefer the refreshed rows. If the
 * refresh failed or omitted the new row, append the created type so a
 * successful copy is visible. Restore any previous row the refresh dropped.
 * Do not call this when create failed.
 */
export function deliveryTypesAfterSuccessfulCopy(
  refreshed: DeliveryTypeSummary[] | null,
  created: DeliveryTypeSummary,
  previous: DeliveryTypeSummary[],
): DeliveryTypeSummary[] {
  const rows = [...(refreshed ?? previous)];
  const sourceIds = new Set(
    previous
      .map((row) => row.deliveryTypeId)
      .filter((id): id is string => !!id && id !== created.deliveryTypeId),
  );
  for (const id of sourceIds) {
    if (!rows.some((row) => row.deliveryTypeId === id)) {
      const source = previous.find((row) => row.deliveryTypeId === id);
      if (source) {
        rows.push(source);
      }
    }
  }
  const createdId = created.deliveryTypeId;
  const createdName = created.name?.trim();
  const present = rows.some((row) => {
    if (createdId && row.deliveryTypeId === createdId) {
      return true;
    }
    return !!createdName && row.name === createdName;
  });
  if (present) {
    return rows;
  }
  return [...rows, created];
}
