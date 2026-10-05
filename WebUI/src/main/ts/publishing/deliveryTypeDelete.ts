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
import { formatApiError, isApiError } from "../api/client";
import { message, MSG } from "../i18n/message";

/** Shown when DELETE 409 has no body. A content list still names this type. */
export const DELIVERY_TYPE_IN_USE = "Delivery type is in use";

/**
 * Map delivery-type DELETE failures to the Design error region.
 * HTTP 400 → bad id; HTTP 403 → forbidden; HTTP 409 → a content list still
 * names the type. Plain {@link ApiError} objects are not {@code Error}
 * instances — do not use {@code e.message} or the shell shows a generic miss.
 */
export function mapDeliveryTypeDeleteError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, DELIVERY_TYPE_IN_USE);
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Wire ids are strings in the type and often numbers in JSON
 * ({@code "deliveryTypeId":10001}). Never call {@code trim} on the raw value.
 */
function deliveryTypeIdText(value: unknown): string {
  if (typeof value === "string") {
    return value.trim();
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value);
  }
  return "";
}

function withoutDeliveryType(
  rows: DeliveryTypeSummary[],
  deletedId: string,
): DeliveryTypeSummary[] {
  return rows.filter((row) => deliveryTypeIdText(row.deliveryTypeId) !== deletedId);
}

/**
 * Delivery types to show after DELETE succeeds. Drop the deleted id from the
 * refreshed list, or from the previous rows when refresh failed. Do not call
 * this when DELETE failed — the type must stay.
 */
export function deliveryTypesAfterSuccessfulDelete(
  refreshed: DeliveryTypeSummary[] | null,
  deletedId: string | number,
  previous: DeliveryTypeSummary[],
): DeliveryTypeSummary[] {
  const id = deliveryTypeIdText(deletedId);
  if (!id) {
    return refreshed ?? previous;
  }
  return withoutDeliveryType(refreshed ?? previous, id);
}
