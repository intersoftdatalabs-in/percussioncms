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

import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { AuthApp, readAuthBootstrap } from "../../../main/ts/auth";
import type { AuthBootstrap } from "../../../main/ts/auth";

function makeBootstrap(overrides: Partial<AuthBootstrap> = {}): AuthBootstrap {
  return {
    view: "verify-email",
    token: "secret-verify-token",
    authCsrfToken: "auth-csrf-123",
    csrfTokenName: "OWASP_CSRFTOKEN",
    csrfTokenValue: "owasp-456",
    contextPath: "",
    locale: "en-us",
    textDir: "ltr",
    ...overrides,
  };
}

describe("AuthApp", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.body.innerHTML = "";
  });

  it("never auto-POSTs verify-email on mount (email scanner safe) and POSTs on button click with CSRF headers", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: async () =>
        JSON.stringify({
          verified: true,
          status: "VERIFIED",
          message: "Your email address has been verified.",
        }),
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AuthApp bootstrap={makeBootstrap()} />);

    expect(screen.getByTestId("perc-auth-verify-submit")).toBeDefined();
    expect(fetchMock).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("perc-auth-verify-submit"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-verify-success").textContent).toContain(
        "Your email address has been verified.",
      );
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/auth/verify-email");
    expect(init.method).toBe("POST");
    expect(init.credentials).toBe("same-origin");
    expect(init.headers["X-Perc-Auth-Csrf"]).toBe("auth-csrf-123");
    expect(init.headers.OWASP_CSRFTOKEN).toBe("owasp-456");
    expect(JSON.parse(init.body)).toEqual({ token: "secret-verify-token" });
  });

  it("shows missing token notice or server error cleanly", async () => {
    render(<AuthApp bootstrap={makeBootstrap({ token: "" })} />);
    expect(screen.getByTestId("perc-auth-verify-missing-token")).toBeDefined();
    expect(screen.queryByTestId("perc-auth-verify-submit")).toBeNull();

    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        text: async () =>
          JSON.stringify({
            verified: false,
            status: "INVALID_OR_EXPIRED",
            message: "This verification link is invalid or has expired.",
          }),
      }),
    );

    render(<AuthApp bootstrap={makeBootstrap({ token: "expired" })} />);
    fireEvent.click(screen.getByTestId("perc-auth-verify-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-verify-error").textContent).toContain(
        "invalid or has expired",
      );
    });
  });

  it("handles forgot-username generic acknowledgment and 429 rate-limit feedback", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () =>
          JSON.stringify({
            accepted: true,
            message:
              "If a verified account matches that email address, we have sent a username reminder.",
          }),
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 429,
        text: async () =>
          JSON.stringify({
            accepted: false,
            message: "Too many requests. Please wait and try again later.",
          }),
      });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <AuthApp
        bootstrap={makeBootstrap({
          view: "forgot-username",
          token: "",
          contextPath: "/Rhythmyx",
        })}
      />,
    );

    const input = screen.getByTestId(
      "perc-auth-forgot-username-email",
    ) as HTMLInputElement;
    fireEvent.change(input, { target: { value: "invalid" } });
    fireEvent.click(screen.getByTestId("perc-auth-forgot-username-submit"));
    expect(screen.getByTestId("perc-auth-forgot-username-error").textContent).toMatch(
      /valid email/i,
    );
    expect(fetchMock).not.toHaveBeenCalled();

    fireEvent.change(input, { target: { value: "user@example.com" } });
    fireEvent.click(screen.getByTestId("perc-auth-forgot-username-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-auth-forgot-username-status").textContent,
      ).toContain("If a verified account matches");
    });
    expect(fetchMock.mock.calls[0][0]).toBe("/Rhythmyx/auth/forgot-username");
    expect(
      screen.getByTestId("perc-auth-back-to-login").getAttribute("href"),
    ).toBe("/Rhythmyx/login");

    fireEvent.click(screen.getByTestId("perc-auth-forgot-username-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-forgot-username-error").textContent).toContain(
        "Too many requests",
      );
    });
  });

  it("reads #perc-auth-bootstrap with CSRF holder fallback", () => {
    document.body.innerHTML = `
      <div id="perc-csrf-holder" data-csrf-name="OWASP_CSRFTOKEN" data-csrf-value="merged-tok"></div>
      <script type="application/json" id="perc-auth-bootstrap">
        {"view":"verify-email","token":"t123","authCsrfToken":"a456","csrfTokenName":"OWASP_CSRFTOKEN","csrfTokenValue":"","contextPath":"","locale":"ar","textDir":"rtl"}
      </script>
    `;
    const boot = readAuthBootstrap();
    expect(boot.view).toBe("verify-email");
    expect(boot.token).toBe("t123");
    expect(boot.authCsrfToken).toBe("a456");
    expect(boot.csrfTokenValue).toBe("merged-tok");
    expect(boot.textDir).toBe("rtl");
  });
});
