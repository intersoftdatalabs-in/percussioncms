/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  get,
  isSessionRedirectError,
  SessionRedirectError,
} from "../../../main/ts/api/client";
import { resetLoginRedirectLatch } from "../../../main/ts/app/auth/sessionHandlers";

describe("api client 401 session redirect", () => {
  afterEach(() => {
    resetLoginRedirectLatch();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("redirects and throws SessionRedirectError without treating as normal ApiError", async () => {
    const assign = vi.fn();
    const original = window.location;
    // @ts-expect-error test stub
    delete (window as { location?: Location }).location;
    // @ts-expect-error test stub
    window.location = {
      ...original,
      pathname: "/cm/app/spa.jsp",
      search: "?entry=home",
      href: "http://localhost/cm/app/spa.jsp?entry=home",
      origin: "http://localhost",
      assign,
    };

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: "unauthorized" }), {
          status: 401,
          statusText: "Unauthorized",
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(get("/Rhythmyx/test")).rejects.toSatisfy(
      (err: unknown) => isSessionRedirectError(err) && err instanceof SessionRedirectError,
    );
    expect(assign).toHaveBeenCalledTimes(1);
    expect(String(assign.mock.calls[0][0])).toContain("/rxlogin.jsp");

    // @ts-expect-error restore
    window.location = original;
  });
});
