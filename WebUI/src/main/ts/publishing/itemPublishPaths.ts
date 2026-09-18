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

import { SERVICES_ROOT } from "../api/paths";

/**
 * Item-level publish path constants (US6). Remain on sitemanage /publish/* —
 * jQuery PercItemPublisherService continues to call these after shell cutover.
 */
export function itemPublishPaths(root = SERVICES_ROOT): {
  pagePublish: string;
  resourcePublish: string;
  pageTakedown: string;
  resourceTakedown: string;
  pageStaging: string;
  resourceStaging: string;
  pageStagingTakedown: string;
  resourceStagingTakedown: string;
  publishingActions: string;
  sitePublish: string;
  /** Pages that link to an item — classic Finder takedown confirm. */
  linkedItems: string;
  /** GET …/getitemdates/{id} — classic PercScheduleDialog. */
  getItemDates: string;
  /** POST …/setitemdates — ItemDates envelope. */
  setItemDates: string;
} {
  const base = `${root}/sitemanage/publish`;
  const itemBase = `${root}/itemmanagement/item`;
  return {
    pagePublish: `${base}/page`,
    resourcePublish: `${base}/resource`,
    pageTakedown: `${base}/takedown/page`,
    resourceTakedown: `${base}/takedown/resource`,
    pageStaging: `${base}/page/staging`,
    resourceStaging: `${base}/resource/staging`,
    pageStagingTakedown: `${base}/takedown/page/staging`,
    resourceStagingTakedown: `${base}/takedown/resource/staging`,
    publishingActions: `${base}/publishingActions`,
    sitePublish: base,
    linkedItems: `${itemBase}/findLinkedItems`,
    getItemDates: `${itemBase}/getitemdates`,
    setItemDates: `${itemBase}/setitemdates`,
  };
}

/** Build modern Publishing shell deep link for status/logs. */
export function publishingShellHref(opts: {
  section?: "status" | "logs" | "sites" | "design" | "runtime";
  siteId?: string;
  serverId?: string;
}): string {
  const params = new URLSearchParams();
  params.set("view", "publish");
  if (opts.section) {
    params.set("section", opts.section);
  }
  if (opts.siteId) {
    params.set("siteId", opts.siteId);
  }
  if (opts.serverId) {
    params.set("serverId", opts.serverId);
  }
  return `/cm/app/?${params.toString()}`;
}
