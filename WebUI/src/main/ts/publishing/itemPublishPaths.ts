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
  publishingActions: string;
  sitePublish: string;
} {
  const base = `${root}/sitemanage/publish`;
  return {
    pagePublish: `${base}/page`,
    resourcePublish: `${base}/resource`,
    pageTakedown: `${base}/takedown/page`,
    resourceTakedown: `${base}/takedown/resource`,
    publishingActions: `${base}/publishingActions`,
    sitePublish: base,
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
