/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
  };
});

const browseRoles = rolesApi.browseRoles as ReturnType<typeof vi.fn>;

describe("RolesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    browseRoles.mockReset();
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
    fireEvent.click(screen.getByTestId("developer-roles-filter-unassigned"));
    expect(screen.getByTestId("developer-roles-group-unassigned")).toBeTruthy();
    expect(screen.queryByTestId("developer-roles-group-community")).toBeNull();
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
});
