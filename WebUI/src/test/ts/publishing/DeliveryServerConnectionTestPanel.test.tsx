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
import { DeliveryServerConnectionTestPanel } from "@/publishing/components/DeliveryServerConnectionTestPanel";

vi.mock("@/api/publishing/serversApi", () => ({
  getServer: vi.fn(),
  updateServer: vi.fn(),
  testServerConnection: vi.fn(),
}));

const serversApi = await import("@/api/publishing/serversApi");

const PASSWORD = "s3cret-password";
const PRIVATE_KEY = "night-private-key-body";
const PASSPHRASE = "night-passphrase-value";

function renderPanel(): void {
  render(<DeliveryServerConnectionTestPanel siteId={42} serverId="9" />);
}

describe("DeliveryServerConnectionTestPanel", () => {
  beforeEach(() => {
    vi.mocked(serversApi.getServer).mockReset();
    vi.mocked(serversApi.updateServer).mockReset();
    vi.mocked(serversApi.testServerConnection).mockReset();
  });

  it("shows a success notice without persisting any server field", async () => {
    vi.mocked(serversApi.testServerConnection).mockResolvedValue({ connected: true });
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-connection-test-button"));
    expect(await screen.findByTestId("delivery-connection-test-notice")).toHaveTextContent(
      /succeeded/i,
    );
    expect(serversApi.testServerConnection).toHaveBeenCalledWith(42, "9");
    expect(serversApi.testServerConnection).toHaveBeenCalledTimes(1);
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(screen.queryByTestId("delivery-connection-test-error")).toBeNull();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
    expect(document.body.textContent).not.toContain(PASSPHRASE);
  });

  it("shows a failure notice when the stored server does not answer", async () => {
    vi.mocked(serversApi.testServerConnection).mockResolvedValue({ connected: false });
    renderPanel();
    fireEvent.click(screen.getByTestId("delivery-connection-test-button"));
    expect(await screen.findByTestId("delivery-connection-test-notice")).toHaveTextContent(
      /failed/i,
    );
    expect(serversApi.updateServer).not.toHaveBeenCalled();
    expect(document.body.textContent).not.toContain(PASSWORD);
    expect(document.body.textContent).not.toContain(PRIVATE_KEY);
    expect(document.body.textContent).not.toContain(PASSPHRASE);
  });

  it("maps HTTP 400, 403, and 409 to the save-error message", async () => {
    renderPanel();
    const cases = [
      [400, "Connection test failed validation.", /error|400|failed/i],
      [403, "Only Admin or Designer users may test.", /forbidden|admin|designer|403/i],
      [409, "Publish server name already exists", /already exists|409|conflict/i],
    ] as const;
    for (const [status, messageText, pattern] of cases) {
      vi.mocked(serversApi.testServerConnection).mockRejectedValueOnce({
        status,
        statusText: String(status),
        body: { message: messageText },
      });
      fireEvent.click(screen.getByTestId("delivery-connection-test-button"));
      expect(await screen.findByTestId("delivery-connection-test-error")).toHaveTextContent(
        pattern,
      );
      expect(screen.queryByTestId("delivery-connection-test-notice")).toBeNull();
      expect(serversApi.updateServer).not.toHaveBeenCalled();
    }
    expect(document.body.textContent).not.toContain(PASSWORD);
  });

  it("renders nothing without a selected server", () => {
    render(<DeliveryServerConnectionTestPanel siteId={42} serverId="" />);
    expect(screen.queryByTestId("delivery-connection-test-panel")).toBeNull();
  });
});
