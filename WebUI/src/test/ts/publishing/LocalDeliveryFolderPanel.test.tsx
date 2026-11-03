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
import { LocalDeliveryFolderPanel } from "@/publishing/components/LocalDeliveryFolderPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";
import { LOCAL_FOLDER_MAX_LENGTH } from "@/publishing/deliveryServerLocalFolder";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";

function localServer(folder = "/kept/local") {
  return {
    serverId: "3",
    serverName: "local-files",
    properties: [
      { key: "driver", value: "Local" },
      { key: "folder", value: folder },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <LocalDeliveryFolderPanel siteId={42} serverId="3" />
    </DirtyFormProvider>,
  );
}

describe("LocalDeliveryFolderPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write an FTP folder and does not show host-style FTP properties", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "9",
      serverName: "ftp-prod",
      properties: [
        { key: "driver", value: "FTP" },
        { key: "serverip", value: "old.example" },
        { key: "port", value: "2121" },
        { key: "userid", value: "ftpuser" },
        { key: "folder", value: "/kept/ftp" },
        { key: "password", value: PASSWORD },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("local-folder-open"));
    expect(await screen.findByTestId("local-folder-not-local")).toHaveTextContent(
      /only on a Local delivery server/i,
    );
    expect(screen.queryByTestId("local-folder-save")).toBeNull();
    expect(screen.queryByTestId("local-folder-current")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain("old.example");
    expect(document.body.textContent).not.toContain("/kept/ftp");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the folder and keeps the driver and name", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(localServer())
      .mockResolvedValueOnce(localServer("/night-local"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("local-folder-open"));
    expect(await screen.findByTestId("local-folder-current")).toHaveTextContent("/kept/local");

    fireEvent.change(screen.getByTestId("local-folder-input"), {
      target: { value: "  /night-local  " },
    });
    fireEvent.click(screen.getByTestId("local-folder-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "3", {
      serverInfo: { properties: [{ key: "folder", value: "/night-local" }] },
    });
    await waitFor(() =>
      expect(screen.getByTestId("local-folder-current")).toHaveTextContent("/night-local"),
    );
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(document.body.textContent).not.toContain("serverip");
  });

  it("clears the folder when the field is blank and does not write a cancel or an overlong folder", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(localServer())
      .mockResolvedValueOnce(localServer())
      .mockResolvedValueOnce(localServer(""));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("local-folder-open"));
    await screen.findByTestId("local-folder-input");

    fireEvent.change(screen.getByTestId("local-folder-input"), {
      target: { value: "/".repeat(LOCAL_FOLDER_MAX_LENGTH + 1) },
    });
    fireEvent.click(screen.getByTestId("local-folder-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/255 characters or fewer/i);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("local-folder-current")).toHaveTextContent("/kept/local");

    fireEvent.change(screen.getByTestId("local-folder-input"), {
      target: { value: "/night-local" },
    });
    fireEvent.click(screen.getByTestId("local-folder-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("local-folder-current")).toHaveTextContent("/kept/local");
    expect(screen.queryByTestId("local-folder-form")).toBeNull();

    fireEvent.click(screen.getByTestId("local-folder-open"));
    await screen.findByTestId("local-folder-input");
    fireEvent.change(screen.getByTestId("local-folder-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("local-folder-save"));
    await waitFor(() => expect(serversApi.updateServer).toHaveBeenCalledTimes(1));
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "3", {
      serverInfo: { properties: [{ key: "folder", value: "" }] },
    });
    await waitFor(() => expect(screen.getByTestId("local-folder-current")).toHaveTextContent("—"));
    vi.unstubAllGlobals();
  });

  it("keeps the previous folder on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(localServer());
    renderPanel();
    fireEvent.click(screen.getByTestId("local-folder-open"));
    await screen.findByTestId("local-folder-input");

    const cases = [
      [400, "Local folder must be 255 characters or fewer.", /255 characters or fewer|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("local-folder-input"), {
        target: { value: `/will-not-save-${status}` },
      });
      fireEvent.click(screen.getByTestId("local-folder-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("local-folder-current")).toHaveTextContent("/kept/local");
      expect(screen.getByTestId("local-folder-form")).toBeTruthy();
    }
  });
});
