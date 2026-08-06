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
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { UserEditor } from "../../../main/ts/workflowAdmin/user/UserEditor";
import * as client from "../../../main/ts/api/client";
import * as homepageApi from "../../../main/ts/api/user/userHomepageApi";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
  putPlainText: vi.fn(),
  formatApiError: (err: unknown, fallback: string) =>
    err && typeof err === "object" && "message" in err
      ? String((err as { message: string }).message)
      : fallback,
}));

vi.mock("../../../main/ts/api/user/userHomepageApi", () => ({
  getUserHomepageOverride: vi.fn(),
  setUserHomepageOverride: vi.fn(),
  clearUserHomepageOverride: vi.fn(),
  HOMEPAGE_TYPES: {
    HOME: "Home",
    DASHBOARD: "Dashboard",
    EDITOR: "Editor",
    DESIGNER: "Designer",
    ARCHITECTURE: "Architecture",
    PUBLISH: "Publish",
    WORKFLOW: "Workflow",
    WIDGET_BUILDER: "WidgetBuilder",
  },
}));

describe("UserEditor default landing", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("user/roles") || url.includes("/roles")) {
        return { RoleList: { roles: ["Admin", "Editor", "Contributor"] } };
      }
      return {};
    });
    vi.mocked(client.post).mockResolvedValue({});
    vi.mocked(homepageApi.getUserHomepageOverride).mockResolvedValue("Editor");
    vi.mocked(homepageApi.setUserHomepageOverride).mockResolvedValue("Editor");
  });

  it("loads and shows default landing select with stored override", async () => {
    render(
      <UserEditor
        user={{
          name: "alice",
          email: "a@example.com",
          providerType: "INTERNAL",
          roles: ["Admin"],
        }}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("user-default-landing-select")).toBeTruthy();
    });

    expect(homepageApi.getUserHomepageOverride).toHaveBeenCalledWith("alice");

    const select = screen.getByTestId(
      "user-default-landing-select",
    ) as HTMLSelectElement;
    await waitFor(() => {
      expect(select.value).toBe("Editor");
    });

    expect(screen.getByTestId("user-default-landing-help")).toBeTruthy();
  });

  it("saves landing override via setUserHomepageOverride on submit", async () => {
    const onSave = vi.fn();
    render(
      <UserEditor
        user={{
          name: "alice",
          email: "a@example.com",
          providerType: "INTERNAL",
          roles: ["Admin"],
        }}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(
        (screen.getByTestId("user-default-landing-select") as HTMLSelectElement)
          .value,
      ).toBe("Editor");
    });

    fireEvent.change(screen.getByTestId("user-default-landing-select"), {
      target: { value: "Home" },
    });

    fireEvent.click(screen.getByTestId("save-user-button"));

    await waitFor(() => {
      expect(homepageApi.setUserHomepageOverride).toHaveBeenCalledWith(
        "alice",
        "Home",
      );
    });
    expect(onSave).toHaveBeenCalled();
  });

  it("allows clearing override to empty (role default)", async () => {
    const onSave = vi.fn();
    render(
      <UserEditor
        user={{
          name: "bob",
          providerType: "INTERNAL",
          roles: ["Contributor"],
        }}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("user-default-landing-select")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("user-default-landing-select"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("save-user-button"));

    await waitFor(() => {
      expect(homepageApi.setUserHomepageOverride).toHaveBeenCalledWith(
        "bob",
        "",
      );
    });
  });

  it("shows landing control for new users with role-default selected", async () => {
    render(
      <UserEditor user={null} onSave={vi.fn()} onCancel={vi.fn()} />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("user-default-landing-select")).toBeTruthy();
    });

    const select = screen.getByTestId(
      "user-default-landing-select",
    ) as HTMLSelectElement;
    expect(select.value).toBe("");
    expect(homepageApi.getUserHomepageOverride).not.toHaveBeenCalled();
  });
});
