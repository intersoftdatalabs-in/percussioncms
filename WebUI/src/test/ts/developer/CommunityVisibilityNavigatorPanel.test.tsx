/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as assemblyApi from "../../../main/ts/api/developer/assemblyApi";
import { CommunityVisibilityNavigatorPanel } from "../../../main/ts/developer/CommunityVisibilityNavigatorPanel";
import { DEV_MSG } from "../../../main/ts/developer/messages";

vi.mock("../../../main/ts/api/developer/assemblyApi", () => ({
  listCommunities: vi.fn(),
  getCommunityVisibility: vi.fn(),
}));

const listCommunities = assemblyApi.listCommunities as ReturnType<typeof vi.fn>;
const getCommunityVisibility = assemblyApi.getCommunityVisibility as ReturnType<
  typeof vi.fn
>;

const defaultComm = {
  id: 7,
  name: "Default",
  label: "Default Community",
  guid: { stringValue: "0-1-7", longValue: 7 },
};

describe("CommunityVisibilityNavigatorPanel", () => {
  beforeEach(() => {
    (window as unknown as { I18N?: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
    listCommunities.mockReset();
    getCommunityVisibility.mockReset();
  });

  it("lists communities in the SE-05 navigator tree", async () => {
    listCommunities.mockResolvedValue([defaultComm]);
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-tree")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cvn-panel").textContent).toContain(
      "Default Community",
    );
    expect(screen.getByTestId("developer-cvn-intro")).toBeTruthy();
  });

  it("shows empty when no communities", async () => {
    listCommunities.mockResolvedValue([]);
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-empty")).toBeTruthy();
    });
  });

  it("expands community, loads visibility, and groups by type", async () => {
    listCommunities.mockResolvedValue([defaultComm]);
    getCommunityVisibility.mockResolvedValue([
      { name: "percPage", label: "Page", type: "NODEDEF", id: 1 },
      { name: "rffSnTitle", label: "Title", type: "TEMPLATE", id: 2 },
      { name: "wfDefault", label: "Default WF", type: "WORKFLOW", id: 3 },
    ]);
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-community-toggle-0-1-7")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("developer-cvn-community-toggle-0-1-7"));

    await waitFor(() => {
      expect(getCommunityVisibility).toHaveBeenCalledWith(defaultComm.guid);
    });
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-type-groups-0-1-7")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cvn-type-0-1-7-NODEDEF")).toBeTruthy();
    expect(screen.getByTestId("developer-cvn-type-0-1-7-TEMPLATE")).toBeTruthy();
    expect(screen.getByTestId("developer-cvn-type-0-1-7-WORKFLOW")).toBeTruthy();

    fireEvent.click(screen.getByTestId("developer-cvn-type-toggle-0-1-7-NODEDEF"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-objects-0-1-7-NODEDEF")).toBeTruthy();
    });
    expect(
      screen.getByTestId("developer-cvn-objects-0-1-7-NODEDEF").textContent,
    ).toMatch(/percPage/);
  });

  it("shows community empty state when visibility returns no objects", async () => {
    listCommunities.mockResolvedValue([defaultComm]);
    getCommunityVisibility.mockResolvedValue([]);
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-community-toggle-0-1-7")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-cvn-community-toggle-0-1-7"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-community-empty-0-1-7")).toBeTruthy();
    });
  });

  it("shows visibility error when API fails for a community", async () => {
    listCommunities.mockResolvedValue([defaultComm]);
    getCommunityVisibility.mockRejectedValue(new Error("visibility boom"));
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-community-toggle-0-1-7")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("developer-cvn-community-toggle-0-1-7"));
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-community-error-0-1-7")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cvn-community-error-0-1-7").textContent).toMatch(
      /visibility boom/,
    );
  });

  it("does not reload visibility when collapsing and re-expanding ready community", async () => {
    listCommunities.mockResolvedValue([defaultComm]);
    getCommunityVisibility.mockResolvedValue([
      { name: "percPage", label: "Page", type: "NODEDEF", id: 1 },
    ]);
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-community-toggle-0-1-7")).toBeTruthy();
    });
    const toggle = screen.getByTestId("developer-cvn-community-toggle-0-1-7");
    fireEvent.click(toggle);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-type-groups-0-1-7")).toBeTruthy();
    });
    expect(getCommunityVisibility).toHaveBeenCalledTimes(1);
    fireEvent.click(toggle); // collapse
    fireEvent.click(toggle); // expand again
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-type-groups-0-1-7")).toBeTruthy();
    });
    expect(getCommunityVisibility).toHaveBeenCalledTimes(1);
  });

  it("shows catalog error when listCommunities fails", async () => {
    listCommunities.mockRejectedValue(new Error("list fail"));
    render(<CommunityVisibilityNavigatorPanel />);
    await waitFor(() => {
      expect(screen.getByTestId("developer-cvn-error")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-cvn-error").textContent).toMatch(/list fail/);
    expect(screen.getByTestId("developer-cvn-error").textContent).toContain(
      DEV_MSG.CVN_ERROR,
    );
  });
});
