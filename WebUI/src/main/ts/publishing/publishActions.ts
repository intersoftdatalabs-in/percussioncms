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

import type { ApiError } from "../api/client";
import type { PublishActionState, PublishErrorToken } from "./types";

export interface PublishActionResult {
  state: PublishActionState;
  token?: PublishErrorToken;
  message?: string;
}

/**
 * Application-level failure status names returned by the sitemanage publish
 * endpoint ({@code PSSitePublishService}) when a pre-flight check fails or
 * publishing is not allowed. These are the {@code State.toString()} names
 * from {@code IPSPublisherJobStatus.State}; they are not HTTP errors.
 */
const PREFLIGHT_FAILURE_STATUSES = new Set<string>([
  "BADCONFIG",
  "BADCONFIGMULTIPLESITES",
  "PUBSERVERNEWDBCONFIG",
  "FORBIDDEN",
  "INVALID",
  "NOSTAGING_SERVERS",
  "RESTARTNEEDED",
]);

/**
 * Wire shape of the response body from the sitemanage publish endpoint. The
 * status field carries the {@code State.toString()} name for preflight
 * failures and the {@code State.getDisplayName()} for in-flight / completed
 * jobs; the legacy REST contract wraps the payload in a
 * {@code SitePublishResponse} key, but the field names are the same either
 * way.
 */
export interface SitePublishResponse {
  siteName?: string;
  status?: string;
  delivered?: string;
  failures?: string;
  warningMessage?: string;
  jobid?: number | string;
  [key: string]: unknown;
}

function readPublishResponse(data: unknown): SitePublishResponse | null {
  if (data == null) {
    return null;
  }
  if (typeof data !== "object") {
    return null;
  }
  const obj = data as Record<string, unknown>;
  const inner = (obj.SitePublishResponse ?? obj.publishResponse) as
    | Record<string, unknown>
    | undefined;
  if (inner && typeof inner === "object") {
    return inner as SitePublishResponse;
  }
  return obj as SitePublishResponse;
}

function stateForStatus(status: string): PublishActionState {
  if (
    status === "BADCONFIG" ||
    status === "BADCONFIGMULTIPLESITES" ||
    status === "PUBSERVERNEWDBCONFIG" ||
    status === "INVALID" ||
    status === "RESTARTNEEDED"
  ) {
    return "badconfig";
  }
  if (status === "FORBIDDEN") {
    return "forbidden";
  }
  return "error";
}

/**
 * Map API failures to publish action state (FORBIDDEN / BADCONFIG / generic).
 */
export function mapPublishError(error: unknown): PublishActionResult {
  const api = error as Partial<ApiError> | null;
  const status = api?.status;
  const bodyText = extractBodyText(api?.body);

  if (status === 403 || containsToken(bodyText, "FORBIDDEN")) {
    return {
      state: "forbidden",
      token: "FORBIDDEN",
      message: bodyText || "FORBIDDEN",
    };
  }
  if (containsToken(bodyText, "BADCONFIG")) {
    return {
      state: "badconfig",
      token: "BADCONFIG",
      message: bodyText || "BADCONFIG",
    };
  }
  if (containsToken(bodyText, "NOSTAGING_SERVERS")) {
    return {
      state: "error",
      token: "NOSTAGING_SERVERS",
      message: bodyText || "NOSTAGING_SERVERS",
    };
  }
  return {
    state: "error",
    message: bodyText || (api?.statusText ?? "Publish failed"),
  };
}

/**
 * Inspect a successful HTTP 200 publish response and return a non-null
 * {@link PublishActionResult} when the server is reporting an
 * application-level preflight failure (e.g. FTP connectivity failed, status
 * {@code BADCONFIG}). Returns {@code null} for in-flight or completed jobs
 * so the caller can proceed with the normal success UX.
 *
 * <p>Scope: this mapper only recognizes the preflight-failure state names
 * that {@code PSSitePublishService} writes via {@code State.toString()}. Job
 * states that the publisher writes via {@code State.getDisplayName()} (e.g.
 * {@code "Queuing content"}, {@code "Edition completed"},
 * {@code "Terminated abnormally"}, {@code "Cancelled by user"}) are
 * intentionally not treated as preflight failures; those are tracked via the
 * per-site job list, not the publish-response signal.</p>
 */
export function mapPublishResponse(
  data: unknown,
): PublishActionResult | null {
  const parsed = readPublishResponse(data);
  if (!parsed) {
    return null;
  }
  const status = (parsed.status ?? "").toString().trim();
  if (!status || !PREFLIGHT_FAILURE_STATUSES.has(status)) {
    return null;
  }
  const warning = (parsed.warningMessage ?? "").toString().trim();
  return {
    state: stateForStatus(status),
    token: status,
    message: warning || status,
  };
}

export function startPublishState(): PublishActionState {
  return "starting";
}

export function successPublishState(): PublishActionState {
  return "success";
}

function extractBodyText(body: unknown): string {
  if (body == null) {
    return "";
  }
  if (typeof body === "string") {
    return body;
  }
  if (typeof body === "object") {
    const o = body as Record<string, unknown>;
    for (const key of ["message", "error", "defaultMessage", "status"]) {
      if (typeof o[key] === "string") {
        return o[key] as string;
      }
    }
    try {
      return JSON.stringify(body);
    } catch {
      return "";
    }
  }
  return String(body);
}

function containsToken(text: string, token: string): boolean {
  return text.toUpperCase().includes(token.toUpperCase());
}
