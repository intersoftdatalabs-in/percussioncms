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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { S3DeliverySecretKeyPanel } from "@/publishing/components/S3DeliverySecretKeyPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const ACCESS_KEY = "AKIATESTACCESS";
const NEW_SECRET_KEY = "new-secret-s3-key";
const SECRET_KEY = "secret-s3-key";

function s3Server() {
  return {
    serverId: "9",
    serverName: "s3-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "AMAZONS3" },
      { key: "bucketlocation", value: "old-bucket" },
      { key: "region", value: "us-west-2" },
      { key: "accesskey", value: ACCESS_KEY },
      { key: "securitykey", value: SECRET_KEY },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <S3DeliverySecretKeyPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("S3DeliverySecretKeyPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
  });

  it("does not write a Local folder or the secret key", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "3",
      serverName: "local-files",
      properties: [
        { key: "driver", value: "Local" },
        { key: "folder", value: "/kept/local" },
        { key: "securitykey", value: SECRET_KEY },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-secret-key-open"));
    expect(await screen.findByTestId("s3-secret-key-not-s3")).toHaveTextContent(
      /Amazon S3 delivery server/i,
    );
    expect(screen.queryByTestId("s3-secret-key-save")).toBeNull();
    expect(screen.queryByTestId("s3-secret-key-current")).toBeNull();
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the encoded secret key and never renders the secret", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(s3Server())
      .mockResolvedValueOnce(s3Server());
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-secret-key-open"));
    expect(await screen.findByTestId("s3-secret-key-summary")).toBeTruthy();
    expect(screen.getByTestId("s3-secret-key-bucket")).toHaveTextContent(
      "old-bucket",
    );
    expect(screen.getByTestId("s3-secret-key-region")).toHaveTextContent(
      "us-west-2",
    );
    expect(screen.getByTestId("s3-secret-key-name")).toHaveTextContent("s3-prod");
    expect(screen.getByTestId("s3-secret-key-driver")).toHaveTextContent("AMAZONS3");
    expect(screen.getByTestId("s3-secret-key-status")).toHaveTextContent(
      /never shown/i,
    );
    // The stored key is never rendered, and the input opens empty.
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
    expect(screen.getByTestId("s3-secret-key-input")).toHaveValue("");

    fireEvent.change(screen.getByTestId("s3-secret-key-input"), {
      target: { value: NEW_SECRET_KEY },
    });
    fireEvent.click(screen.getByTestId("s3-secret-key-save"));
    await waitFor(() =>
      expect(serversApi.updateServer).toHaveBeenCalledTimes(1),
    );
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: {
        properties: [{ key: "securitykey", value: btoa(NEW_SECRET_KEY) }],
      },
    });
    await waitFor(() =>
      expect(screen.queryByTestId("s3-secret-key-form")).toBeNull(),
    );
    // The typed secret is cleared and never rendered after the save.
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
    expect(document.body.textContent).not.toContain(NEW_SECRET_KEY);
    expect(screen.getByTestId("s3-secret-key-bucket")).toHaveTextContent(
      "old-bucket",
    );
    expect(screen.getByTestId("s3-secret-key-region")).toHaveTextContent(
      "us-west-2",
    );
  });

  it("does not write a blank secret key or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(s3Server());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-secret-key-open"));
    await screen.findByTestId("s3-secret-key-input");

    fireEvent.change(screen.getByTestId("s3-secret-key-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("s3-secret-key-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /S3 secret key is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("s3-secret-key-input"), {
      target: { value: NEW_SECRET_KEY },
    });
    fireEvent.click(screen.getByTestId("s3-secret-key-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.queryByTestId("s3-secret-key-form")).toBeNull();
    expect(document.body.textContent).not.toContain(NEW_SECRET_KEY);
    vi.unstubAllGlobals();
  });

  it("keeps the previous secret key on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(s3Server());
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-secret-key-open"));
    await screen.findByTestId("s3-secret-key-input");

    const cases = [
      [400, "S3 secret key is required.", /S3 secret key is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("s3-secret-key-input"), {
        target: { value: `key${status}` },
      });
      fireEvent.click(screen.getByTestId("s3-secret-key-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("s3-secret-key-bucket")).toHaveTextContent(
        "old-bucket",
      );
      expect(screen.getByTestId("s3-secret-key-region")).toHaveTextContent(
        "us-west-2",
      );
      expect(screen.getByTestId("s3-secret-key-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
  });
});
