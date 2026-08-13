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
 * #3115: {@code POST /services/views/{idOrName}/execute}. The Inbox
 * custom-URL leaf uses the same execute contract (C1 / #3239). Other
 * custom-URL views stay listed but unsupported in Explorer (#3240).</p>
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
 * Jackson / JAXB root for {@link ViewExecuteRequest}
 * ({@code @XmlRootElement(name = "ViewExecuteRequest")}). CXF
 * {@code UNWRAP_ROOT_VALUE} rejects a bare {@code startIndex} field
 * (QA #3244 / #3318).
 */
export const VIEW_EXECUTE_REQUEST_ROOT = "ViewExecuteRequest";

/** Wire envelope required by WRAP_ROOT_VALUE / JAXB on view execute. */
export type ViewExecuteRequestEnvelope = {
  ViewExecuteRequest: ViewExecuteRequest;
};

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Wrap execute overrides under {@link VIEW_EXECUTE_REQUEST_ROOT}.
 * Does not double-wrap an already-enveloped payload.
 */
export function wrapViewExecuteRequest(
  request?: ViewExecuteRequest | ViewExecuteRequestEnvelope | null,
): ViewExecuteRequestEnvelope {
  const rec = asRecord(request);
  if (rec != null) {
    const nested = rec[VIEW_EXECUTE_REQUEST_ROOT];
    if (asRecord(nested) != null) {
      return { ViewExecuteRequest: nested as ViewExecuteRequest };
    }
  }
  return { ViewExecuteRequest: (request as ViewExecuteRequest) ?? {} };
}

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
  const payload = await post<unknown>(
    `${PATHS.VIEWS}/${pathKey}/execute`,
    wrapViewExecuteRequest(request),
  );
  return unwrapViewExecuteResult(payload);
}
