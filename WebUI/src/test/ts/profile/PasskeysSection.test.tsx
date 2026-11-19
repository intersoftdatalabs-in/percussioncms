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

import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type {
  AuthenticationResponseJSON,
  RegistrationResponseJSON,
} from "@simplewebauthn/browser";
import { PasskeysSection } from "../../../main/ts/profile/PasskeysSection";

const FAKE_REG_RESPONSE = {
  id: "cred-1",
  rawId: "cred-1",
  type: "public-key",
  response: {
    clientDataJSON: "e30",
    attestationObject: "o2NmbXRkbm9uZQ",
  },
  clientExtensionResults: {},
} as unknown as RegistrationResponseJSON;

const FAKE_AUTH_RESPONSE = {
  id: "cred-1",
  rawId: "cred-1",
  type: "public-key",
  response: {
    clientDataJSON: "e30",
    authenticatorData: "AAA",
    signature: "sig",
  },
  clientExtensionResults: {},
} as unknown as AuthenticationResponseJSON;

describe("PasskeysSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("registers a new passkey and surfaces initial recovery codes", async () => {
    const listPasskeys = vi
      .fn()
      .mockResolvedValueOnce({ passkeys: [], canDeleteLastPasskey: true })
      .mockResolvedValueOnce({
        passkeys: [
          {
            passkeyId: "pk-1",
            label: "MacBook Pro",
            aaguid: "0000",
            transports: ["internal"],
            backupEligible: true,
            backupState: true,
            createdAt: "2026-11-19T00:00:00Z",
            lastUsedAt: null,
          },
        ],
        canDeleteLastPasskey: true,
      });
    const startPasskeyRegistration = vi.fn().mockResolvedValue({
      challengeTxId: "reg-tx-1",
      optionsJson: JSON.stringify({ challenge: "abc", rp: { id: "localhost", name: "Percussion" } }),
    });
    const runPasskeyRegister = vi.fn().mockResolvedValue(FAKE_REG_RESPONSE);
    const finishPasskeyRegistration = vi.fn().mockResolvedValue({
      registered: true,
      passkey: {
        passkeyId: "pk-1",
        label: "MacBook Pro",
        aaguid: "0000",
        transports: ["internal"],
        backupEligible: true,
        backupState: true,
        createdAt: "2026-11-19T00:00:00Z",
        lastUsedAt: null,
      },
      recoveryCodes: ["11111-AAAAA", "22222-BBBBB"],
      message: "Passkey registered.",
    });
    const onMutated = vi.fn();

    render(
      <PasskeysSection
        listPasskeys={listPasskeys}
        startPasskeyRegistration={startPasskeyRegistration}
        finishPasskeyRegistration={finishPasskeyRegistration}
        runPasskeyRegister={runPasskeyRegister}
        supportsWebAuthn={true}
        onMutated={onMutated}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-passkeys-empty")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("perc-profile-passkey-label-input"), {
      target: { value: "MacBook Pro" },
    });
    fireEvent.click(screen.getByTestId("perc-profile-passkey-add"));

    await waitFor(() => {
      expect(startPasskeyRegistration).toHaveBeenCalledWith("MacBook Pro");
      expect(runPasskeyRegister).toHaveBeenCalled();
      expect(finishPasskeyRegistration).toHaveBeenCalledWith(
        "reg-tx-1",
        "MacBook Pro",
        JSON.stringify(FAKE_REG_RESPONSE),
      );
      expect(onMutated).toHaveBeenCalledWith(["11111-AAAAA", "22222-BBBBB"]);
      expect(screen.getByTestId("perc-profile-passkeys-list").textContent).toContain(
        "MacBook Pro",
      );
    });
  });

  it("enforces last-factor deletion guard when canDeleteLastPasskey is false and supports rename", async () => {
    const listPasskeys = vi
      .fn()
      .mockResolvedValueOnce({
        passkeys: [
          {
            passkeyId: "pk-1",
            label: "YubiKey",
            aaguid: null,
            transports: ["usb"],
            backupEligible: false,
            backupState: false,
            createdAt: null,
            lastUsedAt: null,
          },
        ],
        canDeleteLastPasskey: false,
      })
      .mockResolvedValueOnce({
        passkeys: [
          {
            passkeyId: "pk-1",
            label: "Office YubiKey",
            aaguid: null,
            transports: ["usb"],
            backupEligible: false,
            backupState: false,
            createdAt: null,
            lastUsedAt: null,
          },
        ],
        canDeleteLastPasskey: false,
      });
    const renamePasskey = vi.fn().mockResolvedValue({ success: true, message: "Renamed" });

    render(
      <PasskeysSection
        listPasskeys={listPasskeys}
        renamePasskey={renamePasskey}
        supportsWebAuthn={true}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-passkey-delete-pk-1")).toBeDisabled();
      expect(screen.getByTestId("perc-profile-passkeys-last-factor-hint")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("perc-profile-passkey-rename-pk-1"));
    fireEvent.change(screen.getByTestId("perc-profile-passkey-rename-input"), {
      target: { value: "Office YubiKey" },
    });
    fireEvent.click(screen.getByTestId("perc-profile-passkey-rename-save"));

    await waitFor(() => {
      expect(renamePasskey).toHaveBeenCalledWith("pk-1", "Office YubiKey");
      expect(screen.getByTestId("perc-profile-passkeys-list").textContent).toContain(
        "Office YubiKey",
      );
    });
  });

  it("handles HTTP 428 step-up via passkey verification and retries delete", async () => {
    const listPasskeys = vi
      .fn()
      .mockResolvedValueOnce({
        passkeys: [
          {
            passkeyId: "pk-1",
            label: "Passkey A",
            aaguid: null,
            transports: ["internal"],
            backupEligible: true,
            backupState: true,
            createdAt: null,
            lastUsedAt: null,
          },
          {
            passkeyId: "pk-2",
            label: "Passkey B",
            aaguid: null,
            transports: ["usb"],
            backupEligible: false,
            backupState: false,
            createdAt: null,
            lastUsedAt: null,
          },
        ],
        canDeleteLastPasskey: false,
      })
      .mockResolvedValueOnce({
        passkeys: [
          {
            passkeyId: "pk-2",
            label: "Passkey B",
            aaguid: null,
            transports: ["usb"],
            backupEligible: false,
            backupState: false,
            createdAt: null,
            lastUsedAt: null,
          },
        ],
        canDeleteLastPasskey: false,
      });
    const deletePasskey = vi
      .fn()
      .mockRejectedValueOnce({
        status: 428,
        statusText: "Precondition Required",
        body: { error: "STEP_UP_REQUIRED", message: "Verify MFA first" },
      })
      .mockResolvedValueOnce({ success: true, message: "Removed" });
    const startPasskeyStepUp = vi.fn().mockResolvedValue({
      challengeTxId: "su-pk-tx",
      optionsJson: JSON.stringify({ challenge: "def" }),
    });
    const runPasskeyAuth = vi.fn().mockResolvedValue(FAKE_AUTH_RESPONSE);
    const verifyPasskeyStepUp = vi.fn().mockResolvedValue({
      verified: true,
      expiresAtEpochMs: Date.now() + 60_000,
      message: "Verified",
    });

    render(
      <PasskeysSection
        listPasskeys={listPasskeys}
        deletePasskey={deletePasskey}
        startPasskeyStepUp={startPasskeyStepUp}
        verifyPasskeyStepUp={verifyPasskeyStepUp}
        runPasskeyAuth={runPasskeyAuth}
        supportsWebAuthn={true}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-passkey-delete-pk-1")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("perc-profile-passkey-delete-pk-1"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-passkeys-stepup-passkey")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("perc-profile-passkeys-stepup-passkey"));

    await waitFor(() => {
      expect(verifyPasskeyStepUp).toHaveBeenCalledWith(
        "su-pk-tx",
        JSON.stringify(FAKE_AUTH_RESPONSE),
      );
      expect(deletePasskey).toHaveBeenCalledTimes(2);
    });
  });
});
