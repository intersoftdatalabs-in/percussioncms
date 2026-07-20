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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Vitest setup for the modern Content Explorer (US1/T013-T016, T015a).
 *
 * <p>Tests in this folder mock {@link fetch} via {@link vi.fn()} per-test.
 * We do NOT mock the {@code @perc/i18n} helper; the {@code message()}
 * wrapper falls back to the key when {@code window.I18N} is absent, so
 * component tests don't need a TMX bundle.</p>
 */

import { afterEach, beforeEach, vi } from "vitest";

let originalFetch: typeof fetch | undefined;

beforeEach(() => {
  originalFetch = globalThis.fetch;
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
  const fn = vi.fn(impl);
  globalThis.fetch = fn as unknown as typeof fetch;
  return fn;
}