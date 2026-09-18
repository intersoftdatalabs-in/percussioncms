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

import type { PSPathItem } from "../api/contentExplorer/types";
import { mapIdParam } from "./deepLinkMap";
import {
  mapTakedownKind,
  pathItemForTakedown,
  type TakedownKind,
} from "./itemTakedown";

/** Same page vs resource classification as site-workspace takedown. */
export type PublishNowKind = TakedownKind;

export const mapPublishNowKind = mapTakedownKind;

/**
 * Synthetic Explorer item so {@code publishSelectedItem} keeps one contract.
 * CMS paths always use {@code /} (not OS file separators).
 */
export function pathItemForPublishNow(
  itemId: string,
  kind: PublishNowKind,
): PSPathItem {
  return pathItemForTakedown(itemId, kind);
}

/** Path-style Publishing deep link for publish-now on the site workspace. */
export function itemPublishNowShellHref(opts: {
  siteId?: string;
  itemId?: string;
}): string {
  const params = new URLSearchParams();
  const siteId = mapIdParam(opts.siteId);
  const itemId = mapIdParam(opts.itemId);
  if (siteId) {
    params.set("siteId", siteId);
  }
  if (itemId) {
    params.set("itemId", itemId);
  }
  const q = params.toString();
  return q ? `/cm/app/publish/sites?${q}` : "/cm/app/publish/sites";
}

export function spaItemPublishNowHref(opts: {
  siteId?: string;
  itemId?: string;
}): string {
  const params = new URLSearchParams();
  params.set("entry", "publish");
  params.set("section", "sites");
  const siteId = mapIdParam(opts.siteId);
  const itemId = mapIdParam(opts.itemId);
  if (siteId) {
    params.set("siteId", siteId);
  }
  if (itemId) {
    params.set("itemId", itemId);
  }
  return `/cm/app/spa.jsp?${params.toString()}`;
}
