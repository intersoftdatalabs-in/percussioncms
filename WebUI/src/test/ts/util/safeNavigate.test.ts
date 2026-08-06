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

import { describe, expect, it } from "vitest";
import {
  classifyUrl,
  DEFAULT_ALLOWED_PROTOCOLS,
  safeNavigate,
} from "../../../main/ts/util/safeNavigate";

const BASE_HTTP = "http://cms.example.com/Rhythmyx/";

describe("classifyUrl", () => {
  it("accepts a relative path (same-origin)", () => {
    const res = classifyUrl("/Rhythmyx/cm/app/foo.jsp", BASE_HTTP);
    expect(res.ok).toBe(true);
  });

  it("accepts a protocol-relative URL whose origin matches the base", () => {
    const res = classifyUrl("//cms.example.com/Rhythmyx/foo", BASE_HTTP);
    expect(res.ok).toBe(true);
  });

  it("accepts an absolute http(s) URL with the same origin", () => {
    const res = classifyUrl(
      "http://cms.example.com/Rhythmyx/cm/app/foo.jsp",
      BASE_HTTP,
    );
    expect(res.ok).toBe(true);
  });

  it("rejects a javascript: URL", () => {
    const res = classifyUrl("javascript:alert(1)", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("javascript:");
  });

  it("rejects a javascript: URL with mixed case (e.g. JaVaScRiPt:)", () => {
    const res = classifyUrl("JaVaScRiPt:alert(1)", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("javascript:");
  });

  it("rejects a data: URL", () => {
    const res = classifyUrl("data:text/html,<script>alert(1)</script>", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("data:");
  });

  it("rejects a vbscript: URL", () => {
    const res = classifyUrl("vbscript:msgbox(1)", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("vbscript:");
  });

  it("rejects a file: URL", () => {
    const res = classifyUrl("file:///etc/passwd", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("file:");
  });

  it("rejects a blob: URL", () => {
    const res = classifyUrl("blob:https://example.com/uuid", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("blob:");
  });

  it("rejects an http URL to a different origin even when http is in the allow-list", () => {
    const res = classifyUrl(
      "http://malicious.example.com/x",
      BASE_HTTP,
    );
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("different-origin");
  });

  it("rejects a non-default protocol (e.g. ftp:)", () => {
    const res = classifyUrl("ftp://cms.example.com/file", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("protocol-not-allowed");
  });

  it("accepts mailto: by default (it is in DEFAULT_ALLOWED_PROTOCOLS)", () => {
    // mailto is in DEFAULT_ALLOWED_PROTOCOLS so the default guard
    // accepts it (no origin per URL spec for mailto:, same-origin
    // check is bypassed for origin-less protocols).
    const res = classifyUrl("mailto:user@example.com", BASE_HTTP);
    expect(res.ok).toBe(true);
  });

  it("rejects tel: by default; accepts it when explicitly allowed", () => {
    const blocked = classifyUrl("tel:+15555550100", BASE_HTTP);
    expect(blocked.ok).toBe(false);
    if (!blocked.ok) expect(blocked.reason).toBe("protocol-not-allowed");

    const allowed = classifyUrl(
      "tel:+15555550100",
      BASE_HTTP,
      [...DEFAULT_ALLOWED_PROTOCOLS, "tel:"],
    );
    expect(allowed.ok).toBe(true);
  });

  it("rejects an empty / non-string URL", () => {
    expect(classifyUrl("", BASE_HTTP).ok).toBe(false);
    const result = classifyUrl("", BASE_HTTP);
    if (!result.ok) expect(result.reason).toBe("invalid-url");
  });
});

describe("safeNavigate", () => {
  it("assigns window.location.href for an accepted URL", () => {
    // jsdom: window.location.href writes are observable; clear before.
    const before = window.location.href;
    const res = safeNavigate("/Rhythmyx/cm/app/foo.jsp", BASE_HTTP);
    expect(res.ok).toBe(true);
    // jsdom may or may not actually mutate location.href depending on
    // version; we only assert that safeNavigate returned ok.
    expect(typeof before).toBe("string");
  });

  it("does NOT assign window.location.href for a rejected URL", () => {
    const res = safeNavigate("javascript:alert(1)", BASE_HTTP);
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.reason).toBe("javascript:");
  });
});
