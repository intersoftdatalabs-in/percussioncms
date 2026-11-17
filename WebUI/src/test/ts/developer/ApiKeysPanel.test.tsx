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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../../../main/ts/api/developer/apiKeysApi";
import { DEV_MSG } from "../../../main/ts/developer/messages";
import { ApiKeysPanel } from "../../../main/ts/developer/ApiKeysPanel";

vi.mock("../../../main/ts/api/developer/apiKeysApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/developer/apiKeysApi")>();
  return {
    ...actual,
    listApiKeyScopes: vi.fn(),
    listApiKeyScopePresets: vi.fn(),
    listApiKeyAssignments: vi.fn(),
    saveApiKeyScopes: vi.fn(),
  };
});

const listApiKeyScopes = api.listApiKeyScopes as ReturnType<typeof vi.fn>;
const listApiKeyScopePresets = api.listApiKeyScopePresets as ReturnType<typeof vi.fn>;
const listApiKeyAssignments = api.listApiKeyAssignments as ReturnType<typeof vi.fn>;
const saveApiKeyScopes = api.saveApiKeyScopes as ReturnType<typeof vi.fn>;

const SCOPES = [
  { key: "content:read", name: "Read content", group: "content" },
  { key: "content:write", name: "Write content", group: "content" },
  { key: "publish", name: "Publish", group: "publishing" },
];

const PRESETS = [
  { id: "readonly", name: "Read-only", scopes: ["content:read"] },
  { id: "author", name: "Author", scopes: ["content:read", "content:write"] },
];

function mockCatalog(keys: unknown[] = []) {
  listApiKeyScopes.mockResolvedValue(SCOPES);
  listApiKeyScopePresets.mockResolvedValue(PRESETS);
  listApiKeyAssignments.mockResolvedValue(keys);
}

describe("ApiKeysPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listApiKeyScopes.mockReset();
    listApiKeyScopePresets.mockReset();
    listApiKeyAssignments.mockReset();
    saveApiKeyScopes.mockReset();
  });

  it("shows empty state when no keys are registered", async () => {
    mockCatalog([]);
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-empty")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-api-keys-empty").textContent).toBe(
      DEV_MSG.API_KEYS_EMPTY,
    );
    expect(screen.getByTestId("developer-api-keys-new")).toBeTruthy();
  });

  it("shows the 403 message for non-Admin callers", async () => {
    listApiKeyScopes.mockRejectedValue({ status: 403, statusText: "Forbidden", body: null });
    listApiKeyScopePresets.mockResolvedValue(PRESETS);
    listApiKeyAssignments.mockResolvedValue([]);
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-api-keys-error").textContent).toBe(
      DEV_MSG.API_KEYS_FORBIDDEN,
    );
  });

  it("opens a key, applies a preset, and saves only on Save", async () => {
    mockCatalog([
      { name: "sync", scopes: ["content:read"], presetId: "readonly" },
    ]);
    saveApiKeyScopes.mockImplementation(async (name: string, body: { scopes: string[] }) => ({
      name,
      scopes: [...body.scopes],
      presetId: null,
    }));
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-row-sync")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-api-keys-row-sync"));
    expect(screen.getByTestId("developer-api-keys-editor")).toBeTruthy();

    // Applying the author preset selects content:write without calling the server.
    fireEvent.click(screen.getByTestId("developer-api-keys-preset-author"));
    expect(
      (screen.getByTestId("developer-api-keys-scope-content:write") as HTMLInputElement).checked,
    ).toBe(true);
    expect(saveApiKeyScopes).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("developer-api-keys-save"));
    await waitFor(() => {
      expect(saveApiKeyScopes).toHaveBeenCalledTimes(1);
    });
    expect(saveApiKeyScopes).toHaveBeenCalledWith(
      "sync",
      expect.objectContaining({ scopes: ["content:read", "content:write"] }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-saved")).toBeTruthy();
    });
  });

  it("reset restores the stored scope set without calling the server", async () => {
    mockCatalog([{ name: "sync", scopes: ["content:read"], presetId: "readonly" }]);
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-row-sync")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-api-keys-row-sync"));

    const box = screen.getByTestId(
      "developer-api-keys-scope-content:write",
    ) as HTMLInputElement;
    fireEvent.click(box);
    expect(box.checked).toBe(true);
    expect(screen.getByTestId("developer-api-keys-dirty")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-api-keys-reset"));
    expect(
      (screen.getByTestId("developer-api-keys-scope-content:write") as HTMLInputElement).checked,
    ).toBe(false);
    expect(saveApiKeyScopes).not.toHaveBeenCalled();
  });

  it("creates a key from the inline form and lists it after save", async () => {
    mockCatalog([]);
    saveApiKeyScopes.mockImplementation(async (name: string, body: { scopes: string[] }) => ({
      name,
      scopes: [...body.scopes],
      presetId: "readonly",
    }));
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-new")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-api-keys-new"));
    fireEvent.change(screen.getByTestId("developer-api-keys-new-name"), {
      target: { value: "dash" },
    });
    fireEvent.click(screen.getByTestId("developer-api-keys-new-save"));
    await waitFor(() => {
      expect(saveApiKeyScopes).toHaveBeenCalledTimes(1);
    });
    expect(saveApiKeyScopes).toHaveBeenCalledWith(
      "dash",
      expect.objectContaining({ scopes: ["content:read"] }),
    );
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-row-dash")).toBeTruthy();
    });
  });

  it("rejects a blank new-key name before the request", async () => {
    mockCatalog([]);
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-new")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-api-keys-new"));
    fireEvent.click(screen.getByTestId("developer-api-keys-new-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-save-error")).toBeTruthy();
    });
    expect(saveApiKeyScopes).not.toHaveBeenCalled();
  });

  it("surfaces a 403 save error with the Admin message", async () => {
    mockCatalog([{ name: "sync", scopes: ["content:read"], presetId: "readonly" }]);
    saveApiKeyScopes.mockRejectedValue({ status: 403, statusText: "Forbidden", body: null });
    render(<ApiKeysPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-row-sync")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-api-keys-row-sync"));
    fireEvent.click(
      screen.getByTestId("developer-api-keys-scope-content:write"),
    );
    fireEvent.click(screen.getByTestId("developer-api-keys-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-api-keys-save-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-api-keys-save-error").textContent).toBe(
      DEV_MSG.API_KEYS_SAVE_FORBIDDEN,
    );
  });
});
