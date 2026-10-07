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
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as rolesApi from "../../../main/ts/api/developer/rolesApi";
import { RolesPanel } from "../../../main/ts/developer/RolesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/rolesApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../main/ts/api/developer/rolesApi")
  >();
  return {
    ...actual,
    browseRoles: vi.fn(),
    createRole: vi.fn(),
    updateRoleDescription: vi.fn(),
    updateRoleHomePage: vi.fn(),
    addRoleUser: vi.fn(),
    removeRoleUser: vi.fn(),
    deleteRole: vi.fn(),
    loadRole: vi.fn(),
  };
});

const browseRoles = rolesApi.browseRoles as ReturnType<typeof vi.fn>;
const createRole = rolesApi.createRole as ReturnType<typeof vi.fn>;
const updateRoleDescription = rolesApi.updateRoleDescription as ReturnType<typeof vi.fn>;
const updateRoleHomePage = rolesApi.updateRoleHomePage as ReturnType<typeof vi.fn>;
const addRoleUser = rolesApi.addRoleUser as ReturnType<typeof vi.fn>;
const removeRoleUser = rolesApi.removeRoleUser as ReturnType<typeof vi.fn>;
const deleteRole = rolesApi.deleteRole as ReturnType<typeof vi.fn>;
const loadRole = rolesApi.loadRole as ReturnType<typeof vi.fn>;

describe("RolesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    browseRoles.mockReset();
    createRole.mockReset();
    updateRoleDescription.mockReset();
    updateRoleHomePage.mockReset();
    addRoleUser.mockReset();
    removeRoleUser.mockReset();
    deleteRole.mockReset();
    loadRole.mockReset();
    loadRole.mockImplementation(async (name: string) => ({
      name: name.trim(),
      users: [],
    }));
  });

  it("lists roles grouped by community / workflow / unassigned", async () => {
    browseRoles.mockResolvedValue({
      roles: [
        {
          name: "Author",
          description: "Authors content",
          groups: ["community", "workflow"],
          communities: ["Default"],
          workflows: ["Simple Workflow"],
        },
        {
          name: "Orphan",
          groups: ["unassigned"],
          communities: [],
          workflows: [],
        },
      ],
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-panel")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-roles-group-community").textContent).toContain(
      "Author",
    );
    expect(screen.getByTestId("developer-roles-group-workflow").textContent).toContain(
      "Author",
    );
    expect(screen.getByTestId("developer-roles-group-unassigned").textContent).toContain(
      "Orphan",
    );
    expect(screen.getByTestId("developer-roles-table-community").textContent).toContain(
      "Default",
    );
  });

  it("filters to a single group", async () => {
    browseRoles.mockResolvedValue({
      roles: [
        {
          name: "Author",
          groups: ["community"],
          communities: ["Default"],
          workflows: [],
        },
        {
          name: "Orphan",
          groups: ["unassigned"],
          communities: [],
          workflows: [],
        },
      ],
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-filters")).toBeTruthy();
    });
    expect(browseRoles).toHaveBeenCalledWith(null);
    fireEvent.click(screen.getByTestId("developer-roles-filter-unassigned"));
    await waitFor(() => {
      expect(browseRoles).toHaveBeenCalledWith("unassigned");
      expect(screen.getByTestId("developer-roles-group-unassigned")).toBeTruthy();
      expect(screen.queryByTestId("developer-roles-group-community")).toBeNull();
    });
    expect(screen.getByTestId("developer-roles-group-unassigned").textContent).toContain(
      "Orphan",
    );
  });

  it("shows empty state when API returns no roles", async () => {
    browseRoles.mockResolvedValue({ roles: [] });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    browseRoles.mockRejectedValue(new SessionRedirectError());
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-roles-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
  });

  it("shows ApiError status via panelErrMsg", async () => {
    browseRoles.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-roles-error").textContent).toBe(
      `${DEV_MSG.ROLES_ERROR} (403)`,
    );
  });

  it("collapses a group when toggled", async () => {
    browseRoles.mockResolvedValue({
      roles: [
        {
          name: "Author",
          groups: ["community"],
          communities: ["Default"],
          workflows: [],
        },
      ],
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-table-community")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-roles-group-toggle-community"));
    expect(screen.queryByTestId("developer-roles-table-community")).toBeNull();
  });

  it("rejects a blank name before calling create", async () => {
    browseRoles.mockResolvedValue({ roles: [] });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-roles-create"));
    const save = screen.getByTestId("developer-roles-create-save");
    expect(save).toBeDisabled();
    fireEvent.click(save);
    fireEvent.change(screen.getByTestId("developer-roles-create-name"), {
      target: { value: "   " },
    });
    expect(screen.getByTestId("developer-roles-create-save")).toBeDisabled();
    fireEvent.click(screen.getByTestId("developer-roles-create-save"));
    expect(createRole).not.toHaveBeenCalled();
  });

  it("cancel does not create", async () => {
    browseRoles.mockResolvedValue({
      roles: [
        {
          name: "Author",
          groups: ["community"],
          communities: ["Default"],
          workflows: [],
        },
      ],
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-roles-create"));
    fireEvent.change(screen.getByTestId("developer-roles-create-name"), {
      target: { value: "NightRole" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-create-cancel"));
    expect(createRole).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-roles-create-form")).toBeNull();
    expect(document.querySelector('[data-role-name="NightRole"]')).toBeNull();
  });

  it("shows the new row only after create succeeds", async () => {
    browseRoles
      .mockResolvedValueOnce({
        roles: [
          {
            name: "Author",
            groups: ["community"],
            communities: ["Default"],
            workflows: [],
          },
        ],
      })
      .mockResolvedValueOnce({
        roles: [
          {
            name: "Author",
            groups: ["community"],
            communities: ["Default"],
            workflows: [],
          },
          {
            name: "NightRole",
            description: "Editors",
            groups: ["workflow"],
            communities: [],
            workflows: ["Simple Workflow"],
          },
        ],
      });
    let resolveCreate: (value: { name: string; description?: string }) => void = () => {};
    createRole.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-roles-create"));
    fireEvent.change(screen.getByTestId("developer-roles-create-name"), {
      target: { value: "NightRole" },
    });
    fireEvent.change(screen.getByTestId("developer-roles-create-description"), {
      target: { value: "Editors" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-create-save"));
    expect(createRole).toHaveBeenCalledWith({
      name: "NightRole",
      description: "Editors",
    });
    expect(document.querySelector('[data-role-name="NightRole"]')).toBeNull();
    resolveCreate({ name: "NightRole", description: "Editors" });
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="NightRole"]')).toBeTruthy();
    });
    expect(screen.getByTestId("developer-roles-create-notice").textContent).toBe(
      DEV_MSG.ROLES_CREATED,
    );
    expect(browseRoles).toHaveBeenCalledTimes(2);
  });

  it("does not claim success on HTTP 400 or 403", async () => {
    browseRoles.mockResolvedValue({
      roles: [
        {
          name: "Author",
          groups: ["community"],
          communities: ["Default"],
          workflows: [],
        },
      ],
    });
    createRole.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: null,
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-roles-create"));
    fireEvent.change(screen.getByTestId("developer-roles-create-name"), {
      target: { value: "NightRole" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-create-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-roles-create-error").textContent).toContain("(400)");
    expect(document.querySelector('[data-role-name="NightRole"]')).toBeNull();
    expect(screen.queryByTestId("developer-roles-create-notice")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(1);

    createRole.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    fireEvent.click(screen.getByTestId("developer-roles-create-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-create-error").textContent).toContain("(403)");
    });
    expect(document.querySelector('[data-role-name="NightRole"]')).toBeNull();
    expect(screen.queryByTestId("developer-roles-create-notice")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(1);
  });

  function authorCatalog(description = "Authors content", homePage?: string) {
    return {
      roles: [
        {
          name: "Author",
          description,
          ...(homePage ? { homePage } : {}),
          groups: ["workflow"],
          communities: [],
          workflows: ["Simple Workflow"],
        },
      ],
    };
  }

  it("cancel does not save the home page", async () => {
    browseRoles.mockResolvedValue(authorCatalog("Authors content", "Home"));
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe(
        "Home",
      );
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-homepage"), {
      target: { value: "Explorer" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-cancel"));
    expect(updateRoleHomePage).not.toHaveBeenCalled();
    expect(updateRoleDescription).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-roles-edit-form")).toBeNull();
    expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("Home");
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-homepage-notice")).toBeNull();
  });

  it("shows the new home page only after save reloads and leaves the description", async () => {
    browseRoles
      .mockResolvedValueOnce(authorCatalog("Authors content", "Home"))
      .mockResolvedValueOnce(authorCatalog("Authors content", "Explorer"));
    let resolveSave: (value: { name: string; homePage?: string }) => void = () => {};
    updateRoleHomePage.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSave = resolve;
        }),
    );
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("Home");
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-homepage"), {
      target: { value: " explorer " },
    });
    fireEvent.click(screen.getByTestId("developer-roles-homepage-save"));
    expect(updateRoleHomePage).toHaveBeenCalledWith({
      name: "Author",
      homePage: "explorer",
    });
    expect(updateRoleDescription).not.toHaveBeenCalled();
    expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("Home");
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-homepage-notice")).toBeNull();
    resolveSave({ name: "Author", homePage: "Explorer" });
    await waitFor(() => {
      expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe(
        "Explorer",
      );
    });
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.getByTestId("developer-roles-homepage-notice").textContent).toBe(
      DEV_MSG.ROLES_HOME_SAVED,
    );
    expect(screen.queryByTestId("developer-roles-edit-form")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(2);
  });

  it("clears the home page only after a blank save succeeds", async () => {
    browseRoles
      .mockResolvedValueOnce(authorCatalog("Authors content", "Explorer"))
      .mockResolvedValueOnce(authorCatalog("Authors content"));
    updateRoleHomePage.mockResolvedValue({ name: "Author" });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe(
        "Explorer",
      );
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-homepage"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("developer-roles-homepage-save"));
    await waitFor(() => {
      expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("");
    });
    expect(updateRoleHomePage).toHaveBeenCalledWith({ name: "Author", homePage: "" });
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-homepage-notice")).toBeTruthy();
  });

  it("does not claim a home page change on HTTP 400, 403, or 404", async () => {
    browseRoles.mockResolvedValue(authorCatalog("Authors content", "Home"));
    updateRoleHomePage.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: null,
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("Home");
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-homepage"), {
      target: { value: "NotAPage" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-homepage-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-homepage-error").textContent).toContain("(400)");
    });
    expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("Home");
    expect(screen.queryByTestId("developer-roles-homepage-notice")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(1);

    updateRoleHomePage.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    fireEvent.click(screen.getByTestId("developer-roles-homepage-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-homepage-error").textContent).toContain("(403)");
    });
    expect(screen.queryByTestId("developer-roles-homepage-notice")).toBeNull();

    updateRoleHomePage.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    fireEvent.click(screen.getByTestId("developer-roles-homepage-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-homepage-error").textContent).toContain("(404)");
    });
    expect(document.querySelector('[data-role-homepage="Author"]')?.textContent).toBe("Home");
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-homepage-notice")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(1);
  });

  it("cancel does not update the description", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-description"), {
      target: { value: "Not saved" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-cancel"));
    expect(updateRoleDescription).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-roles-edit-form")).toBeNull();
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-edit-notice")).toBeNull();
  });

  it("shows the new description only after save reloads", async () => {
    browseRoles
      .mockResolvedValueOnce(authorCatalog("Authors content"))
      .mockResolvedValueOnce(authorCatalog("Updated copy"));
    let resolveSave: (value: { name: string; description?: string }) => void = () => {};
    updateRoleDescription.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSave = resolve;
        }),
    );
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
        "Authors content",
      );
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-description"), {
      target: { value: "Updated copy" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-save"));
    expect(updateRoleDescription).toHaveBeenCalledWith({
      name: "Author",
      description: "Updated copy",
    });
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-edit-notice")).toBeNull();
    resolveSave({ name: "Author", description: "Updated copy" });
    await waitFor(() => {
      expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
        "Updated copy",
      );
    });
    expect(screen.getByTestId("developer-roles-edit-notice").textContent).toBe(
      DEV_MSG.ROLES_EDIT_SAVED,
    );
    expect(screen.queryByTestId("developer-roles-edit-form")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(2);
  });

  it("does not claim a description change on HTTP 400, 403, or 404", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    updateRoleDescription.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: null,
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    fireEvent.change(screen.getByTestId("developer-roles-edit-description"), {
      target: { value: "Rejected" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-edit-error").textContent).toContain("(400)");
    });
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-edit-notice")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(1);

    updateRoleDescription.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: null,
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-edit-error").textContent).toContain("(403)");
    });
    expect(screen.queryByTestId("developer-roles-edit-notice")).toBeNull();

    updateRoleDescription.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-save"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-edit-error").textContent).toContain("(404)");
    });
    expect(document.querySelector('[data-role-description="Author"]')?.textContent).toBe(
      "Authors content",
    );
    expect(screen.queryByTestId("developer-roles-edit-notice")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(1);
  });

  it("shows each user returned for the open role", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Author", users: ["Ada", "Bea"] });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-members-list")).toBeTruthy();
    });
    expect(
      screen.getAllByTestId("developer-roles-member").map((el) => el.getAttribute("data-user-name")),
    ).toEqual(["Ada", "Bea"]);
    expect(screen.queryByTestId("developer-roles-members-empty")).toBeNull();
    expect(screen.queryByTestId("developer-roles-members-error")).toBeNull();
    expect(loadRole).toHaveBeenCalledWith("Author");
  });

  it("shows an empty membership state when the role has no users", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Author", users: [] });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-members-empty").textContent).toBe(
        DEV_MSG.ROLES_MEMBERS_EMPTY,
      );
    });
    expect(screen.queryByTestId("developer-roles-member")).toBeNull();
    expect(screen.queryByTestId("developer-roles-members-error")).toBeNull();
  });

  it("does not invent members on HTTP 403 or 404", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: { users: ["Invented"], Role: { name: "Author", users: ["Invented"] } },
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-members-error").textContent).toContain("(403)");
    });
    expect(screen.queryByTestId("developer-roles-member")).toBeNull();
    expect(screen.queryByTestId("developer-roles-members-empty")).toBeNull();
    expect(screen.getByTestId("developer-roles-members-error").textContent).not.toContain(
      "Invented",
    );

    loadRole.mockRejectedValue({
      status: 404,
      statusText: "Not Found",
      body: { message: "missing", users: ["Invented"] },
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-members-error").textContent).toContain(
        "No users are shown",
      );
    });
    expect(screen.getByTestId("developer-roles-members-error").textContent).toContain("missing");
    expect(screen.queryByTestId("developer-roles-member")).toBeNull();
    expect(screen.queryByTestId("developer-roles-members-empty")).toBeNull();
    expect(screen.getByTestId("developer-roles-members-error").textContent).not.toContain(
      "Invented",
    );
  });

  it("does not show users when the read names a different role", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Other", users: ["Ghost"] });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-members-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-roles-member")).toBeNull();
    expect(screen.getByTestId("developer-roles-members").textContent).not.toContain("Ghost");
  });

  it("description save does not send users", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Author", users: ["Ada"] });
    updateRoleDescription.mockResolvedValue({ name: "Author", description: "Updated copy" });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-member").getAttribute("data-user-name")).toBe(
        "Ada",
      );
    });
    fireEvent.change(screen.getByTestId("developer-roles-edit-description"), {
      target: { value: "Updated copy" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-save"));
    await waitFor(() => {
      expect(updateRoleDescription).toHaveBeenCalledWith({
        name: "Author",
        description: "Updated copy",
      });
    });
    const sent = updateRoleDescription.mock.calls[0]?.[0] as { users?: unknown };
    expect(sent.users).toBeUndefined();
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-edit-notice")).toBeTruthy();
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    expect(screen.queryByTestId("developer-roles-edit-form")).toBeNull();
    expect(screen.queryByTestId("developer-roles-member")).toBeNull();
  });

  it("shows the added user only after save and leaves the list on 400, 403, and 409", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Author", users: ["Bea"] });
    let resolveAdd: (value: { name: string; users: string[] }) => void = () => {};
    addRoleUser.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveAdd = resolve;
        }),
    );
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-member").getAttribute("data-user-name")).toBe(
        "Bea",
      );
    });

    const nameInput = screen.getByTestId("developer-roles-add-user-name") as HTMLInputElement;
    const save = screen.getByTestId("developer-roles-add-user-save") as HTMLButtonElement;
    fireEvent.change(nameInput, { target: { value: "   " } });
    expect(save.disabled).toBe(true);
    fireEvent.click(save);
    expect(addRoleUser).not.toHaveBeenCalled();

    fireEvent.change(nameInput, { target: { value: "Ada" } });
    expect(save.disabled).toBe(false);
    fireEvent.click(save);
    expect(addRoleUser).toHaveBeenCalledWith({ name: "Author", userName: "Ada" });
    expect(screen.getAllByTestId("developer-roles-member")).toHaveLength(1);
    expect(screen.getByTestId("developer-roles-member").getAttribute("data-user-name")).toBe("Bea");

    resolveAdd({ name: "Author", users: ["Bea", "Ada"] });
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-add-user-notice").textContent).toBe(
        DEV_MSG.ROLES_ADD_USER_SAVED,
      );
    });
    const shown = screen
      .getAllByTestId("developer-roles-member")
      .map((node) => node.getAttribute("data-user-name"));
    expect(shown).toEqual(["Bea", "Ada"]);
    expect(screen.queryByTestId("developer-roles-add-user-error")).toBeNull();

    for (const status of [400, 403, 409]) {
      addRoleUser.mockRejectedValue({
        status,
        statusText: "Error",
        body: { users: ["Mallory"] },
      });
      fireEvent.change(screen.getByTestId("developer-roles-add-user-name"), {
        target: { value: "Mallory" },
      });
      fireEvent.click(screen.getByTestId("developer-roles-add-user-save"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-roles-add-user-error").textContent).toContain(
          `(${status})`,
        );
      });
      const after = screen
        .getAllByTestId("developer-roles-member")
        .map((node) => node.getAttribute("data-user-name"));
      expect(after).toEqual(["Bea", "Ada"]);
      expect(screen.getByTestId("developer-roles-members").textContent).not.toContain("Mallory");
    }
  });

  it("cancel does not add a user", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Author", users: ["Bea"] });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-add-user-name")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-roles-add-user-name"), {
      target: { value: "Ada" },
    });
    fireEvent.click(screen.getByTestId("developer-roles-edit-cancel"));
    expect(addRoleUser).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-roles-edit-form")).toBeNull();
    expect(screen.queryByTestId("developer-roles-member")).toBeNull();
  });

  it("drops one user only after confirm and leaves that user on 400, 403, and 409", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    loadRole.mockResolvedValue({ name: "Author", users: ["Bea", "Ada"] });
    let resolveRemove: (value: { name: string; users: string[] }) => void = () => {};
    removeRoleUser.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRemove = resolve;
        }),
    );
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(document.querySelector('[data-role-name="Author"]') as Element);
    await waitFor(() => {
      expect(screen.getAllByTestId("developer-roles-member")).toHaveLength(2);
    });

    fireEvent.click(screen.getAllByTestId("developer-roles-remove-user")[0]);
    // First button is Bea (list order).
    expect(screen.getByTestId("developer-catalog-confirm-body").textContent).toContain("Bea");
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(removeRoleUser).not.toHaveBeenCalled();
    expect(
      screen.getAllByTestId("developer-roles-member").map((node) => node.getAttribute("data-user-name")),
    ).toEqual(["Bea", "Ada"]);
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();

    const adaRemove = document.querySelector(
      '[data-testid="developer-roles-remove-user"][data-user-name="Ada"]',
    ) as Element;
    fireEvent.click(adaRemove);
    expect(removeRoleUser).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    expect(removeRoleUser).toHaveBeenCalledWith({ name: "Author", userName: "Ada" });
    expect(
      screen.getAllByTestId("developer-roles-member").map((node) => node.getAttribute("data-user-name")),
    ).toEqual(["Bea", "Ada"]);

    resolveRemove({ name: "Author", users: ["Bea"] });
    await waitFor(() => {
      expect(screen.getByTestId("developer-roles-remove-user-notice").textContent).toBe(
        DEV_MSG.ROLES_REMOVE_USER_SAVED,
      );
    });
    expect(
      screen.getAllByTestId("developer-roles-member").map((node) => node.getAttribute("data-user-name")),
    ).toEqual(["Bea"]);
    expect(screen.queryByTestId("developer-roles-remove-user-error")).toBeNull();

    for (const status of [400, 403, 409]) {
      removeRoleUser.mockRejectedValue({
        status,
        statusText: "Error",
        body: { message: `forced ${status}`, users: [] },
      });
      fireEvent.click(
        document.querySelector(
          '[data-testid="developer-roles-remove-user"][data-user-name="Bea"]',
        ) as Element,
      );
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-roles-remove-user-error").textContent).toContain(
          `forced ${status}`,
        );
      });
      expect(screen.getByTestId("developer-roles-member").getAttribute("data-user-name")).toBe("Bea");
      expect(screen.queryByTestId("developer-roles-remove-user-notice")).toBeNull();
    }
  });

  it("does not delete a system role and cancel does not call the server", async () => {
    browseRoles.mockResolvedValue({
      roles: [
        {
          name: "Author",
          description: "Authors content",
          groups: ["workflow"],
          communities: [],
          workflows: ["Simple Workflow"],
        },
        {
          name: "Default",
          groups: ["unassigned"],
          communities: [],
          workflows: [],
        },
      ],
    });
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Default"]')).toBeTruthy();
    });
    const systemDelete = document.querySelector(
      '[data-testid="developer-roles-delete"][data-role-name="Default"]',
    ) as HTMLButtonElement;
    expect(systemDelete.disabled).toBe(true);
    fireEvent.click(systemDelete);
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
    expect(deleteRole).not.toHaveBeenCalled();
    // jsdom still bubbles a click on a disabled button to the row.
    if (screen.queryByTestId("developer-roles-edit-form")) {
      await waitFor(() => {
        expect(screen.getByTestId("developer-roles-members-empty")).toBeTruthy();
      });
      fireEvent.click(screen.getByTestId("developer-roles-edit-cancel"));
    }

    fireEvent.click(
      document.querySelector(
        '[data-testid="developer-roles-delete"][data-role-name="Author"]',
      ) as Element,
    );
    expect(screen.getByTestId("developer-catalog-confirm-body").textContent).toContain(
      "Author",
    );
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-cancel"));
    expect(deleteRole).not.toHaveBeenCalled();
    expect(screen.queryByTestId("developer-catalog-confirm-dialog")).toBeNull();
    expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    expect(screen.queryByTestId("developer-roles-delete-notice")).toBeNull();
  });

  it("drops the catalog row only after delete succeeds", async () => {
    browseRoles
      .mockResolvedValueOnce(authorCatalog())
      .mockResolvedValueOnce({ roles: [] });
    let resolveDelete: () => void = () => {};
    deleteRole.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveDelete = resolve;
        }),
    );
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });
    fireEvent.click(
      document.querySelector(
        '[data-testid="developer-roles-delete"][data-role-name="Author"]',
      ) as Element,
    );
    fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
    expect(deleteRole).toHaveBeenCalledWith("Author");
    expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    expect(screen.queryByTestId("developer-roles-delete-notice")).toBeNull();
    resolveDelete();
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeNull();
    });
    expect(screen.getByTestId("developer-roles-delete-notice").textContent).toBe(
      DEV_MSG.ROLES_DELETED,
    );
    expect(screen.queryByTestId("developer-roles-delete-error")).toBeNull();
    expect(browseRoles).toHaveBeenCalledTimes(2);
  });

  it("does not claim a delete on HTTP 400, 403, or 409", async () => {
    browseRoles.mockResolvedValue(authorCatalog());
    render(<RolesPanel />);
    await waitFor(() => {
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
    });

    for (const status of [400, 403, 409]) {
      deleteRole.mockRejectedValue({
        status,
        statusText: "Error",
        body: null,
      });
      fireEvent.click(
        document.querySelector(
          '[data-testid="developer-roles-delete"][data-role-name="Author"]',
        ) as Element,
      );
      fireEvent.click(screen.getByTestId("developer-catalog-confirm-submit"));
      await waitFor(() => {
        expect(screen.getByTestId("developer-roles-delete-error").textContent).toContain(
          `(${status})`,
        );
      });
      expect(document.querySelector('[data-role-name="Author"]')).toBeTruthy();
      expect(screen.queryByTestId("developer-roles-delete-notice")).toBeNull();
    }
    expect(browseRoles).toHaveBeenCalledTimes(1);
  });
});
