/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { del, get, post, put } from "../client";
import { PATHS } from "../paths";
import type { PublishServer } from "./types";

function normalizeServerList(data: unknown): PublishServer[] {
  if (data == null) {
    return [];
  }
  if (Array.isArray(data)) {
    return data as PublishServer[];
  }
  if (typeof data === "object") {
    const obj = data as Record<string, unknown>;
    for (const key of ["PubServer", "servers", "serverInfo", "items"]) {
      const v = obj[key];
      if (Array.isArray(v)) {
        return v as PublishServer[];
      }
      if (v && typeof v === "object") {
        return [v as PublishServer];
      }
    }
  }
  return [];
}

/** List publish servers for a site. */
export async function listServers(
  siteId: string | number,
): Promise<PublishServer[]> {
  const data = await get<unknown>(
    `${PATHS.PUB_SERVERS}${encodeURIComponent(String(siteId))}`,
  );
  return normalizeServerList(data);
}

/** Get one publish server. */
export async function getServer(
  siteId: string | number,
  serverId: string | number,
): Promise<PublishServer> {
  const data = await get<unknown>(
    `${PATHS.PUB_SERVERS}${encodeURIComponent(String(siteId))}/${encodeURIComponent(String(serverId))}`,
  );
  return (data ?? {}) as PublishServer;
}

/** Create a publish server. */
export async function createServer(
  siteId: string | number,
  serverName: string,
  body: unknown,
): Promise<unknown> {
  return post(
    `${PATHS.PUB_SERVERS}${encodeURIComponent(String(siteId))}/${encodeURIComponent(serverName)}`,
    body,
  );
}

/** Update a publish server. */
export async function updateServer(
  siteId: string | number,
  serverId: string | number,
  body: unknown,
): Promise<unknown> {
  return put(
    `${PATHS.PUB_SERVERS}${encodeURIComponent(String(siteId))}/${encodeURIComponent(String(serverId))}`,
    body,
  );
}

/** Delete a publish server. */
export async function deleteServer(
  siteId: string | number,
  serverId: string | number,
): Promise<unknown> {
  return del(
    `${PATHS.PUB_SERVERS}${encodeURIComponent(String(siteId))}/${encodeURIComponent(String(serverId))}`,
  );
}

/** Request stop for a running publish job. */
export async function stopPublishing(
  jobId: string | number,
): Promise<unknown> {
  return get(
    `${PATHS.PUB_SERVERS}stopPublishing/${encodeURIComponent(String(jobId))}`,
  );
}

export async function fetchAvailableDrivers(): Promise<unknown> {
  return get(`${PATHS.PUB_SERVERS}availableDrivers`);
}

export async function fetchAvailableRegions(): Promise<unknown> {
  return get(`${PATHS.PUB_SERVERS}availableRegions`);
}

export async function isEC2Instance(): Promise<unknown> {
  return get(`${PATHS.PUB_SERVERS}isEC2Instance`);
}

export async function fetchDefaultFolderLocation(
  siteId: string | number,
  publishType: string,
  driver: string,
  serverType: string,
): Promise<unknown> {
  return get(
    `${PATHS.PUB_SERVERS}defaultFolderLocation/` +
      `${encodeURIComponent(String(siteId))}/` +
      `${encodeURIComponent(publishType)}/` +
      `${encodeURIComponent(driver)}/` +
      `${encodeURIComponent(serverType)}`,
  );
}
