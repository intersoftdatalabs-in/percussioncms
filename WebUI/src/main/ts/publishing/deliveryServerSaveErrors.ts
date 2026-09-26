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
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { formatApiError, isApiError } from "../api/client";
import { message, MSG } from "../i18n/message";

/**
 * Map site delivery-server create/update failures to operator-visible text.
 * HTTP 403 → forbidden; HTTP 409 → duplicate server name.
 */
export function mapDeliveryServerSaveError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_SERVER_NAME_CONFLICT));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Map copy-as-create failures. HTTP 400 is a blank/invalid name; HTTP 409 is
 * a name already used on this site. Bodies are shown as-is and must not be
 * expected to contain credentials (the copy payload omits secret properties).
 */
export function mapDeliveryServerCopyError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_COPY_SERVER_BLANK));
    }
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_SERVER_NAME_CONFLICT));
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * Map delivery-server delete failures to operator-visible text.
 * HTTP 403 → forbidden. In-use / default-server / other HTTP bodies stay as text.
 */
export function mapDeliveryServerDeleteError(err: unknown): string {
  if (isApiError(err) && err.status === 403) {
    return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}
