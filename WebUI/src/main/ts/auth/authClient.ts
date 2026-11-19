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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { withCmsContextPrefix } from "../api/paths";
import type { AuthBootstrap } from "./types";

export class AuthClientError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, message: string, code = "") {
    super(message);
    this.name = "AuthClientError";
    this.status = status;
    this.code = code;
  }
}

/** Resolves an unauthenticated auth servlet route respecting context path or reverse-proxy prefix. */
export function resolveAuthEndpoint(
  bootstrap: Pick<AuthBootstrap, "contextPath">,
  subPath: string,
): string {
  const cleanSub = subPath.startsWith("/") ? subPath : `/${subPath}`;
  const ctx = (bootstrap.contextPath ?? "").replace(/\/+$/, "");
  if (ctx) {
    return `${ctx}/auth${cleanSub}`;
  }
  return withCmsContextPrefix(`/auth${cleanSub}`);
}

/** Resolves login return link respecting context path. */
export function resolveLoginUrl(
  bootstrap: Pick<AuthBootstrap, "contextPath">,
): string {
  const ctx = (bootstrap.contextPath ?? "").replace(/\/+$/, "");
  if (ctx) {
    return `${ctx}/login`;
  }
  return withCmsContextPrefix("/login");
}

function extractErrorMessage(payload: unknown, fallback: string): {
  message: string;
  code: string;
} {
  if (payload != null && typeof payload === "object") {
    const rec = payload as Record<string, unknown>;
    const msg = typeof rec.message === "string" && rec.message.trim()
      ? rec.message.trim()
      : fallback;
    const code = typeof rec.error === "string"
      ? rec.error
      : typeof rec.status === "string"
        ? rec.status
        : "";
    return { message: msg, code };
  }
  return { message: fallback, code: "" };
}

/**
 * Dedicated fetch wrapper for unauthenticated {@code /auth/*} POST endpoints.
 * Intentionally decoupled from {@code api/client.ts} so 401/403 never forces SPA redirects.
 */
export async function postAuthJson<T>(
  url: string,
  body: Record<string, unknown>,
  bootstrap: Pick<
    AuthBootstrap,
    "authCsrfToken" | "csrfTokenName" | "csrfTokenValue"
  >,
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
    "X-Requested-With": "XMLHttpRequest",
  };
  if (bootstrap.authCsrfToken) {
    headers["X-Perc-Auth-Csrf"] = bootstrap.authCsrfToken;
  }
  if (bootstrap.csrfTokenName && bootstrap.csrfTokenValue) {
    headers[bootstrap.csrfTokenName] = bootstrap.csrfTokenValue;
  }

  const res = await fetch(url, {
    method: "POST",
    credentials: "same-origin",
    headers,
    body: JSON.stringify(body),
  });

  const rawText = await res.text();
  let parsed: unknown = null;
  if (rawText) {
    try {
      parsed = JSON.parse(rawText);
    } catch {
      parsed = null;
    }
  }

  if (!res.ok) {
    const fallback = `Request failed (${res.status})`;
    const info = extractErrorMessage(parsed, fallback);
    throw new AuthClientError(res.status, info.message, info.code);
  }

  return (parsed ?? {}) as T;
}
