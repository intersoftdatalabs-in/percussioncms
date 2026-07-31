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

/**
 * Typed fetch wrapper for Percussion CMS REST APIs.
 *
 * <p>Every request automatically includes the OWASP CSRFGuard token (if
 * available) and sends/receives JSON by default. Endpoints that return
 * {@code text/plain} (e.g. widgetbuilder /active) are parsed as text.</p>
 */

import { redirectToLoginOnUnauthorized } from "../app/auth/sessionHandlers";
import { getCsrfToken } from "./csrf";

export interface ApiError {
  status: number;
  statusText: string;
  body: unknown;
}

/**
 * Thrown after a mid-session 401 has already started navigation to React Login.
 * Callers should not surface this as a normal API failure (toasts / form errors).
 */
export class SessionRedirectError extends Error {
  readonly status = 401;
  constructor(message = "Session expired; redirecting to login") {
    super(message);
    this.name = "SessionRedirectError";
  }
}

/** True when the error is a terminal session redirect (page is navigating away). */
export function isSessionRedirectError(err: unknown): boolean {
  return err instanceof SessionRedirectError;
}

/** True when {@code err} is a thrown {@link ApiError} (plain object, not Error). */
export function isApiError(err: unknown): err is ApiError {
  return (
    !!err &&
    typeof err === "object" &&
    typeof (err as ApiError).status === "number"
  );
}

/**
 * Human-readable message for SPA error chrome.
 *
 * <p>{@link handleResponse} throws a plain {@link ApiError} object (not an
 * {@code Error}). {@code String(err)} therefore becomes {@code "[object Object]"}
 * — always use this helper (or equivalent) before displaying API failures.</p>
 */
export function formatApiError(err: unknown, fallback = "Request failed"): string {
  if (isSessionRedirectError(err)) {
    return fallback;
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  if (isApiError(err)) {
    const st = err.statusText ? ` ${err.statusText}` : "";
    return `${fallback} (HTTP ${err.status}${st})`.trim();
  }
  return fallback;
}

function buildHeaders(extra: HeadersInit = {}, preferJson = true): Headers {
  const headers = new Headers(extra);
  if (!headers.has("Content-Type") && preferJson) {
    headers.set("Content-Type", "application/json");
  }
  // Accept both — some endpoints are TEXT_PLAIN (boolean flags)
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json, text/plain, */*");
  }

  const csrf = getCsrfToken();
  if (csrf) {
    headers.set(csrf.headerName, csrf.token);
  }
  return headers;
}

async function parseBody(response: Response): Promise<unknown> {
  const contentType = (response.headers.get("Content-Type") || "").toLowerCase();
  const text = await response.text();
  if (text == null || text.length === 0) {
    return undefined;
  }
  if (
    contentType.includes("application/json") ||
    contentType.includes("+json") ||
    text.trimStart().startsWith("{") ||
    text.trimStart().startsWith("[")
  ) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  return text;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // Mid-session expiry / auth loss → React Login (query return URL).
    // Do not parse the body or throw a normal ApiError — callers would race
    // setState/toasts while the document navigates away (#1526 review).
    if (response.status === 401) {
      redirectToLoginOnUnauthorized({ reason: "api-401" });
      throw new SessionRedirectError();
    }
    let body: unknown;
    try {
      body = await parseBody(response);
    } catch {
      body = undefined;
    }
    const error: ApiError = {
      status: response.status,
      statusText: response.statusText,
      body,
    };
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await parseBody(response)) as T;
}

/** Sends a GET request and returns the parsed body (JSON object/array or plain text). */
export async function get<T>(
  url: string,
  headers?: HeadersInit,
  init?: Omit<RequestInit, "method" | "headers" | "body">,
): Promise<T> {
  const response = await fetch(url, {
    method: "GET",
    headers: buildHeaders(headers),
    credentials: "same-origin",
    ...init,
  });
  return handleResponse<T>(response);
}

/** Sends a POST request with a JSON body. */
export async function post<T>(
  url: string,
  body?: unknown,
  headers?: HeadersInit,
): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers: buildHeaders(headers),
    credentials: "same-origin",
    body: body != null ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

/** Sends a PUT request with a JSON body. */
export async function put<T>(
  url: string,
  body?: unknown,
  headers?: HeadersInit,
): Promise<T> {
  const response = await fetch(url, {
    method: "PUT",
    headers: buildHeaders(headers),
    credentials: "same-origin",
    body: body != null ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

/** Sends a DELETE request. */
export async function del<T>(
  url: string,
  headers?: HeadersInit,
): Promise<T> {
  const response = await fetch(url, {
    method: "DELETE",
    headers: buildHeaders(headers),
    credentials: "same-origin",
  });
  return handleResponse<T>(response);
}
