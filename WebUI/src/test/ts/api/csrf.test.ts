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
 * Vitest spec: T092d / Edge Cases #7 — cross-frame session + CSRF.
 *
 * <p>Edge Case #7: "Iframe-heavy legacy editor still open beside modern
 * explorer (session/CSRF shared; no cross-frame Finder assumptions)" —
 * the legacy editor and the modern explorer share the same
 * {@code window.OWASP_CSRFTOKEN} global; if the legacy surface causes a
 * session rotation, the modern explorer's next request must pick up
 * the new token (no stale-CSRF-token leakage).</p>
 *
 * <p>This spec asserts the load-bearing contract:</p>
 * <ol>
 *   <li>{@link getCsrfToken} reads {@code window.OWASP_CSRFTOKEN.token}
 *       at the moment of the call (not memoized at module load).</li>
 *   <li>Mutating the global between calls is reflected on the next call.</li>
 *   <li>The {@link client.get}/{@link client.post} wrappers consult the
 *       token freshly per request via {@link buildHeaders}.</li>
 * </ol>
 *
 * <p>Playwright spec for the two-context scenario (modern tab + legacy
 * editor tab) lives at
 * {@code modules/perc-qa-automation/frontend/tests/us8-edge-cases-cross-frame.spec.js}
 * for QA re-execution on the UAT candidate build.</p>
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { get } from "../../../main/ts/api/client";
import { getCsrfToken } from "../../../main/ts/api/csrf";

describe("csrf / T092d / Edge Cases #7: cross-frame session + CSRF", () => {
  const originalCsrfToken = (globalThis as { OWASP_CSRFTOKEN?: unknown })
    .OWASP_CSRFTOKEN;

  beforeEach(() => {
    delete (globalThis as { OWASP_CSRFTOKEN?: unknown }).OWASP_CSRFTOKEN;
  });

  afterEach(() => {
    if (originalCsrfToken !== undefined) {
      (globalThis as { OWASP_CSRFTOKEN?: unknown }).OWASP_CSRFTOKEN =
        originalCsrfToken;
    } else {
      delete (globalThis as { OWASP_CSRFTOKEN?: unknown }).OWASP_CSRFTOKEN;
    }
    vi.restoreAllMocks();
  });

  it("returns null when the CSRF global is absent (modern explorer first paint, no CSRFGuard yet)", () => {
    expect(getCsrfToken()).toBeNull();
  });

  it("reads the OWASP_CSRFTOKEN global fresh per call (not memoized)", () => {
    // First call: legacy editor's CSRFGuard sets the global.
    (globalThis as { OWASP_CSRFTOKEN?: { token: string } }).OWASP_CSRFTOKEN = {
      token: "legacy-rotation-1",
    };
    expect(getCsrfToken()).toEqual({
      headerName: "OWASP-CSRFTOKEN",
      token: "legacy-rotation-1",
    });

    // Cross-frame scenario: legacy surface triggers a session rotation;
    // CSRFGuard rewrites the global. The modern explorer's NEXT call
    // must see the new token, not the one captured at module load.
    (globalThis as { OWASP_CSRFTOKEN?: { token: string } }).OWASP_CSRFTOKEN = {
      token: "legacy-rotation-2",
    };
    expect(getCsrfToken()).toEqual({
      headerName: "OWASP-CSRFTOKEN",
      token: "legacy-rotation-2",
    });

    // A third rotation (e.g. modern explorer re-mounts after legacy
    // navigated) is still observed.
    (globalThis as { OWASP_CSRFTOKEN?: { token: string } }).OWASP_CSRFTOKEN = {
      token: "modern-fresh",
    };
    expect(getCsrfToken()?.token).toBe("modern-fresh");
  });

  it("attaches the fresh CSRF token to every client.get call (no stale header)", async () => {
    (globalThis as { OWASP_CSRFTOKEN?: { token: string } }).OWASP_CSRFTOKEN = {
      token: "tok-A",
    };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(
      () =>
        Promise.resolve(
          new Response("{}", {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        ),
    );

    await get("/anything");
    const firstHeaders = fetchMock.mock.calls[0]?.[1]?.headers as
      | Headers
      | undefined;
    expect(firstHeaders?.get("OWASP-CSRFTOKEN")).toBe("tok-A");

    // Cross-frame rotation; the modern explorer issues a second request.
    (globalThis as { OWASP_CSRFTOKEN?: { token: string } }).OWASP_CSRFTOKEN = {
      token: "tok-B",
    };
    await get("/anything-else");
    const secondHeaders = fetchMock.mock.calls[1]?.[1]?.headers as
      | Headers
      | undefined;
    expect(secondHeaders?.get("OWASP-CSRFTOKEN")).toBe("tok-B");

    // The first request must not have leaked tok-B into the second (and
    // vice versa) — i.e., no shared header cache.
    expect(firstHeaders?.get("OWASP-CSRFTOKEN")).toBe("tok-A");
  });

  it("omits the OWASP-CSRFTOKEN header when no token is set (graceful degradation)", async () => {
    // No CSRFGuard global. The modern explorer should still be able to
    // issue GET requests (read-only paths don't require CSRF on the
    // server); the wrapper must not crash on missing token.
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(
      () =>
        Promise.resolve(
          new Response("{}", {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        ),
    );
    await get("/read-only");
    const headers = fetchMock.mock.calls[0]?.[1]?.headers as
      | Headers
      | undefined;
    expect(headers?.get("OWASP-CSRFTOKEN")).toBeNull();
  });
});