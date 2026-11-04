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
import { S3DeliveryBucketPanel } from "@/publishing/components/S3DeliveryBucketPanel";
import { DirtyFormProvider } from "@/publishing/dirtyFormContext";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const ACCESS_KEY = "AKIATESTACCESS";
const SECRET_KEY = "secret-s3-key";

function s3Server(bucket = "old-bucket") {
  return {
    serverId: "9",
    serverName: "s3-prod",
    isDefault: false,
    properties: [
      { key: "driver", value: "AMAZONS3" },
      { key: "bucketlocation", value: bucket },
      { key: "region", value: "us-west-2" },
      { key: "accesskey", value: ACCESS_KEY },
      { key: "securitykey", value: SECRET_KEY },
    ],
  };
}

function renderPanel(): void {
  render(
    <DirtyFormProvider>
      <S3DeliveryBucketPanel siteId={42} serverId="9" />
    </DirtyFormProvider>,
  );
}

describe("S3DeliveryBucketPanel", () => {
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
    fireEvent.click(screen.getByTestId("s3-bucket-open"));
    expect(await screen.findByTestId("s3-bucket-not-s3")).toHaveTextContent(
      /Amazon S3 delivery server/i,
    );
    expect(screen.queryByTestId("s3-bucket-save")).toBeNull();
    expect(screen.queryByTestId("s3-bucket-current")).toBeNull();
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
    fireEvent.click(screen.getByTestId("s3-bucket-open"));
    expect(await screen.findByTestId("s3-bucket-not-s3")).toHaveTextContent(
      /Amazon S3 delivery server/i,
    );
    expect(screen.queryByTestId("s3-bucket-save")).toBeNull();
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain("SYSTEM");
    expect(serversApi.updateServer).not.toHaveBeenCalled();
  });

  it("saves only the bucket and keeps region, name, driver, and default", async () => {
    vi.mocked(serversApi.getServer)
      .mockResolvedValueOnce(s3Server())
      .mockResolvedValueOnce(s3Server("new-bucket"));
    vi.mocked(serversApi.updateServer).mockResolvedValue({});
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-bucket-open"));
    expect(await screen.findByTestId("s3-bucket-current")).toHaveTextContent(
      "old-bucket",
    );
    expect(screen.getByTestId("s3-bucket-region")).toHaveTextContent(
      "us-west-2",
    );
    expect(screen.getByTestId("s3-bucket-name")).toHaveTextContent("s3-prod");
    expect(screen.getByTestId("s3-bucket-driver")).toHaveTextContent("AMAZONS3");
    expect(screen.getByTestId("s3-bucket-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);

    fireEvent.change(screen.getByTestId("s3-bucket-input"), {
      target: { value: "new-bucket" },
    });
    fireEvent.click(screen.getByTestId("s3-bucket-save"));
    await waitFor(() =>
      expect(serversApi.updateServer).toHaveBeenCalledTimes(1),
    );
    expect(serversApi.updateServer).toHaveBeenCalledWith(42, "9", {
      serverInfo: {
        properties: [{ key: "bucketlocation", value: "new-bucket" }],
      },
    });
    await waitFor(() =>
      expect(screen.getByTestId("s3-bucket-current")).toHaveTextContent(
        "new-bucket",
      ),
    );
    expect(screen.getByTestId("s3-bucket-region")).toHaveTextContent(
      "us-west-2",
    );
    expect(screen.getByTestId("s3-bucket-name")).toHaveTextContent("s3-prod");
    expect(screen.getByTestId("s3-bucket-driver")).toHaveTextContent("AMAZONS3");
    expect(screen.getByTestId("s3-bucket-default")).toHaveTextContent("no");
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
  });

  it("does not write a blank bucket or a cancel", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(s3Server());
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal("confirm", confirm);
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-bucket-open"));
    await screen.findByTestId("s3-bucket-input");

    fireEvent.change(screen.getByTestId("s3-bucket-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("s3-bucket-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /S3 bucket is required/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("s3-bucket-current")).toHaveTextContent(
      "old-bucket",
    );

    fireEvent.change(screen.getByTestId("s3-bucket-input"), {
      target: { value: "new-bucket" },
    });
    fireEvent.click(screen.getByTestId("s3-bucket-cancel"));
    expect(confirm).toHaveBeenCalled();
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.getByTestId("s3-bucket-current")).toHaveTextContent(
      "old-bucket",
    );
    expect(screen.queryByTestId("s3-bucket-form")).toBeNull();
    vi.unstubAllGlobals();
  });

  it("keeps the previous bucket on HTTP 400, 403, and 409", async () => {
    vi.mocked(serversApi.getServer).mockResolvedValue(s3Server());
    renderPanel();
    fireEvent.click(screen.getByTestId("s3-bucket-open"));
    await screen.findByTestId("s3-bucket-input");

    const cases = [
      [400, "S3 bucket is required.", /S3 bucket is required|400/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.updateServer).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.change(screen.getByTestId("s3-bucket-input"), {
        target: { value: `bkt${status}` },
      });
      fireEvent.click(screen.getByTestId("s3-bucket-save"));
      expect(await screen.findByRole("alert")).toHaveTextContent(pattern);
      expect(screen.getByTestId("s3-bucket-current")).toHaveTextContent(
        "old-bucket",
      );
      expect(screen.getByTestId("s3-bucket-region")).toHaveTextContent(
        "us-west-2",
      );
      expect(screen.getByTestId("s3-bucket-form")).toBeTruthy();
    }
    expect(document.body.textContent).not.toContain(SECRET_KEY);
    expect(document.body.textContent).not.toContain(ACCESS_KEY);
  });
});
