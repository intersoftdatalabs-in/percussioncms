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
import { validateDeliveryTypeCopyName } from "./deliveryTypeCopy";

export {
  DELIVERY_TYPE_NAME_MAX_LENGTH,
  DELIVERY_TYPE_NAME_REQUIRED,
  DELIVERY_TYPE_NAME_TOO_LONG,
} from "./deliveryTypeCopy";

export type DeliveryTypeRenameNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong rename before any update request. */
export function validateDeliveryTypeRenameName(
  raw: string,
): DeliveryTypeRenameNameResult {
  return validateDeliveryTypeCopyName(raw);
}

/**
 * Name-only {@code updateDeliveryType} body. Bean name, description, and
 * {@code unpublishingRequiresAssembly} are omitted so the server leaves them
 * stored. The id is the path parameter, not a field on this body.
 */
export function buildDeliveryTypeRenameBody(name: string): DeliveryTypeSummary {
  return { name };
}

/**
 * List to show after a rename succeeds. Prefer the refreshed rows. If the
 * refresh failed, keep every previous row and replace only the renamed name
 * so the bean name and description stay visible. Do not call this when the
 * update failed.
 */
export function deliveryTypesAfterSuccessfulRename(
  refreshed: DeliveryTypeSummary[] | null,
  deliveryTypeId: string,
  name: string,
  previous: DeliveryTypeSummary[],
): DeliveryTypeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.deliveryTypeId === deliveryTypeId ? { ...row, name } : row,
  );
}
