/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
import { DeliveryServerDefaultPanel } from "@/publishing/components/DeliveryServerDefaultPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function ftpServer(isDefault = false) {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    type: "File",
    isDefault,
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

function renderPanel(onDefaulted?: () => void): void {
  render(
    <DirtyFormProvider>
      <DeliveryServerDefaultPanel
        siteId={42}
        serverId="9"
        onDefaulted={onDefaulted}
      />
    </DirtyFormProvider>,
  );
}

describe("DeliveryServerDefaultPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("saves only isDefault and keeps name, driver, and folder", async () => {
    const onDefaulted = vi.fn();
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(ftpServer(false))
      .mockResolvedValueOnce(ftpServer(true));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel(onDefaulted);
    fireEvent.click(screen.getByTestId("delivery-server-default-open"));
    expect(await screen.findByTestId("delivery-server-default-name")).toHaveTextContent(
      "ftp-prod",
    );
    expect(screen.getByTestId("delivery-server-default-driver")).toHaveTextContent("FTP");
    expect(screen.getByTestId("delivery-server-default-flag")).toHaveTextContent("No");
    expect(screen.getByTestId("delivery-server-default-folder")).toHaveTextContent(
      "/kept/folder",
    );
    expect(document.body.textContent).not.toContain(PASSWORD);

    fireEvent.click(screen.getByTestId("delivery-server-default-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: { isDefault: true },
    });
    await waitFor(() =>
      expect(screen.getByTestId("delivery-server-default-flag")).toHaveTextContent("Yes"),
    );
    expect(screen.getByTestId("delivery-server-default-name")).toHaveTextContent("ftp-prod");
    expect(screen.getByTestId("delivery-server-default-driver")).toHaveTextContent("FTP");
    expect(screen.getByTestId("delivery-server-default-folder")).toHaveTextContent(
      "/kept/folder",
    );
    expect(onDefaulted).toHaveBeenCalledTimes(1);
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("does not write when the server is already default", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer(true));
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-server-default-open"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/already the default/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("delivery-server-default-flag")).toHaveTextContent("Yes");
    expect(screen.queryByTestId("delivery-server-default-form")).toBeNull();
    expect(screen.getByTestId("delivery-server-default-open")).toBeDisabled();
  });

  it("does not write when cancelled", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer(false));
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-server-default-open"));
    await screen.findByTestId("delivery-server-default-form");
    fireEvent.click(screen.getByTestId("delivery-server-default-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.queryByTestId("delivery-server-default-form")).toBeNull();
    expect(screen.getByTestId("delivery-server-default-flag")).toHaveTextContent("No");
    vi.unstubAllGlobals();
  });

  it("keeps the previous default on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer(false));
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-server-default-open"));
    await screen.findByTestId("delivery-server-default-form");

    const cases = [
      [400, "Default flag must be true to mark a delivery server as default.", /must be true|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.click(screen.getByTestId("delivery-server-default-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("delivery-server-default-flag")).toHaveTextContent("No");
      expect(screen.getByTestId("delivery-server-default-name")).toHaveTextContent("ftp-prod");
      expect(screen.getByTestId("delivery-server-default-driver")).toHaveTextContent("FTP");
      expect(screen.getByTestId("delivery-server-default-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });
});
