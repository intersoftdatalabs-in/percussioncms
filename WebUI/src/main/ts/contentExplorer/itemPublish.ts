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

/**
 * Explorer Publish Now — existing sitemanage publish-now GET, not the
 * demandpublishing servlet page (that 404s from the SPA).
 */

import { get } from "../api/client";
import type { PSPathItem } from "../api/contentExplorer/types";
import { itemPublishPaths } from "../publishing/itemPublishPaths";
import { resolvePreviewKind } from "./previewItem";

/**
 * Demand-publish a page or asset. Other types return false so the
 * dispatcher can show that the action is not available.
 */
export async function publishSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePreviewKind(item);
  const paths = itemPublishPaths();
  if (kind === "page") {
    await get<unknown>(`${paths.pagePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  if (kind === "asset") {
    await get<unknown>(`${paths.resourcePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  return false;
}
