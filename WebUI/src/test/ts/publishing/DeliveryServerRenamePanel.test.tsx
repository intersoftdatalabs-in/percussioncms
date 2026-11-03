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
import { DeliveryServerRenamePanel } from "@/publishing/components/DeliveryServerRenamePanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpServer(name = "ftp-prod") {
  return {
    serverId: "9",
    serverName: name,
    type: "File",
    isDefault: true,
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: PASSWORD },
    ],
  };
}

function renderPanel(siblingNames: string[] = ["ftp-prod", "stage"]): void {
  render(
    <DirtyFormProvider>
      <DeliveryServerRenamePanel
        siteId={42}
        serverId="9"
        siblingNames={siblingNames}
      />
    </DirtyFormProvider>,
  );
}

describe("DeliveryServerRenamePanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("saves only the name and keeps driver, folder, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer())
      .mockResolvedValueOnce(ftpServer("Night-FTP"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-server-rename-open"));
    expect(await screen.findByTestId("delivery-server-rename-current")).toHaveTextContent(
      "ftp-prod",
    );
    expect(screen.getByTestId("delivery-server-rename-driver")).toHaveTextContent("FTP");
    expect(screen.getByTestId("delivery-server-rename-default")).toHaveTextContent("Yes");
    expect(screen.getByTestId("delivery-server-rename-folder")).toHaveTextContent(
      "/kept/folder",
    );
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.change(screen.getByTestId("delivery-server-rename-input"), {
      target: { value: "Night-FTP" },
    });
    fireEvent.click(screen.getByTestId("delivery-server-rename-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { serverName: "Night-FTP" },
    });
    await waitFor(() =>
      expect(screen.getByTestId("delivery-server-rename-current")).toHaveTextContent(
        "Night-FTP",
      ),
    );
    expect(screen.getByTestId("delivery-server-rename-driver")).toHaveTextContent("FTP");
    expect(screen.getByTestId("delivery-server-rename-folder")).toHaveTextContent(
      "/kept/folder",
    );
    expect(screen.getByTestId("delivery-server-rename-default")).toHaveTextContent("Yes");
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write a blank, duplicate, or cancelled name", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-server-rename-open"));
    await screen.findByTestId("delivery-server-rename-input");

    fireEvent.change(screen.getByTestId("delivery-server-rename-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("delivery-server-rename-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/required field/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-server-rename-current")).toHaveTextContent(
      "ftp-prod",
    );

    fireEvent.change(screen.getByTestId("delivery-server-rename-input"), {
      target: { value: "stage" },
    });
    fireEvent.click(screen.getByTestId("delivery-server-rename-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/already exists/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("delivery-server-rename-input"), {
      target: { value: "will-not-save" },
    });
    fireEvent.click(screen.getByTestId("delivery-server-rename-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-server-rename-current")).toHaveTextContent(
      "ftp-prod",
    );
    expect(screen.queryByTestId("delivery-server-rename-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous name on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-server-rename-open"));
    await screen.findByTestId("delivery-server-rename-input");

    const cases = [
      [400, "Name is a required field.", /required field|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("delivery-server-rename-input"), {
        target: { value: `will-not-save-${status}` },
      });
      fireEvent.click(screen.getByTestId("delivery-server-rename-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("delivery-server-rename-current")).toHaveTextContent(
        "ftp-prod",
      );
      expect(screen.getByTestId("delivery-server-rename-driver")).toHaveTextContent("FTP");
      expect(screen.getByTestId("delivery-server-rename-folder")).toHaveTextContent(
        "/kept/folder",
      );
      expect(screen.getByTestId("delivery-server-rename-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});
