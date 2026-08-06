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

import { describe, it, expect } from "vitest";
import {
  buildSpaEntryRedirect,
  sanitizeLoginRedirect,
  DEFAULT_SPA_ENTRY_REDIRECT,
} from "@/login/redirect";

describe("login redirect helpers", () => {
  it("builds default home SPA entry", () => {
    expect(buildSpaEntryRedirect()).toBe("/cm/app/spa.jsp?entry=home");
    expect(DEFAULT_SPA_ENTRY_REDIRECT).toBe("/cm/app/spa.jsp?entry=home");
  });

  it("allowlists entries and rejects unknown", () => {
    expect(buildSpaEntryRedirect("publish")).toContain("entry=publish");
    expect(buildSpaEntryRedirect("evil")).toBe("/cm/app/spa.jsp?entry=home");
  });

  it("includes allowlisted extra params only", () => {
    const url = buildSpaEntryRedirect("publish", {
      section: "logs",
      siteId: "42",
      evil: "nope",
    });
    expect(url).toContain("entry=publish");
    expect(url).toContain("section=logs");
    expect(url).toContain("siteId=42");
    expect(url).not.toContain("evil");
  });

  it("rejects open redirects and fragments", () => {
    expect(sanitizeLoginRedirect("http://evil.example/x")).toBe(DEFAULT_SPA_ENTRY_REDIRECT);
    expect(sanitizeLoginRedirect("//evil.example/x")).toBe(DEFAULT_SPA_ENTRY_REDIRECT);
    expect(sanitizeLoginRedirect("/cm/app/spa.jsp?entry=home#/hack")).toBe(
      DEFAULT_SPA_ENTRY_REDIRECT,
    );
    expect(sanitizeLoginRedirect("../etc/passwd")).toBe(DEFAULT_SPA_ENTRY_REDIRECT);
  });

  it("accepts SPA and transitional /cm/app paths", () => {
    expect(sanitizeLoginRedirect("/cm/app/spa.jsp?entry=workflow")).toBe(
      "/cm/app/spa.jsp?entry=workflow",
    );
    expect(sanitizeLoginRedirect("/cm/app")).toBe("/cm/app");
  });
});
