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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SftpDeliveryPrivateKeyPanel } from "@/publishing/components/SftpDeliveryPrivateKeyPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSPHRASE = "sftp-passphrase";
const NEW_PRIVATE_KEY = "new-private-key";
const PRIVATE_KEY = "stored-private-key";

function sftpServer() {
  return {
    serverId: "9",
    serverName: "sftp-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "SFTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "22" },
      { key: "userid", value: "sftp-user" },
      { key: "password", value: PASSPHRASE },
      { key: "privateKey", value: PRIVATE_KEY },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <SftpDeliveryPrivateKeyPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("SftpDeliveryPrivateKeyPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write an FTP host or the private key", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "old.example" },
        { key: "privateKey", value: PRIVATE_KEY },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-private-key-open"));
    expect(await screen.findByTestId("sftp-private-key-not-sftp")).toHaveTextContent(
      /SFTP delivery server/i,
    );
    expect(screen.queryByTestId("sftp-private-key-save")).toBeNull();
    expect(screen.queryByTestId("sftp-private-key-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain("old.example");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the encoded private key and never renders the key", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(sftpServer())
      .mockResolvedValueOnce(sftpServer());
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-private-key-open"));
    expect(await screen.findByTestId("sftp-private-key-summary")).toBeTruthy();
    expect(screen.getByTestId("sftp-private-key-host")).toHaveTextContent(
      "old.example",
    );
    expect(screen.getByTestId("sftp-private-key-port")).toHaveTextContent("22");
    expect(screen.getByTestId("sftp-private-key-user")).toHaveTextContent(
      "sftp-user",
    );
    expect(screen.getByTestId("sftp-private-key-name")).toHaveTextContent(
      "sftp-prod",
    );
    expect(screen.getByTestId("sftp-private-key-driver")).toHaveTextContent("SFTP");
    expect(screen.getByTestId("sftp-private-key-status")).toHaveTextContent(
      /never shown/i,
    );
    // The stored key (and passphrase) are never rendered, and the input opens empty.
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(screen.getByTestId("sftp-private-key-input")).toHaveValue("");

    fireEvent.change(screen.getByTestId("sftp-private-key-input"), {
      target: { value: NEW_PRIVATE_KEY },
    });
    fireEvent.click(screen.getByTestId("sftp-private-key-save"));
    await waitFor(() =>
      expect(serversApi.updateServer).toHaveBeenCalledTimes(1),
    );
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: {
        properties: [{ key: "privateKey", value: btoa(NEW_PRIVATE_KEY) }],
      },
    });
    await waitFor(() =>
      expect(screen.queryByTestId("sftp-private-key-form")).toBeNull(),
    );
    // The typed key is cleared and never rendered after the save.
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
    expect(document.body.textContent).not.toContain(PASSPHRASE);
    expect(document.body.textContent).not.toContain(NEW_PRIVATE_KEY);
    expect(screen.getByTestId("sftp-private-key-host")).toHaveTextContent(
      "old.example",
    );
    expect(screen.getByTestId("sftp-private-key-port")).toHaveTextContent("22");
  });

  it("does not write a blank private key or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(sftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-private-key-open"));
    await screen.findByTestId("sftp-private-key-input");

    fireEvent.change(screen.getByTestId("sftp-private-key-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("sftp-private-key-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /SFTP private key is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("sftp-private-key-input"), {
      target: { value: NEW_PRIVATE_KEY },
    });
    fireEvent.click(screen.getByTestId("sftp-private-key-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.queryByTestId("sftp-private-key-form")).toBeNull();
    expect(document.body.textContent).not.toContain(NEW_PRIVATE_KEY);
    vi.unstubAllGlobals();
  });

  it("keeps the previous private key on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(sftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("sftp-private-key-open"));
    await screen.findByTestId("sftp-private-key-input");

    const cases = [
      [400, "SFTP private key is required.", /SFTP private key is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("sftp-private-key-input"), {
        target: { value: `key${status}` },
      });
      fireEvent.click(screen.getByTestId("sftp-private-key-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("sftp-private-key-host")).toHaveTextContent(
        "old.example",
      );
      expect(screen.getByTestId("sftp-private-key-port")).toHaveTextContent("22");
      expect(screen.getByTestId("sftp-private-key-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
    expect(document.body.textContent).not.toContain(PASSPHRASE);
  });
});
