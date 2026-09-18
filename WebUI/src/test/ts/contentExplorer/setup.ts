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

/**
 * Vitest setup for the modern Content Explorer (US1/T013-T016, T015a).
 *
 * <p>Tests in this folder mock {@link fetch} via {@link mockFetch} per-test.
 * A default never-hanging fetch is installed in {@code beforeEach} so
 * mount/refresh cannot stall on unmocked REST (views, display formats,
 * {@code getitemdates}, etc.) under the full Maven Vitest suite (#4558).
 * We do NOT mock the {@code @perc/i18n} helper; the {@code message()}
 * wrapper falls back to the key when {@code window.I18N} is absent, so
 * component tests don't need a TMX bundle.</p>
 */

import { afterEach, beforeEach, vi } from "vitest";

let originalFetch: typeof fetch | undefined;

/** Folder-mutation + axe-core budget under full-suite CPU contention (#4558). */
export const EXPLORER_SHELL_TEST_TIMEOUT = 20_000;

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/**
 * Resolve explorer REST that tests do not care about. Always a new
 * {@link Response} so callers cannot hit "Body is unusable".
 */
export function defaultExplorerFetchResponse(
  input: RequestInfo | URL,
): Response {
  const url =
    typeof input === "string"
      ? input
      : input instanceof Request
        ? input.url
        : String(input);
  if (url.includes("getitemdates")) {
    return jsonResponse({
      ItemDates: { itemId: "", startDate: "", endDate: "", comments: "" },
    });
  }
  if (url.includes("setitemdates")) {
    return jsonResponse({ status: "SUCCESS" });
  }
  return jsonResponse({});
}

beforeEach(() => {
  originalFetch = globalThis.fetch;
  globalThis.fetch = vi.fn(async (input: RequestInfo | URL) =>
    defaultExplorerFetchResponse(input),
  ) as unknown as typeof fetch;
});

afterEach(() => {
  if (originalFetch) {
    globalThis.fetch = originalFetch;
  } else {
    // @ts-expect-error - reset to undefined for next test
    delete (globalThis as { fetch?: typeof fetch }).fetch;
  }
  vi.restoreAllMocks();
});

export function mockFetch(impl: Parameters<typeof vi.fn>[0]): ReturnType<typeof vi.fn> {
  const fn = vi.fn(
    impl ??
      (async (input: RequestInfo | URL) => defaultExplorerFetchResponse(input)),
  );
  globalThis.fetch = fn as unknown as typeof fetch;
  return fn;
}
