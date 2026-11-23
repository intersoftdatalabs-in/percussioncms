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

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { message, MSG } from "@/i18n/message";
import { FtpDeliveryFolderPanel } from "@/publishing/components/FtpDeliveryFolderPanel";
import { DatabaseDeliveryHostPanel } from "@/publishing/components/DatabaseDeliveryHostPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

function ftpServer() {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "old.example" },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: "s3cret" },
    ],
  };
}

describe("delivery panels i18n keying (#6936)", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("every PUBLISH_DELIVERY_* key resolves to its non-empty English default", () => {
    const keys = Object.keys(MSG).filter((k) => k.startsWith("PUBLISH_DELIVERY_"));
    expect(keys.length).toBeGreaterThan(100);
    for (const key of keys) {
      const value = (MSG as Record<string, string>)[key];
      expect(value.startsWith("perc.ui.publish.modern@")).toBe(true);
      const resolved = message(value);
      expect(resolved).toBe(value.slice(value.lastIndexOf("@") + 1));
      expect(resolved.trim().length).toBeGreaterThan(0);
    }
  });

  it("FTP folder panel renders keyed title, labels, hint, and actions", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(ftpServer());
    render(
      <DirtyFormProvider>
        <FtpDeliveryFolderPanel siteId={42} serverId="9" />
      </DirtyFormProvider>,
    );
    fireEvent.click(screen.getByTestId("ftp-folder-open"));
    expect(await screen.findByTestId("ftp-folder-current")).toHaveTextContent("/kept/folder");
    expect(screen.getByTestId("ftp-folder-panel").textContent).toContain(
      message(MSG.PUBLISH_DELIVERY_TITLE_FTP_FOLDER),
    );
    expect(screen.getByTestId("ftp-folder-panel").textContent).toContain(
      message(MSG.PUBLISH_DELIVERY_LABEL_FOLDER),
    );
    expect(screen.getByTestId("ftp-folder-optional")).toHaveTextContent(
      message(MSG.PUBLISH_DELIVERY_HINT_CLEAR_FOLDER),
    );
    expect(screen.getByTestId("ftp-folder-save")).toHaveTextContent(
      message(MSG.PUBLISH_DELIVERY_ACTION_SAVE_FOLDER),
    );
    expect(screen.getByTestId("ftp-folder-cancel")).toHaveTextContent(
      message(MSG.PUBLISH_DELIVERY_ACTION_CANCEL),
    );
  });

  it("database host panel renders the keyed title without opening", () => {
    render(
      <DirtyFormProvider>
        <DatabaseDeliveryHostPanel siteId={42} serverId="9" />
      </DirtyFormProvider>,
    );
    expect(screen.getByTestId("db-host-panel").textContent).toContain(
      message(MSG.PUBLISH_DELIVERY_TITLE_DB_HOST),
    );
  });
});
