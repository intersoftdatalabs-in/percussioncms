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
import { S3DeliveryRegionPanel } from "@/publishing/components/S3DeliveryRegionPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const ACCESS_KEY = "AKIATESTACCESS";
const SECRET_KEY = "secret-s3-key";

function s3Server(region = "us-west-2") {
  return {
    serverId: "9",
    serverName: "s3-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "AMAZONS3" },
      { key: "bucketlocation", value: "old-bucket" },
      { key: "region", value: region },
      { key: "accesskey", value: ACCESS_KEY },
      { key: "securitykey", value: SECRET_KEY },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <S3DeliveryRegionPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("S3DeliveryRegionPanel", () => {
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
    fireEvent.click(screen.getByTestId("s3-region-open"));
    expect(await screen.findByTestId("s3-region-not-s3")).toHaveTextContent(
      /Amazon S3 delivery server/i,
    );
    expect(screen.queryByTestId("s3-region-save")).toBeNull();
    expect(screen.queryByTestId("s3-region-current")).toBeNull();
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
    expect(document.body.textContent).not.toContain("/kept/local");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("does not write a database schema", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue({
      serverId: "4",
      serverName: "ora-prod",
      properties: [
        { key: "driver", value: "Oracle" },
        { key: "schema", value: "SYSTEM" },
        { key: "password", value: SECRET_KEY },
      ],
    });
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-region-open"));
    expect(await screen.findByTestId("s3-region-not-s3")).toHaveTextContent(
      /Amazon S3 delivery server/i,
    );
    expect(screen.queryByTestId("s3-region-save")).toBeNull();
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain("SYSTEM");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the region and keeps bucket, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(s3Server())
      .mockResolvedValueOnce(s3Server("us-east-1"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-region-open"));
    expect(await screen.findByTestId("s3-region-current")).toHaveTextContent(
      "us-west-2",
    );
    expect(screen.getByTestId("s3-region-bucket")).toHaveTextContent(
      "old-bucket",
    );
    expect(screen.getByTestId("s3-region-name")).toHaveTextContent("s3-prod");
    expect(screen.getByTestId("s3-region-driver")).toHaveTextContent("AMAZONS3");
    expect(screen.getByTestId("s3-region-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);

    fireEvent.change(screen.getByTestId("s3-region-input"), {
      target: { value: "us-east-1" },
    });
    fireEvent.click(screen.getByTestId("s3-region-save"));
    await waitFor(() =>
      expect(serversApi.updateServer).toHaveBeenCalledTimes(1),
    );
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: {
        properties: [{ key: "region", value: "us-east-1" }],
      },
    });
    await waitFor(() =>
      expect(screen.getByTestId("s3-region-current")).toHaveTextContent(
        "us-east-1",
      ),
    );
    expect(screen.getByTestId("s3-region-bucket")).toHaveTextContent(
      "old-bucket",
    );
    expect(screen.getByTestId("s3-region-name")).toHaveTextContent("s3-prod");
    expect(screen.getByTestId("s3-region-driver")).toHaveTextContent("AMAZONS3");
    expect(screen.getByTestId("s3-region-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
  });

  it("does not write a blank region or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(s3Server());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-region-open"));
    await screen.findByTestId("s3-region-input");

    fireEvent.change(screen.getByTestId("s3-region-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("s3-region-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /S3 region is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("s3-region-current")).toHaveTextContent(
      "us-west-2",
    );

    fireEvent.change(screen.getByTestId("s3-region-input"), {
      target: { value: "us-east-1" },
    });
    fireEvent.click(screen.getByTestId("s3-region-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("s3-region-current")).toHaveTextContent(
      "us-west-2",
    );
    expect(screen.queryByTestId("s3-region-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous region on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(s3Server());
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-region-open"));
    await screen.findByTestId("s3-region-input");

    const cases = [
      [400, "S3 region is required.", /S3 region is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("s3-region-input"), {
        target: { value: `reg${status}` },
      });
      fireEvent.click(screen.getByTestId("s3-region-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("s3-region-current")).toHaveTextContent(
        "us-west-2",
      );
      expect(screen.getByTestId("s3-region-bucket")).toHaveTextContent(
        "old-bucket",
      );
      expect(screen.getByTestId("s3-region-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
  });
});
