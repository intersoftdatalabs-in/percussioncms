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

/** PublishingShell site-workspace stage action. */
export type StageAction = "stage" | "unstage";

/** Page vs asset classification shared with takedown and publish-now. */
export type StageKind = TakedownKind;

export const mapStageKind = mapTakedownKind;

/**
 * Synthetic Explorer item so {@code stageSelectedItem} /
 * {@code removeFromStagingSelectedItem} keep one contract.
 * CMS paths always use {@code /} (not OS file separators).
 */
export function pathItemForStage(
  itemId: string,
  kind: StageKind,
): PSPathItem {
  return pathItemForTakedown(itemId, kind);
}

/**
 * Map a query or form value to the stage action. Unknown values
 * default to {@code "stage"} (the more common shell entry path).
 */
export function mapStageAction(
  raw: string | null | undefined,
): StageAction {
  const normalized = (raw ?? "").trim().toLowerCase();
  if (normalized === "unstage" || normalized === "remove_from_staging") {
    return "unstage";
  }
  return "stage";
}

/**
 * Path-style Publishing deep link for stage / remove-from-staging on the
 * site workspace.
 */
export function itemStageShellHref(opts: {
  siteId?: string;
  itemId?: string;
  action?: StageAction;
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
  if (opts.action) {
    params.set("action", opts.action);
  }
  const q = params.toString();
  return q ? `/cm/app/publish/sites?${q}` : "/cm/app/publish/sites";
}

export function spaItemStageHref(opts: {
  siteId?: string;
  itemId?: string;
  action?: StageAction;
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
  if (opts.action) {
    params.set("action", opts.action);
  }
  return `/cm/app/spa.jsp?${params.toString()}`;
}
