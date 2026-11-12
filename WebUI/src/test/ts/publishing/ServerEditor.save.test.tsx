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
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "@/api/client";
import { ServerEditor } from "@/publishing/components/ServerEditor";

describe("ServerEditor save", () => {
  it("saves a new local delivery server", async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(
      <ServerEditor
        siteId={42}
        server={null}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Server Name/i), {
      target: { value: "NightLocal" },
    });
    fireEvent.click(screen.getByTestId("publish-server-save"));
    await waitFor(() => expect(onSave).toHaveBeenCalled());
    const [body, isCreate] = onSave.mock.calls[0];
    expect(isCreate).toBe(true);
    expect(body.serverInfo.serverName).toBe("NightLocal");
  });

  it("shows 409 conflict on the editor", async () => {
    const onSave = vi.fn().mockRejectedValue({
      status: 409,
      statusText: "Conflict",
      body: { message: "Cannot create server because a server named Dup already exists." },
    });
    render(
      <ServerEditor
        siteId={42}
        server={null}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Server Name/i), {
      target: { value: "Dup" },
    });
    fireEvent.click(screen.getByTestId("publish-server-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/already exists/i);
  });

  it("shows 403 forbidden on the editor", async () => {
    const onSave = vi.fn().mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    render(
      <ServerEditor
        siteId={42}
        server={null}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Server Name/i), {
      target: { value: "Nope" },
    });
    fireEvent.click(screen.getByTestId("publish-server-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(/Forbidden|403/i);
  });

  it("preserves edits after a 409 conflict so a second operator can retry (last-write-wins, FR-020)", async () => {
    const onSave = vi
      .fn()
      .mockRejectedValueOnce({
        status: 409,
        statusText: "Conflict",
        body: {
          message: "Cannot create server because a server named Dup already exists.",
        },
      })
      .mockResolvedValueOnce(undefined);
    const onDirtyChange = vi.fn();
    render(
      <ServerEditor
        siteId={42}
        server={null}
        onSave={onSave}
        onCancel={vi.fn()}
        onDirtyChange={onDirtyChange}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Server Name/i), {
      target: { value: "Dup" },
    });
    fireEvent.click(screen.getByTestId("publish-server-save"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      /already exists/i,
    );
    // Losing edit is not acceptable: the typed value survives the conflict.
    expect(
      (screen.getByLabelText(/Server Name/i) as HTMLInputElement).value,
    ).toBe("Dup");
    fireEvent.change(screen.getByLabelText(/Server Name/i), {
      target: { value: "Dup2" },
    });
    fireEvent.click(screen.getByTestId("publish-server-save"));
    await waitFor(() => expect(onSave).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(onDirtyChange).toHaveBeenCalledWith(false),
    );
  });

  it("stays silent on mid-save session expiry so the React Login redirect proceeds (#6092)", async () => {
    const onSave = vi.fn().mockRejectedValue(new SessionRedirectError());
    const onDirtyChange = vi.fn();
    render(
      <ServerEditor
        siteId={42}
        server={null}
        onSave={onSave}
        onCancel={vi.fn()}
        onDirtyChange={onDirtyChange}
      />,
    );
    fireEvent.change(screen.getByLabelText(/Server Name/i), {
      target: { value: "NightLocal" },
    });
    onDirtyChange.mockClear();
    fireEvent.click(screen.getByTestId("publish-server-save"));
    await waitFor(() => expect(onSave).toHaveBeenCalled());
    // Save settles (button re-enabled) with no spurious generic alert, and
    // the dirty model is kept so the operator can retry after re-login.
    await waitFor(() =>
      expect(screen.getByTestId("publish-server-save")).not.toBeDisabled(),
    );
    expect(screen.queryByRole("alert")).toBeNull();
    expect(onDirtyChange).not.toHaveBeenCalledWith(false);
  });
});
