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

import type { PublishServer } from "./types";
import { unwrapPublishServer } from "./deliveryServerFtpHost";

/**
 * PSX_PUBSERVER.NAME is VARCHAR(50). Keep in step with
 * {@code PSDeliveryServerRename.MAX_NAME_LENGTH}.
 */
export const DELIVERY_SERVER_NAME_MAX_LENGTH = 50;

export const DELIVERY_SERVER_NAME_REQUIRED = "Name is a required field.";

export const DELIVERY_SERVER_NAME_TOO_LONG =
  "Server name cannot have more than 50 characters.";

export const DELIVERY_SERVER_NAME_INVALID =
  "Invalid character in server name. Characters allowed are: a-z, 0-9, ., _ and -.";

export const DELIVERY_SERVER_NAME_DUPLICATE =
  "Publish server name already exists";

/** Same character class as {@code PSDeliveryServerRename} / {@code PSPubServerService}. */
const NAME_PATTERN = /^[\w.-]+$/;

export type DeliveryServerRenameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

export type DeliveryServerRenameHttpFailure =
  | "bad_request"
  | "forbidden"
  | "conflict";

export function deliveryServerDisplayName(server: PublishServer): string {
  return (server.serverName ?? server.name ?? "").trim();
}

export function isDefaultDeliveryServer(server: PublishServer): boolean {
  return Boolean(server.isDefault || server.defaultServer);
}

/**
 * Blank (after trim) is rejected and does not clear the stored name.
 * Overlong text, invalid characters, and a name already used on this site
 * (case-insensitive, excluding the current name) do not write.
 */
export function validateDeliveryServerRenameName(
  raw: string,
  currentName: string,
  siblingNames: readonly string[],
): DeliveryServerRenameResult {
  const name = raw.trim();
  if (!name) {
    return { ok: false, error: DELIVERY_SERVER_NAME_REQUIRED };
  }
  if (name.length > DELIVERY_SERVER_NAME_MAX_LENGTH) {
    return { ok: false, error: DELIVERY_SERVER_NAME_TOO_LONG };
  }
  if (!NAME_PATTERN.test(name)) {
    return { ok: false, error: DELIVERY_SERVER_NAME_INVALID };
  }
  const current = currentName.trim().toLowerCase();
  const taken = siblingNames.some((sibling) => {
    const other = sibling.trim().toLowerCase();
    return other !== "" && other !== current && other === name.toLowerCase();
  });
  if (taken) {
    return { ok: false, error: DELIVERY_SERVER_NAME_DUPLICATE };
  }
  return { ok: true, name };
}

/**
 * Name-only delivery-server PUT body. Driver, type, default flag, and
 * properties are omitted so the server leaves them stored.
 */
export function buildDeliveryServerRenameBody(name: string): {
  serverInfo: { serverName: string };
} {
  return { serverInfo: { serverName: name } };
}

/** Replace only the visible name. Properties, type, and default flag stay. */
export function withUpdatedDeliveryServerName(
  server: PublishServer,
  name: string,
): PublishServer {
  return { ...server, serverName: name, name };
}

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function deliveryServerRenameHttpFailure(
  status: number,
): DeliveryServerRenameHttpFailure | null {
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
