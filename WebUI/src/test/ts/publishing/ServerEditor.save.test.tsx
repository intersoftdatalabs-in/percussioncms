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
});
