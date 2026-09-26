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
import { modelToSaveBody, serverToModel } from "./serverFormModel";

/** Property keys that must not be copied or shown when duplicating a server. */
const SECRET_PROPERTY_KEYS = new Set([
  "password",
  "securitykey",
  "accesskey",
  "secretkey",
]);

export type DeliveryServerCopyResult =
  | { ok: true; body: { serverInfo: Record<string, unknown> } }
  | { ok: false; reason: "blank" };

function withoutSecrets(
  properties: Record<string, string>,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(properties)) {
    if (SECRET_PROPERTY_KEYS.has(key.toLowerCase())) {
      continue;
    }
    out[key] = value;
  }
  return out;
}

/**
 * Build a create-server body for the same site: source driver and non-secret
 * settings, a new name, no server id, and not the site default.
 * Blank names are rejected here so the shell does not post an empty path.
 */
export function buildDeliveryServerCopy(
  source: PublishServer | Record<string, unknown>,
  requestedName: string,
): DeliveryServerCopyResult {
  const name = requestedName.trim();
  if (name === "") {
    return { ok: false, reason: "blank" };
  }
  const model = serverToModel(source);
  const properties = withoutSecrets(model.properties);
  properties.driver = model.driver;
  const body = modelToSaveBody({
    ...model,
    serverId: "",
    serverName: name,
    isDefault: false,
    properties,
  });
  return { ok: true, body };
}
