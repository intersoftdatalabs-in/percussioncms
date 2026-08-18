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

import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { HOMEPAGE_TYPES } from "../../../main/ts/api/user/userHomepageApi";
import { RoleEditor } from "../../../main/ts/workflowAdmin/role/RoleEditor";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
  formatApiError: (err: unknown, fallback: string) =>
    err && typeof err === "object" && "message" in err
      ? String((err as { message: string }).message)
      : fallback,
}));

const ALL_USERS = ["Admin", "Contributor", "Editor"];

function mockUsersGet(): void {
  vi.mocked(client.get).mockImplementation(async (url: string) => {
    if (url.includes("/user/user/users")) {
      return { UserList: { users: ALL_USERS } };
    }
    return {};
  });
}

describe("RoleEditor membership dual-list (#3504)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mockUsersGet();
    vi.mocked(client.post).mockResolvedValue({});
  });

  it("loads available users from GET all-users, not availableUsers POST", async () => {
    render(<RoleEditor role={null} onSave={vi.fn()} onCancel={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId("add-user-Admin")).toBeTruthy();
    });
    expect(screen.getByTestId("add-user-Editor")).toBeTruthy();
    expect(screen.getByTestId("add-user-Contributor")).toBeTruthy();
    expect(screen.getByTestId("available-users-heading").textContent).toContain(
      "(3)",
    );

    expect(client.get).toHaveBeenCalled();
    expect(client.post).not.toHaveBeenCalled();
  });

  it("keeps remaining users available after adding the first member", async () => {
    render(<RoleEditor role={null} onSave={vi.fn()} onCancel={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId("add-user-Admin")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("add-user-Admin"));

    await waitFor(() => {
      expect(screen.getByTestId("assigned-user-row-Admin")).toBeTruthy();
    });
    expect(screen.getByTestId("add-user-Editor")).toBeTruthy();
    expect(screen.getByTestId("add-user-Contributor")).toBeTruthy();
    expect(screen.queryByTestId("add-user-Admin")).toBeNull();
    expect(screen.getByTestId("available-users-heading").textContent).toContain(
      "(2)",
    );
    expect(screen.getByTestId("assigned-users-heading").textContent).toContain(
      "(1)",
    );

    fireEvent.click(screen.getByTestId("add-user-Editor"));

    await waitFor(() => {
      expect(screen.getByTestId("assigned-user-row-Editor")).toBeTruthy();
    });
    expect(screen.getByTestId("add-user-Contributor")).toBeTruthy();
    expect(screen.getByTestId("available-users-heading").textContent).toContain(
      "(1)",
    );
    expect(screen.getByTestId("assigned-users-heading").textContent).toContain(
      "(2)",
    );

    expect(
      vi.mocked(client.post).mock.calls.some((call) =>
        String(call[0]).includes("availableUsers"),
      ),
    ).toBe(false);
  });

  it("returns a removed user to Available Users", async () => {
    render(
      <RoleEditor
        role={{ name: "Authors", description: "", users: [] }}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("add-user-Contributor")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("add-user-Contributor"));
    await waitFor(() => {
      expect(screen.getByTestId("remove-user-Contributor")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("remove-user-Contributor"));
    await waitFor(() => {
      expect(screen.getByTestId("add-user-Contributor")).toBeTruthy();
    });
    expect(screen.queryByTestId("assigned-user-row-Contributor")).toBeNull();
    expect(screen.getByTestId("available-users-heading").textContent).toContain(
      "(3)",
    );
  });

  it("exposes remaining-app homepage select and persists Explorer (#3537)", async () => {
    const onSave = vi.fn();
    render(
      <RoleEditor
        role={{ name: "Authors", description: "", users: [], homepage: "Home" }}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("role-default-homepage-select")).toBeTruthy();
    });

    const select = screen.getByTestId(
      "role-default-homepage-select",
    ) as HTMLSelectElement;
    const values = Array.from(select.options).map((o) => o.value);
    expect(values).toContain(HOMEPAGE_TYPES.HOME);
    expect(values).toContain(HOMEPAGE_TYPES.EXPLORER);
    expect(values).toContain(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(values).toContain(HOMEPAGE_TYPES.DEVELOPER);
    expect(values).toContain(HOMEPAGE_TYPES.PUBLISH);
    expect(values).toContain(HOMEPAGE_TYPES.WORKFLOW);
    expect(values).not.toContain(HOMEPAGE_TYPES.EDITOR);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);

    fireEvent.change(select, { target: { value: HOMEPAGE_TYPES.EXPLORER } });
    fireEvent.click(screen.getByTestId("save-role-button"));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalled();
    });
    const updateCall = vi
      .mocked(client.post)
      .mock.calls.find((call) => String(call[0]).includes("update"));
    expect(updateCall).toBeTruthy();
    const payload = updateCall![1] as { Role?: { homepage?: string } };
    expect(payload.Role?.homepage).toBe(HOMEPAGE_TYPES.EXPLORER);
  });

  it("keeps a stale Editor homepage visible so it can be cleared", async () => {
    render(
      <RoleEditor
        role={{
          name: "Editors",
          description: "",
          users: [],
          homepage: HOMEPAGE_TYPES.EDITOR,
        }}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("role-default-homepage-select")).toBeTruthy();
    });
    const select = screen.getByTestId(
      "role-default-homepage-select",
    ) as HTMLSelectElement;
    expect(select.value).toBe(HOMEPAGE_TYPES.EDITOR);
    expect(Array.from(select.options).map((o) => o.value)).toContain(
      HOMEPAGE_TYPES.EDITOR,
    );
  });
});
