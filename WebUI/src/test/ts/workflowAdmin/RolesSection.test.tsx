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
import { RolesSection } from "../../../main/ts/workflowAdmin/role/RolesSection";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("RolesSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders role list after fetching successfully", async () => {
    vi.mocked(client.get).mockResolvedValue({
      RoleList: { roles: ["Admin", "Contributor"] },
    });
    vi.mocked(client.post).mockImplementation(async (url: string, payload: any) => {
      if (url.includes("role/find")) {
        const name = payload.psstring.value;
        return {
          Role: {
            name,
            description: `${name} role`,
            users: ["user1", "user2"],
          },
        };
      }
      return {};
    });

    render(<RolesSection />);

    await waitFor(() => {
      expect(screen.getByTestId("role-card-Admin")).toBeTruthy();
    });
    expect(screen.getByTestId("role-card-Contributor")).toBeTruthy();
  });

  it("does not crash when RoleList.roles is a single string (#3202)", async () => {
    vi.mocked(client.get).mockResolvedValue({
      RoleList: { roles: "Admin" },
    });
    vi.mocked(client.post).mockResolvedValue({
      Role: { name: "Admin", description: "Administrator", users: "admin" },
    });

    render(<RolesSection />);

    await waitFor(() => {
      expect(screen.getByTestId("role-card-Admin")).toBeTruthy();
    });
    expect(screen.getByTestId("perc-roles-section")).toBeTruthy();
    expect(screen.queryByTestId("route-error")).toBeNull();
  });

  it("opens editor when create role button clicked", async () => {
    vi.mocked(client.get).mockResolvedValue({ RoleList: { roles: [] } });

    render(<RolesSection />);

    await waitFor(() => {
      expect(screen.getByTestId("create-role-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("create-role-button"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-role-editor")).toBeTruthy();
    });
  });
});
