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

/**
 * Logical types for the unified Publishing UI (data-model.md).
 * Field names track existing REST DTOs where known.
 */

export type PublishSection =
  | "sites"
  | "status"
  | "logs"
  | "design"
  | "runtime";

export type SiteListViewMode = "card" | "list";

export interface PublishSiteSummary {
  name: string;
  id?: string | number;
  siteId?: string | number;
  folderPath?: string;
  [key: string]: unknown;
}

export type ServerType = "PRODUCTION" | "STAGING" | string;
export type ServerDeliveryType = "File" | "Database" | string;

export interface PublishServerProperty {
  key?: string;
  name?: string;
  value?: string;
  [key: string]: unknown;
}

export interface PublishServer {
  serverId?: string | number;
  serverName?: string;
  name?: string;
  siteId?: string | number;
  serverType?: ServerType;
  type?: ServerDeliveryType;
  isDefault?: boolean;
  defaultServer?: boolean;
  properties?: PublishServerProperty[] | Record<string, string>;
  [key: string]: unknown;
}

export interface PublishingJob {
  jobId?: string | number;
  siteName?: string;
  siteId?: string | number;
  serverName?: string;
  status?: string;
  startTime?: string;
  startDate?: string;
  elapsedTime?: number | string;
  completedItems?: number;
  totalItems?: number;
  isStopping?: boolean;
  [key: string]: unknown;
}

export interface SitePublishLogRequest {
  siteId?: string | number;
  serverId?: string | number;
  days?: number;
  maxcount?: number;
  [key: string]: unknown;
}

export interface PublishingLogEntry {
  jobId?: string | number;
  siteName?: string;
  serverName?: string;
  status?: string;
  startDate?: string;
  [key: string]: unknown;
}

export interface IncrementalQueuePage {
  items?: unknown[];
  startIndex?: number;
  pageSize?: number;
  totalCount?: number;
  [key: string]: unknown;
}

/** Known server error tokens for publish ops (ops-publish-api.md). */
export type PublishErrorToken =
  | "FORBIDDEN"
  | "BADCONFIG"
  | "NOSTAGING_SERVERS"
  | string;

export type PublishActionState =
  | "idle"
  | "starting"
  | "success"
  | "error"
  | "forbidden"
  | "badconfig";
