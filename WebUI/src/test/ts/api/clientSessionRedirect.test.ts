/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  get,
  getBinary,
  isSessionRedirectError,
  SessionRedirectError,
} from "../../../main/ts/api/client";
import { resetLoginRedirectLatch } from "../../../main/ts/app/auth/sessionHandlers";

/** Stub window.location with a navigable assign (jsdom location is static). */
function stubLocation(assign: ReturnType<typeof vi.fn>) {
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
  return original;
}

/** A followed container-auth redirect: 200 login HTML, redirected + login URL. */
function loginRedirectResponse(): Response {
  const res = new Response("<html><body>login</body></html>", {
    status: 200,
    statusText: "OK",
    headers: { "Content-Type": "text/html" },
  });
  Object.defineProperty(res, "redirected", { value: true });
  Object.defineProperty(res, "url", {
    value: "http://localhost/login?sys_redirect=http%3A%2F%2Flocalhost%2Ftest",
  });
  return res;
}

/** A followed non-login redirect (e.g. moved resource): must NOT log out. */
function otherRedirectResponse(): Response {
  const res = new Response(JSON.stringify({ ok: true }), {
    status: 200,
    statusText: "OK",
    headers: { "Content-Type": "application/json" },
  });
  Object.defineProperty(res, "redirected", { value: true });
  Object.defineProperty(res, "url", {
    value: "http://localhost/Rhythmyx/services/moved/target",
  });
  return res;
}

describe("api client 401 session redirect", () => {
  afterEach(() => {
    resetLoginRedirectLatch();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("redirects and throws SessionRedirectError without treating as normal ApiError", async () => {
    const assign = vi.fn();
    const original = stubLocation(assign);

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

  it("redirects when fetch followed a container login redirect (200 login HTML, #6085 T057)", async () => {
    const assign = vi.fn();
    const original = stubLocation(assign);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(loginRedirectResponse()));

    await expect(get("/Rhythmyx/test")).rejects.toSatisfy(
      (err: unknown) => isSessionRedirectError(err) && err instanceof SessionRedirectError,
    );
    expect(assign).toHaveBeenCalledTimes(1);
    expect(String(assign.mock.calls[0][0])).toContain("/rxlogin.jsp");

    // @ts-expect-error restore
    window.location = original;
  });

  it("redirects binary downloads on followed login redirects too", async () => {
    const assign = vi.fn();
    const original = stubLocation(assign);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(loginRedirectResponse()));

    await expect(getBinary("/Rhythmyx/test")).rejects.toSatisfy(
      (err: unknown) => isSessionRedirectError(err) && err instanceof SessionRedirectError,
    );
    expect(assign).toHaveBeenCalledTimes(1);

    // @ts-expect-error restore
    window.location = original;
  });

  it("does not log out on followed non-login redirects", async () => {
    const assign = vi.fn();
    const original = stubLocation(assign);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(otherRedirectResponse()));

    await expect(get("/Rhythmyx/test")).resolves.toEqual({ ok: true });
    expect(assign).not.toHaveBeenCalled();

    // @ts-expect-error restore
    window.location = original;
  });
});
