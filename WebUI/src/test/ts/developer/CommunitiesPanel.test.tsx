/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { CommunitiesPanel } from "../../../main/ts/developer/CommunitiesPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listCommunities: vi.fn(),
}));

const listCommunities = assemblyApi.listCommunities as ReturnType<typeof vi.fn>;

describe("CommunitiesPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listCommunities.mockReset();
  });

  it("lists communities on success", async () => {
    listCommunities.mockResolvedValue([
      {
        id: 7,
        name: "Default",
        label: "Default Community",
        description: "System community",
        guid: { stringValue: "0-1-7", longValue: 7 },
      },
    ]);
    render(<CommunitiesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-table").textContent).toContain(
      "Default Community",
    );
    expect(screen.getByTestId("developer-comm-table").textContent).toContain("Default");
  });

  it("shows empty state when API returns no communities", async () => {
    listCommunities.mockResolvedValue([]);
    render(<CommunitiesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-empty")).toBeTruthy();
    });
  });

  it("shows session-redirect message via panelErrMsg", async () => {
    listCommunities.mockRejectedValue(new SessionRedirectError());
    render(<CommunitiesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-error").textContent).toBe(
      DEV_MSG.SESSION_REDIRECT,
    );
    expect(screen.queryByTestId("developer-comm-empty")).toBeNull();
  });

  it("shows ApiError status via panelErrMsg", async () => {
    listCommunities.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
      body: null,
    });
    render(<CommunitiesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-error").textContent).toBe(
      `${DEV_MSG.COMM_ERROR} (500)`,
    );
  });

  it("shows Error.message via panelErrMsg", async () => {
    listCommunities.mockRejectedValue(new Error("network down"));
    render(<CommunitiesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-error").textContent).toBe(
      `${DEV_MSG.COMM_ERROR} network down`,
    );
    expect(screen.queryByTestId("developer-comm-table")).toBeNull();
  });

  it("shows fallback when rejection has no message", async () => {
    listCommunities.mockRejectedValue("boom");
    render(<CommunitiesPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-comm-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-comm-error").textContent).toBe(DEV_MSG.COMM_ERROR);
  });
});
