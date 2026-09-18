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

/** Classic PercItemPublisherService itemType Page vs Asset. */
export type TakedownKind = "page" | "resource";

/**
 * Map a query or form value to page vs resource. Asset aliases resource.
 * Unknown values default to page (site-workspace pages).
 */
export function mapTakedownKind(
  raw: string | null | undefined,
): TakedownKind {
  const normalized = (raw ?? "").trim().toLowerCase();
  if (
    normalized === "resource" ||
    normalized === "asset" ||
    normalized === "assets"
  ) {
    return "resource";
  }
  return "page";
}

/**
 * Synthetic Explorer item so {@code takedownSelectedItem} keeps one contract.
 * CMS paths always use {@code /} (not OS file separators).
 */
export function pathItemForTakedown(
  itemId: string,
  kind: TakedownKind,
): PSPathItem {
  const id = itemId.trim();
  if (kind === "resource") {
    return {
      id,
      name: id,
      path: `/Assets/${id}`,
      type: "percAsset",
      category: "asset",
      leaf: true,
    };
  }
  return {
    id,
    name: id,
    path: `/Sites/${id}`,
    type: "percPage",
    category: "page",
    leaf: true,
  };
}

/**
 * Path-style Publishing deep link for takedown on the site workspace.
 */
export function itemTakedownShellHref(opts: {
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

export function spaItemTakedownHref(opts: {
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
