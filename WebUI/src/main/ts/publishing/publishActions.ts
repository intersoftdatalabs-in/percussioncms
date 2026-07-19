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

import type { ApiError } from "../api/client";
import type { PublishActionState, PublishErrorToken } from "./types";

export interface PublishActionResult {
  state: PublishActionState;
  token?: PublishErrorToken;
  message?: string;
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
