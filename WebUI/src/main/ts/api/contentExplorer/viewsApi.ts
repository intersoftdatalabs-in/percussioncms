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
 * Explorer Views catalog + V1 execute client (#3116).
 *
 * <p>List reuses {@link listViews} from the Developer catalog client
 * ({@code GET /services/views}). Execute is the product contract from
 * #3115: {@code POST /services/views/{idOrName}/execute}. Custom URL
 * views (Inbox family) are expected to return HTTP 400 — the Explorer
 * tree surfaces that as unsupported rather than running Inbox (#3118).</p>
 */

import { post } from "../client";
import { PATHS } from "../paths";
import { listViews } from "../developer/viewsApi";
import type {
  ViewExecuteRequest,
  ViewExecuteResult,
  ViewResultItem,
} from "../developer/types";

export { listViews };

/**
 * Unwrap Jackson root-name wrapping for {@link ViewExecuteResult}
 * ({@code ViewExecuteResult} / camelCase aliases) while accepting a flat
 * payload when root wrapping is off.
 */
export function unwrapViewExecuteResult(payload: unknown): ViewExecuteResult {
  if (payload == null || typeof payload !== "object") {
    return { children: [], totalCount: 0, startIndex: 1 };
  }
  const root = payload as Record<string, unknown>;
  const nested =
    root.ViewExecuteResult ??
    root.viewExecuteResult ??
    (Array.isArray(root.children) ||
    typeof root.totalCount === "number" ||
    typeof root.startIndex === "number"
      ? root
      : null);
  if (nested == null || typeof nested !== "object") {
    return { children: [], totalCount: 0, startIndex: 1 };
  }
  const body = nested as ViewExecuteResult;
  const children = Array.isArray(body.children)
    ? (body.children as ViewResultItem[])
    : [];
  return {
    children,
    totalCount: typeof body.totalCount === "number" ? body.totalCount : children.length,
    startIndex: typeof body.startIndex === "number" ? body.startIndex : 1,
    viewName: body.viewName,
    displayFormatId: body.displayFormatId,
  };
}

/**
 * POST /services/views/{idOrName}/execute — run a standard CX design view.
 *
 * <p>Empty or blank {@code idOrName} rejects client-side before the network
 * call so a tree leaf cannot fire a meaningless path segment.</p>
 */
export async function executeView(
  idOrName: string,
  request?: ViewExecuteRequest | null,
): Promise<ViewExecuteResult> {
  const key = (idOrName ?? "").trim();
  if (!key) {
    throw new Error("View id or name is required");
  }
  const pathKey = encodeURIComponent(key);
  const body = request ?? {};
  const payload = await post<unknown>(
    `${PATHS.VIEWS}/${pathKey}/execute`,
    body,
  );
  return unwrapViewExecuteResult(payload);
}
