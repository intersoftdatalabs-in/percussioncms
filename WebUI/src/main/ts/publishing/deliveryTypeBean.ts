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
 * PSX_DELIVERY_TYPE.BEAN_NAME is VARCHAR(255). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_DELIVERY_TYPE_BEAN_NAME_LENGTH}.
 */
export const DELIVERY_TYPE_BEAN_NAME_MAX_LENGTH = 255;

export const DELIVERY_TYPE_BEAN_NAME_REQUIRED = "Bean name is required";

export const DELIVERY_TYPE_BEAN_NAME_TOO_LONG =
  "Delivery type bean name must be 255 characters or fewer";

export type DeliveryTypeBeanNameResult =
  | { ok: true; beanName: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is rejected and does not clear the stored bean.
 * Overlong text is rejected before any update request.
 */
export function validateDeliveryTypeBeanName(raw: string): DeliveryTypeBeanNameResult {
  const beanName = raw.trim();
  if (!beanName) {
    return { ok: false, error: DELIVERY_TYPE_BEAN_NAME_REQUIRED };
  }
  if (beanName.length > DELIVERY_TYPE_BEAN_NAME_MAX_LENGTH) {
    return { ok: false, error: DELIVERY_TYPE_BEAN_NAME_TOO_LONG };
  }
  return { ok: true, beanName };
}

/**
 * Bean-only {@code updateDeliveryType} body. Name, description, and
 * {@code unpublishingRequiresAssembly} are omitted so the server leaves them
 * stored. The id is the path parameter.
 */
export function buildDeliveryTypeBeanBody(beanName: string): DeliveryTypeSummary {
  return { beanName };
}

/**
 * List to show after a bean-name save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the bean
 * name so the name and description stay visible. Do not call this when the
 * update failed.
 */
export function deliveryTypesAfterSuccessfulBean(
  refreshed: DeliveryTypeSummary[] | null,
  deliveryTypeId: string,
  beanName: string,
  previous: DeliveryTypeSummary[],
): DeliveryTypeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.deliveryTypeId === deliveryTypeId ? { ...row, beanName } : row,
  );
}
