/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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

import type { PublishServer } from "./types";
import { unwrapPublishServer } from "./deliveryServerFtpHost";
import { isDefaultDeliveryServer } from "./deliveryServerRename";

export const DELIVERY_SERVER_DEFAULT_REQUIRED =
  "Default flag must be true to mark a delivery server as default.";

export const DELIVERY_SERVER_ALREADY_DEFAULT =
  "This server is already the default.";

export type DeliveryServerDefaultHttpFailure =
  | "bad_request"
  | "forbidden"
  | "conflict";

export { isDefaultDeliveryServer };

/**
 * Default-flag-only delivery-server PUT body. Name, driver, type, and
 * properties are omitted so the server leaves them stored.
 */
export function buildDeliveryServerDefaultBody(): {
  serverInfo: { isDefault: true };
} {
  return { serverInfo: { isDefault: true } };
}

/** Mark only the default flag. Name, driver, type, and properties stay. */
export function withUpdatedDeliveryServerDefault(
  server: PublishServer,
  isDefault: boolean,
): PublishServer {
  return { ...server, isDefault, defaultServer: isDefault };
}

/** True when the selected server is already the site default. Not a write. */
export function shouldSkipDefaultWrite(server: PublishServer): boolean {
  return isDefaultDeliveryServer(server);
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function deliveryServerDefaultHttpFailure(
  status: number,
): DeliveryServerDefaultHttpFailure | null {
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 409) {
    return "conflict";
  }
  return null;
}

export { unwrapPublishServer };
