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
import type {
  AuthenticationResponseJSON,
  RegistrationResponseJSON,
} from "@simplewebauthn/browser";
import {
  AuthApp,
  MfaChallengePage,
  MfaSetupPage,
  readAuthBootstrap,
} from "../../../main/ts/auth";
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

  it("reads mfaStateJson for mfa-challenge bootstrap", () => {
    const mfaMeta = JSON.stringify({
      hasValidTx: true,
      txType: "CHALLENGE",
      totpAvailable: true,
      passkeyAvailable: false,
      recoveryCodeAvailable: true,
    });
    document.body.innerHTML = `
      <div id="perc-csrf-holder" data-csrf-name="OWASP_CSRFTOKEN" data-csrf-value="csrf-1"></div>
      <script type="application/json" id="perc-auth-bootstrap">
        ${JSON.stringify({
          view: "mfa-challenge",
          token: "",
          authCsrfToken: "a-csrf",
          mfaStateJson: mfaMeta,
          csrfTokenName: "OWASP_CSRFTOKEN",
          csrfTokenValue: "",
          contextPath: "/Rhythmyx",
          locale: "en-us",
          textDir: "ltr",
        })}
      </script>
    `;
    const boot = readAuthBootstrap();
    expect(boot.view).toBe("mfa-challenge");
    expect(boot.mfaState?.hasValidTx).toBe(true);
    expect(boot.mfaState?.totpAvailable).toBe(true);
    expect(boot.mfaState?.recoveryCodeAvailable).toBe(true);
  });

  it("renders mfa-challenge expired notice when tx is invalid, supports TOTP & recovery toggle, and handles cancel", async () => {
    render(
      <AuthApp
        bootstrap={makeBootstrap({
          view: "mfa-challenge",
          mfaState: {
            hasValidTx: false,
            txType: "",
            totpAvailable: false,
            passkeyAvailable: false,
            recoveryCodeAvailable: false,
          },
        })}
      />,
    );
    expect(screen.getByTestId("perc-auth-mfa-expired")).toBeTruthy();

    document.body.innerHTML = "";
    const navigateSpy = vi.fn();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 423,
        text: async () =>
          JSON.stringify({ error: "MFA_LOCKED", message: "Too many attempts. Locked." }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () =>
          JSON.stringify({
            authenticated: true,
            redirectUrl: "/Rhythmyx/cm/app/spa.jsp",
            recoveryCodesRemaining: 9,
            message: "Verified",
          }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ cancelled: true, loginUrl: "/Rhythmyx/login" }),
      });
    vi.stubGlobal("fetch", fetchMock);

    const activeBoot = makeBootstrap({
      view: "mfa-challenge",
      contextPath: "/Rhythmyx",
      mfaState: {
        hasValidTx: true,
        txType: "CHALLENGE",
        totpAvailable: true,
        passkeyAvailable: false,
        recoveryCodeAvailable: true,
      },
    });
    render(<MfaChallengePage bootstrap={activeBoot} onNavigate={navigateSpy} />);

    fireEvent.change(screen.getByTestId("perc-auth-mfa-code-input"), {
      target: { value: "123456" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-mfa-submit"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-mfa-error").textContent).toContain("Locked");
    });
    expect(fetchMock.mock.calls[0][0]).toBe("/Rhythmyx/auth/challenge/totp");

    fireEvent.click(screen.getByTestId("perc-auth-mfa-toggle-recovery"));
    fireEvent.change(screen.getByTestId("perc-auth-mfa-code-input"), {
      target: { value: "AB12C-DE34F" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-mfa-submit"));

    await waitFor(() => {
      expect(navigateSpy).toHaveBeenCalledWith("/Rhythmyx/cm/app/spa.jsp");
    });
    expect(fetchMock.mock.calls[1][0]).toBe("/Rhythmyx/auth/challenge/recovery-code");

    fireEvent.click(screen.getByTestId("perc-auth-mfa-cancel"));
    await waitFor(() => {
      expect(navigateSpy).toHaveBeenCalledWith("/Rhythmyx/login");
    });
  });

  it("handles mfa-setup start, QR display, confirm recovery codes, and continue redirect", async () => {
    const sampleKey = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
    const navigateSpy = vi.fn();
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () =>
          JSON.stringify({
            totpId: "totp-setup-1",
            otpauthUri: `otpauth://totp/Percussion:Admin?secret=${sampleKey}`,
            manualSecret: sampleKey,
            issuer: "Percussion CMS",
            accountName: "Admin",
            digits: 6,
            periodSeconds: 30,
          }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        text: async () =>
          JSON.stringify({
            enrolled: true,
            recoveryCodes: ["AAAA1-BBBB2", "CCCC3-DDDD4"],
            redirectUrl: "/cm/app/spa.jsp",
            message: "Enabled",
          }),
      });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <MfaSetupPage
        bootstrap={makeBootstrap({
          view: "mfa-setup",
          mfaState: {
            hasValidTx: true,
            txType: "ENROLL_ONLY",
            totpAvailable: false,
            passkeyAvailable: false,
            recoveryCodeAvailable: false,
          },
        })}
        onNavigate={navigateSpy}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-mfa-qr")).toBeTruthy();
      expect(screen.getByTestId("perc-auth-mfa-manual-secret").textContent).toBe(
        sampleKey,
      );
    });

    fireEvent.change(screen.getByTestId("perc-auth-mfa-setup-code"), {
      target: { value: "654321" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-mfa-setup-confirm"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-mfa-recovery-codes").textContent).toContain(
        "AAAA1-BBBB2",
      );
    });
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({
      totpId: "totp-setup-1",
      code: "654321",
    });

    fireEvent.click(screen.getByTestId("perc-auth-mfa-continue"));
    expect(navigateSpy).toHaveBeenCalledWith("/cm/app/spa.jsp");
  });

  it("supports passkey verification in MfaChallengePage and passkey registration in MfaSetupPage", async () => {
    const navigateSpy = vi.fn();
    const fakeAssertion = { id: "cred-1", rawId: "cred-1", type: "public-key", response: {} } as unknown as AuthenticationResponseJSON;
    const fakeRegistration = { id: "cred-2", rawId: "cred-2", type: "public-key", response: {} } as unknown as RegistrationResponseJSON;

    const fetchMock = vi.fn(async (url: string) => {
      if (url.endsWith("/challenge/passkey/options")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              challengeTxId: "pk-chal-1",
              optionsJson: JSON.stringify({ challenge: "c1", rpId: "localhost" }),
            }),
        };
      }
      if (url.endsWith("/challenge/passkey/verify")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              authenticated: true,
              redirectUrl: "/cm/app/spa.jsp",
              recoveryCodesRemaining: 10,
              message: "Verified",
            }),
        };
      }
      if (url.endsWith("/setup/totp/start")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              totpId: "totp-setup-unused",
              otpauthUri: "otpauth://totp/PercussionCMS:admin?secret=BASE32TEST",
              manualEntryKey: "BASE32TEST",
              issuer: "Percussion CMS",
            }),
        };
      }
      if (url.endsWith("/setup/passkey/start")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              challengeTxId: "pk-reg-1",
              optionsJson: JSON.stringify({ challenge: "r1", rp: { id: "localhost", name: "CMS" } }),
            }),
        };
      }
      if (url.endsWith("/setup/passkey/finish")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              enrolled: true,
              recoveryCodes: ["PK111-PK222"],
              redirectUrl: "/cm/app/spa.jsp",
              message: "Saved",
            }),
        };
      }
      throw new Error(`Unexpected fetch URL: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const runAuth = vi.fn().mockResolvedValue(fakeAssertion);
    const { unmount } = render(
      <MfaChallengePage
        bootstrap={makeBootstrap({
          view: "mfa-challenge",
          mfaState: {
            hasValidTx: true,
            txType: "CHALLENGE",
            totpAvailable: false,
            passkeyAvailable: true,
            recoveryCodeAvailable: true,
          },
        })}
        onNavigate={navigateSpy}
        runPasskeyAuth={runAuth}
        supportsWebAuthn={true}
      />,
    );

    fireEvent.click(screen.getByTestId("perc-auth-mfa-passkey-btn"));
    await waitFor(() => {
      expect(runAuth).toHaveBeenCalled();
      expect(navigateSpy).toHaveBeenCalledWith("/cm/app/spa.jsp");
    });

    unmount();
    const runRegister = vi.fn().mockResolvedValue(fakeRegistration);
    render(
      <MfaSetupPage
        bootstrap={makeBootstrap({
          view: "mfa-setup",
          mfaState: {
            hasValidTx: true,
            txType: "ENROLL_ONLY",
            totpAvailable: false,
            passkeyAvailable: false,
            recoveryCodeAvailable: false,
          },
        })}
        onNavigate={navigateSpy}
        runPasskeyRegister={runRegister}
        supportsWebAuthn={true}
      />,
    );

    fireEvent.click(screen.getByTestId("perc-auth-mfa-setup-mode-passkey"));
    fireEvent.change(screen.getByTestId("perc-auth-mfa-setup-passkey-label"), {
      target: { value: "Desk Key" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-mfa-setup-passkey-btn"));

    await waitFor(() => {
      expect(runRegister).toHaveBeenCalled();
      expect(screen.getByTestId("perc-auth-mfa-recovery-codes").textContent).toContain(
        "PK111-PK222",
      );
    });
  });

  it("handles forgot-password, reset-password, and admin-recovery views with CSRF and policy checks", async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (url.endsWith("/forgot-password")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              accepted: true,
              message: "If an eligible account matches, reset instructions were sent.",
            }),
        };
      }
      if (url.endsWith("/reset-password")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              reset: true,
              status: "RESET",
              message: "Password updated and active sessions signed out.",
            }),
        };
      }
      if (url.endsWith("/admin-recovery")) {
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              redeemed: true,
              status: "REDEEMED",
              loginUrl: "/Rhythmyx/login",
              message: "MFA factors cleared.",
            }),
        };
      }
      throw new Error(`Unexpected fetch URL: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const r1 = render(
      <AuthApp
        bootstrap={makeBootstrap({
          view: "forgot-password",
          token: "",
        })}
      />,
    );
    fireEvent.change(screen.getByTestId("perc-auth-forgot-password-identifier"), {
      target: { value: "alice" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-forgot-password-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-forgot-password-status").textContent).toContain(
        "reset instructions were sent",
      );
    });
    r1.unmount();

    const r2 = render(
      <AuthApp
        bootstrap={makeBootstrap({
          view: "reset-password",
          token: "reset-tok-123",
          passwordMinLength: 12,
          passwordMaxLength: 150,
        })}
      />,
    );
    fireEvent.change(screen.getByTestId("perc-auth-reset-password-new"), {
      target: { value: "tooShort1!" },
    });
    fireEvent.change(screen.getByTestId("perc-auth-reset-password-confirm"), {
      target: { value: "tooShort1!" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-reset-password-submit"));
    expect(screen.getByTestId("perc-auth-reset-password-error").textContent).toContain("12");

    fireEvent.change(screen.getByTestId("perc-auth-reset-password-new"), {
      target: { value: "StrongNewPassword12!" },
    });
    fireEvent.change(screen.getByTestId("perc-auth-reset-password-confirm"), {
      target: { value: "StrongNewPassword12!" },
    });
    fireEvent.click(screen.getByTestId("perc-auth-reset-password-submit"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-reset-password-success").textContent).toContain(
        "Password updated",
      );
    });
    r2.unmount();

    const rMissing = render(
      <AuthApp
        bootstrap={makeBootstrap({
          view: "admin-recovery",
          token: "",
        })}
      />,
    );
    expect(screen.getByTestId("perc-auth-admin-recovery-missing-token")).toBeDefined();
    rMissing.unmount();

    render(
      <AuthApp
        bootstrap={makeBootstrap({
          view: "admin-recovery",
          token: "bg-tok-9",
        })}
      />,
    );
    fireEvent.click(screen.getByTestId("perc-auth-admin-recovery-confirm"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-auth-admin-recovery-success").textContent).toContain(
        "MFA factors cleared",
      );
    });
    expect(screen.getByTestId("perc-auth-back-to-login").getAttribute("href")).toBe(
      "/Rhythmyx/login",
    );
  });
});
