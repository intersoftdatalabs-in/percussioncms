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

import { get, post } from "../client";
import {
  parseItemScheduleDates,
  type ItemScheduleDates,
} from "../../contentExplorer/itemScheduleDates";
import { itemPublishPaths } from "../../publishing/itemPublishPaths";
import { mapPublishResponse } from "../../publishing/publishActions";

export type { ItemScheduleDates };

/**
 * GET classic {@code /itemmanagement/item/getitemdates/{id}}.
 */
export async function fetchItemScheduleDates(
  itemId: string,
): Promise<ItemScheduleDates> {
  const id = itemId.trim();
  if (!id) {
    return { itemId: "", startDate: "", endDate: "", comments: "" };
  }
  const body = await get<unknown>(
    `${itemPublishPaths().getItemDates}/${encodeURIComponent(id)}`,
  );
  return parseItemScheduleDates(body, id);
}

/**
 * POST {@code ItemDates}. HTTP 400 (invalid dates) and 403 (forbidden)
 * throw {@code ApiError}. HTTP 200 {@code FORBIDDEN}/{@code INVALID}/
 * {@code BADCONFIG} is also a failure ({@code mapPublishResponse}).
 */
export async function saveItemScheduleDates(
  dates: ItemScheduleDates,
): Promise<void> {
  const itemId = dates.itemId.trim();
  if (!itemId) {
    throw new Error("itemId");
  }
  const envelope = {
    ItemDates: {
      itemId,
      startDate: dates.startDate ?? "",
      endDate: dates.endDate ?? "",
      comments: dates.comments ?? "",
    },
  };
  const body = await post<unknown>(itemPublishPaths().setItemDates, envelope);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Schedule failed");
  }
}
