/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { UsersSection } from "../../../main/ts/workflowAdmin/user/UsersSection";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("UsersSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders user list after fetching successfully", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("user/users")) {
        return { UserList: { users: ["admin", "editor"] } };
      }
      if (url.includes("user/find/admin")) {
        return {
          name: "admin",
          email: "admin@percussion.com",
          providerType: "INTERNAL",
          roles: ["Admin"],
        };
      }
      if (url.includes("user/find/editor")) {
        return {
          name: "editor",
          email: "editor@percussion.com",
          providerType: "INTERNAL",
          roles: ["Contributor"],
        };
      }
      return {};
    });

    render(<UsersSection />);

    await waitFor(() => {
      expect(screen.getByTestId("user-card-admin")).toBeTruthy();
    });
    expect(screen.getByTestId("user-card-editor")).toBeTruthy();
  });

  it("opens create user editor on button click", async () => {
    vi.mocked(client.get).mockResolvedValue({ UserList: { users: [] } });

    render(<UsersSection />);

    await waitFor(() => {
      expect(screen.getByTestId("create-user-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("create-user-button"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-user-editor")).toBeTruthy();
    });
  });

  it("opens LDAP import dialog on button click and allows search if enabled", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("user/users")) {
        return { UserList: { users: [] } };
      }
      if (url.includes("external/status")) {
        return { DirectoryServiceStatus: { status: "ENABLED" } };
      }
      if (url.includes("external/find/john")) {
        return [{ name: "john.doe" }];
      }
      return {};
    });

    render(<UsersSection />);

    await waitFor(() => {
      expect(screen.getByTestId("ldap-import-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("ldap-import-button"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-ldap-dialog-overlay")).toBeTruthy();
    });

    const searchInput = screen.getByTestId("ldap-search-input");
    fireEvent.change(searchInput, { target: { value: "john" } });

    const searchSubmit = screen.getByTestId("ldap-search-submit");
    fireEvent.click(searchSubmit);

    await waitFor(() => {
      expect(screen.getByTestId("ldap-select-john.doe")).toBeTruthy();
    });
  });
});
