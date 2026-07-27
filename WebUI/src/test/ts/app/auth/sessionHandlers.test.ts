/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  buildLoginReturnUrl,
  currentSpaReturnUrl,
  redirectToLoginOnUnauthorized,
  resetLoginRedirectLatch,
} from "../../../../main/ts/app/auth/sessionHandlers";

describe("sessionHandlers", () => {
  afterEach(() => {
    resetLoginRedirectLatch();
    vi.restoreAllMocks();
  });

  it("buildLoginReturnUrl uses allowlisted spa return", () => {
    const url = buildLoginReturnUrl("/cm/app/spa.jsp?entry=publish");
    expect(url.startsWith("/rxlogin.jsp?")).toBe(true);
    expect(url).toContain("return=");
    expect(decodeURIComponent(url)).toContain(
      "/cm/app/spa.jsp?entry=publish",
    );
    expect(url).not.toContain("#");
  });

  it("buildLoginReturnUrl rejects open redirects", () => {
    const url = buildLoginReturnUrl("https://evil.example/phish");
    expect(url).toContain("entry%3Dhome");
  });

  it("currentSpaReturnUrl parses search", () => {
    expect(currentSpaReturnUrl("?entry=workflow&tab=users")).toBe(
      "/cm/app/spa.jsp?entry=workflow&tab=users",
    );
  });

  it("currentSpaReturnUrl derives entry from path-based URL (PR-9)", () => {
    expect(currentSpaReturnUrl("", "/cm/app/publish/logs")).toBe(
      "/cm/app/spa.jsp?entry=publish&section=logs",
    );
    expect(currentSpaReturnUrl("", "/cm/app/home/gadgets")).toBe(
      "/cm/app/spa.jsp?entry=home&section=gadgets",
    );
  });

  it("redirectToLoginOnUnauthorized assigns login once", () => {
    const assign = vi.fn();
    const original = window.location;
    // @ts-expect-error test stub
    delete (window as { location?: Location }).location;
    // @ts-expect-error test stub
    window.location = {
      ...original,
      pathname: "/cm/app/spa.jsp",
      search: "?entry=home",
      assign,
    };

    redirectToLoginOnUnauthorized({ reason: "test" });
    redirectToLoginOnUnauthorized({ reason: "test-again" });
    expect(assign).toHaveBeenCalledTimes(1);
    expect(String(assign.mock.calls[0][0])).toContain("/rxlogin.jsp");

    // @ts-expect-error restore
    window.location = original;
  });
});
