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
 * Behavioral contract for the shared jsdom location navigation mock
 * installed in {@code WebUI/src/main/frontend/vitest.setup.ts}.
 *
 * Production code (Dashboard legacy redirect, safeNavigate, HomeShell
 * open item, etc.) assigns {@code window.location.href} or calls
 * {@code assign}/{@code replace}. Under raw jsdom those operations emit
 * "Not implemented: navigation" and do not update pathname/search.
 * The setup mock applies navigations via history so tests can observe
 * the resulting URL without changing browser semantics in production.
 */

import { afterEach, describe, expect, it } from "vitest";

describe("jsdom location navigation mock (vitest.setup)", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/");
  });

  it("accepts window.location.href assignment and updates pathname/search", () => {
    window.history.replaceState({}, "", "/cm/app/home");
    expect(window.location.pathname).toBe("/cm/app/home");

    window.location.href = "/cm/app/dashboard.jsp?legacy=true";

    expect(window.location.pathname).toBe("/cm/app/dashboard.jsp");
    expect(window.location.search).toBe("?legacy=true");
    expect(window.location.href).toContain("/cm/app/dashboard.jsp?legacy=true");
  });

  it("location.assign and location.replace update the observable URL", () => {
    window.history.replaceState({}, "", "/cm/app/home");

    window.location.assign("/cm/app/publish");
    expect(window.location.pathname).toBe("/cm/app/publish");

    window.location.replace("/cm/app/admin/tools");
    expect(window.location.pathname).toBe("/cm/app/admin/tools");
  });

  it("stays in sync when tests use history.replaceState (React Router)", () => {
    window.history.replaceState({}, "", "/cm/app/spa.jsp?entry=home");
    expect(window.location.pathname).toBe("/cm/app/spa.jsp");
    expect(window.location.search).toBe("?entry=home");

    window.history.pushState({}, "", "/cm/app/home/library");
    expect(window.location.pathname).toBe("/cm/app/home/library");
  });

  it("location.reload is a no-op (does not throw)", () => {
    expect(() => window.location.reload()).not.toThrow();
  });
});
